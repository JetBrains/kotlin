/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList
import org.jetbrains.kotlin.cli.CliDiagnostics.JAVA_MODULE_RESOLUTION_ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartProviderBase
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.resolve.JvmCompilerDeserializationConfiguration
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.PrintStream

class JvmPackagePartProvider(
    languageVersionSettings: LanguageVersionSettings,
    private val scope: GlobalSearchScope
) : JvmPackagePartProviderBase<VirtualFile>() {
    override val deserializationConfiguration = JvmCompilerDeserializationConfiguration(languageVersionSettings)

    override val loadedModules: MutableList<ModuleMappingInfo<VirtualFile>> = SmartList()

    private var allPackageNamesCache: Set<String>? = null

    override val allPackageNames: Set<String>
        // assuming that the modifications of loadedModules happen in predictable moments now, so no synchronization is used
        get() = allPackageNamesCache
            ?: loadedModules.flatMapTo(mutableSetOf()) { it.mapping.packageFqName2Parts.keys }
                .also { allPackageNamesCache = it }

    // TODO: redesign to avoid cache-unfriendly usages, see KT-76516
    fun addRoots(roots: List<JavaRoot>, configuration: CompilerConfiguration) {
        for ((root, type) in roots) {
            if (type != JavaRoot.RootType.BINARY) continue
            if (root !in scope) continue

            val metaInf = root.findChild("META-INF") ?: continue
            for (moduleFile in metaInf.children) {
                if (!moduleFile.name.endsWith(ModuleMapping.MAPPING_FILE_EXT)) continue

                tryLoadModuleMapping(
                    { moduleFile.contentsToByteArray() }, moduleFile.toString(), moduleFile.path,
                    deserializationConfiguration, configuration
                )?.let {
                    loadedModules.add(ModuleMappingInfo(root, it, moduleFile.nameWithoutExtension))
                    allPackageNamesCache = null
                }
            }
        }
    }
}

fun tryLoadModuleMapping(
    getModuleBytes: () -> ByteArray,
    debugName: String,
    modulePath: String,
    deserializationConfiguration: JvmCompilerDeserializationConfiguration,
    configuration: CompilerConfiguration
): ModuleMapping? = try {
    ModuleMapping.loadModuleMapping(getModuleBytes(), debugName, deserializationConfiguration) { incompatibleVersion ->
        configuration.report(
            JAVA_MODULE_RESOLUTION_ERROR,
            "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is " +
                    "$incompatibleVersion, expected version is ${MetadataVersion.INSTANCE}.",
            CompilerMessageLocation.create(modulePath)
        )
    }
} catch (e: EOFException) {
    configuration.report(
        JAVA_MODULE_RESOLUTION_ERROR,
        "Error occurred when reading the module: ${e.message}", CompilerMessageLocation.create(modulePath)
    )
    configuration.reportLog(
        String(ByteArrayOutputStream().also { e.printStackTrace(PrintStream(it)) }.toByteArray()),
        CompilerMessageLocation.create(modulePath)
    )
    null
}
