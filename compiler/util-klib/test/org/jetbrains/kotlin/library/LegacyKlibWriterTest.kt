/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.LegacyKlibWriterTest.LegacyKlibWriterParameters
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties

class LegacyKlibWriterTest : AbstractKlibWriterTest<LegacyKlibWriterParameters>(::LegacyKlibWriterParameters) {
    class LegacyKlibWriterParameters : Parameters() {
        var nativeTargets: List<String> = emptyList()
    }

    @Test
    fun `Writing a klib with different native targets`() {
        listOf(
            listOf("a"),
            listOf("b", "Android_X64"),
            listOf("macos_x64", "linux_arm64", "ios_arm64"),
        ).forEach { nativeTargets ->
            BuiltInsPlatform.entries.forEach { builtInsPlatform ->
                runTestWithParameters {
                    this.builtInsPlatform = builtInsPlatform
                    this.nativeTargets = nativeTargets
                }
            }
        }
    }

    context(properties: org.jetbrains.kotlin.konan.properties.Properties)
    override fun customizeManifestForMockKlib(parameters: LegacyKlibWriterParameters) {
        super.customizeManifestForMockKlib(parameters)
        if (parameters.nativeTargets.isNotEmpty() && parameters.builtInsPlatform == BuiltInsPlatform.NATIVE) {
            properties[KLIB_PROPERTY_NATIVE_TARGETS] = parameters.nativeTargets.joinToString(" ")
        }
    }

    override fun writeKlib(parameters: LegacyKlibWriterParameters): File {
        val klibPath = buildKotlinLibrary(
            linkDependencies = KlibLoader { libraryPaths(parameters.dependencies.map { it.path }) }.load().librariesStdlibFirst,
            metadata = parameters.metadata,
            ir = if (parameters.ir != null || parameters.irOfInlinableFunctions != null) {
                SerializedIrModule(parameters.ir.orEmpty(), parameters.irOfInlinableFunctions)
            } else null,
            versions = KotlinLibraryVersioning(
                compilerVersion = parameters.compilerVersion,
                metadataVersion = parameters.metadataVersion,
                abiVersion = parameters.abiVersion,
            ),
            output = createNewKlibDir().absolutePath,
            moduleName = parameters.uniqueName,
            nopack = parameters.nopack,
            manifestProperties = Properties().apply {
                parameters.customManifestProperties.forEach { (key, value) -> setProperty(key, value) }
            },
            builtInsPlatform = parameters.builtInsPlatform,
            nativeTargets = parameters.nativeTargets,
        ).libFile.absolutePath

        return File(klibPath)
    }
}