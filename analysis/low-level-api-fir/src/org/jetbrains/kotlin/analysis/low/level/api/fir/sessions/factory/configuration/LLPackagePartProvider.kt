/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinModuleMappingFileProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartProviderBase
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.resolve.CommonCompilerDeserializationConfiguration
import java.io.EOFException

/**
 * Provides package parts from the `.kotlin_module` files found by [KotlinModuleMappingFileProvider].
 */
internal class LLPackagePartProvider(
    private val moduleMappingFiles: Collection<VirtualFile>,
    languageVersionSettings: LanguageVersionSettings,
) : JvmPackagePartProviderBase<VirtualFile>() {
    override val deserializationConfiguration = CommonCompilerDeserializationConfiguration(languageVersionSettings)

    override val loadedModules: MutableList<ModuleMappingInfo<VirtualFile>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        moduleMappingFiles.mapNotNullTo(mutableListOf()) { file ->
            val mapping = loadModuleMapping(file) ?: return@mapNotNullTo null

            // The base class merges the package parts of mappings with the same key, which should happen for mappings of the same root.
            ModuleMappingInfo(key = file.parent ?: file, mapping, name = file.nameWithoutExtension)
        }
    }

    private fun loadModuleMapping(file: VirtualFile): ModuleMapping? =
        try {
            ModuleMapping.loadModuleMapping(file.contentsToByteArray(), file.toString(), deserializationConfiguration) {
                // A mapping with an incompatible metadata version is loaded as an empty mapping.
            }
        } catch (_: EOFException) {
            null
        }

    override fun mayHaveOptionalAnnotationClasses(): Boolean = moduleMappingFiles.isNotEmpty()
}
