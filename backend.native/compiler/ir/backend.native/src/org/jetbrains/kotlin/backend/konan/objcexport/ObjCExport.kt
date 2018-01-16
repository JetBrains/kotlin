/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isSubpackageOf

internal class ObjCExport(val context: Context) {

    private val target get() = context.config.target

    internal fun produceObjCFramework() {
        if (context.config.produce != CompilerOutputKind.FRAMEWORK) return

        val headerGenerator = ObjCExportHeaderGenerator(context)
        headerGenerator.translateModule()

        val namer = headerGenerator.namer
        val mapper = headerGenerator.mapper

        val framework = File(context.config.outputFile)
        val frameworkContents = when (target) {
            KonanTarget.IPHONE, KonanTarget.IPHONE_SIM -> framework
            KonanTarget.MACBOOK -> framework.child("Versions/A")
            else -> error(target)
        }

        val headers = frameworkContents.child("Headers")

        val frameworkName = framework.name.removeSuffix(".framework")
        val headerName = frameworkName + ".h"
        val header = headers.child(headerName)
        headers.mkdirs()
        header.writeLines(headerGenerator.build())

        val modules = frameworkContents.child("Modules")
        modules.mkdirs()

        val moduleMap = """
            |framework module $frameworkName {
            |    umbrella header "$headerName"
            |
            |    export *
            |    module * { export * }
            |}
        """.trimMargin()

        modules.child("module.modulemap").writeBytes(moduleMap.toByteArray())

        emitInfoPlist(frameworkContents, frameworkName)

        if (target == KonanTarget.MACBOOK) {
            framework.child("Versions/Current").createAsSymlink("A")
            for (child in listOf(frameworkName, "Headers", "Modules", "Resources")) {
                framework.child(child).createAsSymlink("Versions/Current/$child")
            }
        }

        val objCCodeGenerator = ObjCExportCodeGenerator(CodeGenerator(context), namer, mapper)
        objCCodeGenerator.emitRtti(headerGenerator.generatedClasses, headerGenerator.topLevel)
    }

    private fun emitInfoPlist(frameworkContents: File, name: String) {
        val directory = when (target) {
            KonanTarget.IPHONE,
            KonanTarget.IPHONE_SIM -> frameworkContents
            KonanTarget.MACBOOK -> frameworkContents.child("Resources").also { it.mkdirs() }
            else -> error(target)
        }

        val file = directory.child("Info.plist")
        val bundleExecutable = name
        val pkg = context.moduleDescriptor.guessMainPackage() // TODO: consider showing warning if it is root.
        val bundleId = pkg.child(Name.identifier(name)).asString()

        val platform = when (target) {
            KonanTarget.IPHONE -> "iPhoneOS"
            KonanTarget.IPHONE_SIM -> "iPhoneSimulator"
            KonanTarget.MACBOOK -> "MacOSX"
            else -> error(target)
        }
        val properties = context.config.platform.configurables as AppleConfigurables
        val minimumOsVersion = properties.osVersionMin!!

        val contents = StringBuilder()
        contents.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleExecutable</key>
                <string>$bundleExecutable</string>
                <key>CFBundleIdentifier</key>
                <string>$bundleId</string>
                <key>CFBundleInfoDictionaryVersion</key>
                <string>6.0</string>
                <key>CFBundleName</key>
                <string>$name</string>
                <key>CFBundlePackageType</key>
                <string>FMWK</string>
                <key>CFBundleShortVersionString</key>
                <string>1.0</string>
                <key>CFBundleSupportedPlatforms</key>
                <array>
                    <string>$platform</string>
                </array>
                <key>CFBundleVersion</key>
                <string>1</string>

        """.trimIndent())


        contents.append(when (target) {
            KonanTarget.IPHONE,
            KonanTarget.IPHONE_SIM -> """
                |    <key>MinimumOSVersion</key>
                |    <string>$minimumOsVersion</string>
                |    <key>UIDeviceFamily</key>
                |    <array>
                |        <integer>1</integer>
                |        <integer>2</integer>
                |    </array>

                """.trimMargin()
            KonanTarget.MACBOOK -> ""
            else -> error(target)
        })

        if (target == KonanTarget.IPHONE) {
            contents.append("""
                |    <key>UIRequiredDeviceCapabilities</key>
                |    <array>
                |        <string>arm64</string>
                |    </array>

                """.trimMargin()
            )
        }

        contents.append("""
            </dict>
            </plist>
        """.trimIndent())

        // TODO: Xcode also add some number of DT* keys.

        file.writeBytes(contents.toString().toByteArray())
    }
}

internal fun ModuleDescriptor.guessMainPackage(): FqName {
    val allPackages = this.getPackageFragments() // Includes also all parent packages, e.g. the root one.

    val nonEmptyPackages = allPackages
            .filter { it.getMemberScope().getContributedDescriptors().isNotEmpty() }
            .map { it.fqName }.distinct()

    return allPackages.map { it.fqName }.distinct()
            .filter { candidate -> nonEmptyPackages.all { it.isSubpackageOf(candidate) } }
            // Now there are all common ancestors of non-empty packages. Longest of them is the least common accessor:
            .maxBy { it.asString().length }!!
}
