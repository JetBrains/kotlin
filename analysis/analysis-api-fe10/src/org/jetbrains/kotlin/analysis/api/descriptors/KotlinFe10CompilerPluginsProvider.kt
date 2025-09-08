/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider.CompilerPluginType
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.areCompilerPluginsSupported
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension

@Suppress("unused")
internal class KotlinFe10CompilerPluginsProvider : KotlinCompilerPluginsProvider {
    override fun <T : Any> getRegisteredExtensions(module: KaModule, extensionType: ProjectExtensionDescriptor<T>): List<T> {
        if (!module.areCompilerPluginsSupported()) {
            return emptyList()
        }

        return extensionType.getInstances(module.project)
    }

    @OptIn(InternalNonStableExtensionPoints::class)
    override fun isPluginOfTypeRegistered(module: KaModule, pluginType: CompilerPluginType): Boolean {
        val extension = when (pluginType) {
            CompilerPluginType.ASSIGNMENT -> AssignResolutionAltererExtension
        }
        return getRegisteredExtensions(module, extension).isNotEmpty()
    }
}
