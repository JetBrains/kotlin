/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider.CompilerPluginType
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.areCompilerPluginsSupported
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.fir.extensions.FirAssignExpressionAltererExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class KotlinStandaloneFirCompilerPluginsProvider(compilerConfiguration: CompilerConfiguration) : KotlinCompilerPluginsProvider {
    private val extensionStorage = CompilerPluginRegistrar.ExtensionStorage().apply {
        for (registrar in compilerConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)) {
            with(registrar) { registerExtensions(compilerConfiguration) }
        }
    }

    override fun <T : Any> getRegisteredExtensions(
        module: KaModule,
        extensionType: ProjectExtensionDescriptor<T>,
    ): List<T> {
        if (!module.areCompilerPluginsSupported()) {
            return emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        return (extensionStorage.registeredExtensions[extensionType] as? List<T>) ?: emptyList()
    }

    override fun isPluginOfTypeRegistered(module: KaModule, pluginType: CompilerPluginType): Boolean {
        val extension = when (pluginType) {
            CompilerPluginType.ASSIGNMENT -> FirAssignExpressionAltererExtension::class
        }

        return getRegisteredExtensions(module, FirExtensionRegistrarAdapter)
            .map { (it as FirExtensionRegistrar).configure() }
            .any { it.extensions[extension]?.isNotEmpty() == true }
    }
}
