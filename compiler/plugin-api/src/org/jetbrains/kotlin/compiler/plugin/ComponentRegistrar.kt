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
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.CompilerConfiguration

public trait ComponentRegistrar {
    default object {
        public val PLUGIN_COMPONENT_REGISTRARS: CompilerConfigurationKey<MutableList<ComponentRegistrar>> = CompilerConfigurationKey.create("plugin component registrars")
    }

    public fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration)
}
