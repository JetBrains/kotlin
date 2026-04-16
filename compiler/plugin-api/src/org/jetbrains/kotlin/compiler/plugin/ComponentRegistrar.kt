/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugin

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@Deprecated(
    message = "ComponentRegistrar is deprecated. Please use CompilerPluginRegistrar instead. Check https://youtrack.jetbrains.com/issue/KT-52665 for more details",
    replaceWith = ReplaceWith("CompilerPluginRegistrar", "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar"),
    level = DeprecationLevel.ERROR
)
@ExperimentalCompilerApi
interface ComponentRegistrar {
    companion object {
        @Suppress("DEPRECATION_ERROR")
        val PLUGIN_COMPONENT_REGISTRARS: CompilerConfigurationKey<MutableList<ComponentRegistrar>> =
            CompilerConfigurationKey.create("PLUGIN_COMPONENT_REGISTRARS")
    }

    fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration)

    val supportsK2: Boolean
        get() = false
}
