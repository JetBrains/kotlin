/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

/**
 * [KotlinCompilerPluginsProvider] provides information about registered compiler plugins.
 *
 * The component is optional. If [KotlinCompilerPluginsProvider] is not implemented, the Analysis API engine will assume that no compiler
 * plugins are registered.
 */
public interface KotlinCompilerPluginsProvider : KotlinOptionalPlatformComponent {
    public enum class CompilerPluginType {
        /**
         * An assign expression alterer extension. See `FirAssignExpressionAltererExtension`.
         */
        ASSIGNMENT,
    }

    /**
     * Returns a possibly empty list of extensions of a base [extensionType] that compiler plugins have registered for [module].
     *
     * These extensions are used in addition to those provided by the extension descriptor's [ProjectExtensionDescriptor.getInstances].
     */
    public fun <T : Any> getRegisteredExtensions(module: KaModule, extensionType: ProjectExtensionDescriptor<T>): List<T>

    /**
     * Returns `true` if at least one plugin with the requested [pluginType] is registered, and `false` otherwise.
     */
    public fun isPluginOfTypeRegistered(module: KaModule, pluginType: CompilerPluginType): Boolean

    public companion object {
        public fun getInstance(project: Project): KotlinCompilerPluginsProvider? = project.serviceOrNull()
    }
}
