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
import org.jetbrains.kotlin.cli.common.arguments.collectFieldsToCopy
import org.jetbrains.kotlin.cli.common.arguments.copyBean
import org.jetbrains.kotlin.config.SettingConstants

abstract class BaseKotlinCompilerSettings<T : Any> protected constructor() : PersistentStateComponent<Element>, Cloneable {
    @Suppress("LeakingThis")
    private var _settings: T = createSettings()

    var settings: T
        get() = copyBean(_settings)
        set(value) {
            validateNewSettings(value)
            _settings = copyBean(value)
        }

    fun update(changer: T.() -> Unit) {
        settings = settings.apply { changer() }
    }

    protected fun validateInheritedFieldsUnchanged(settings: T) {
        val inheritedFields = collectFieldsToCopy(settings.javaClass, true)
        val defaultInstance = createSettings()
        val invalidFields = inheritedFields.filter { it.get(settings) != it.get(defaultInstance) }
        if (invalidFields.isNotEmpty()) {
            throw IllegalArgumentException("Following fields are expected to be left unchanged in ${settings.javaClass}: ${invalidFields.joinToString { it.name }}")
        }
    }

    protected open fun validateNewSettings(settings: T) {

    }

    protected abstract fun createSettings(): T

    override fun getState() = XmlSerializer.serialize(_settings, SKIP_DEFAULT_VALUES)

    override fun loadState(state: Element) {
        _settings = XmlSerializer.deserialize(state, _settings.javaClass) ?: createSettings()
    }

    public override fun clone(): Any = super.clone()

    companion object {
        const val KOTLIN_COMPILER_SETTINGS_PATH = PROJECT_CONFIG_DIR + "/" + SettingConstants.KOTLIN_COMPILER_SETTINGS_FILE

        private val SKIP_DEFAULT_VALUES = SkipDefaultValuesSerializationFilters(
                KotlinCommonCompilerArgumentsHolder.createDefaultArguments(),
                Kotlin2JvmCompilerArgumentsHolder.createDefaultArguments(),
                Kotlin2JsCompilerArgumentsHolder.createDefaultArguments()
        )
    }
}
