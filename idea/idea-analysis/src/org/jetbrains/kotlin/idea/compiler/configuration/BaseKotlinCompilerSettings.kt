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
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.SettingConstants
import kotlin.reflect.KClass

abstract class BaseKotlinCompilerSettings<T : Freezable> protected constructor() : PersistentStateComponent<Element>, Cloneable {
    @Suppress("LeakingThis", "UNCHECKED_CAST")
    private var _settings: T = createSettings().frozen() as T
        private set(value) {
            field = value.frozen() as T
        }

    var settings: T
        get() = _settings
        set(value) {
            validateNewSettings(value)
            @Suppress("UNCHECKED_CAST")
            _settings = value
        }

    fun update(changer: T.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        settings = (settings.unfrozen() as T).apply { changer() }
    }

    protected fun validateInheritedFieldsUnchanged(settings: T) {
        @Suppress("UNCHECKED_CAST")
        val inheritedProperties = collectProperties<T>(settings::class as KClass<T>, true)
        val defaultInstance = createSettings()
        val invalidFields = inheritedProperties.filter { it.get(settings) != it.get(defaultInstance) }
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
                CommonCompilerArguments.DummyImpl(),
                K2JVMCompilerArguments(),
                K2JSCompilerArguments()
        )
    }
}
