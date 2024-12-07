/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.compiler.plugin

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@Deprecated(
    message = "ComponentRegistrar is deprecated. Please use CompilerPluginRegistrar instead. Check https://youtrack.jetbrains.com/issue/KT-52665 for more details",
    replaceWith = ReplaceWith("CompilerPluginRegistrar", "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar"),
    level = DeprecationLevel.WARNING
)
@ExperimentalCompilerApi
interface ComponentRegistrar {
    companion object {
        @Suppress("DEPRECATION")
        val PLUGIN_COMPONENT_REGISTRARS: CompilerConfigurationKey<MutableList<ComponentRegistrar>> =
            CompilerConfigurationKey.create("plugin component registrars")
    }

    fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration)

    val supportsK2: Boolean
        get() = false
}
