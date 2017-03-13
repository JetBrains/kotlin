/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.compiler.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.StoragePathMacros.PROJECT_CONFIG_DIR
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.SettingConstants

abstract class BaseKotlinCompilerSettings<T : Any> protected constructor() : PersistentStateComponent<Element>, Cloneable {
    @Suppress("LeakingThis")
    var settings: T = createSettings()
        private set

    protected abstract fun createSettings(): T

    override fun getState() = XmlSerializer.serialize(settings, SKIP_DEFAULT_VALUES)

    override fun loadState(state: Element) {
        settings = XmlSerializer.deserialize(state, settings.javaClass) ?: createSettings()
    }

    public override fun clone(): Any = super.clone()

    companion object {
        const val KOTLIN_COMPILER_SETTINGS_PATH = PROJECT_CONFIG_DIR + "/" + SettingConstants.KOTLIN_COMPILER_SETTINGS_FILE

        private val SKIP_DEFAULT_VALUES = SkipDefaultValuesSerializationFilters(
                CommonCompilerArguments.createDefaultInstance(),
                K2JVMCompilerArguments.createDefaultInstance(),
                K2JSCompilerArguments.createDefaultInstance()
        )
    }
}
