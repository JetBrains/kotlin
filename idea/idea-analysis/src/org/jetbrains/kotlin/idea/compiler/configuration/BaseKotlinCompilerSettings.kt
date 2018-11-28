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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.ReflectionUtil
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.Accessor
import com.intellij.util.xmlb.SerializationFilterBase
import com.intellij.util.xmlb.XmlSerializer
import gnu.trove.THashMap
import org.jdom.Element
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.idea.syncPublisherWithDisposeCheck
import kotlin.reflect.KClass

abstract class BaseKotlinCompilerSettings<T : Freezable> protected constructor(private val project: Project) : PersistentStateComponent<Element>, Cloneable {
    // Based on com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters
    private object DefaultValuesFilter : SerializationFilterBase() {
        private val defaultBeans = THashMap<Class<*>, Any>()

        private fun createDefaultBean(beanClass: Class<Any>): Any {
            return ReflectionUtil.newInstance<Any>(beanClass).apply {
                if (this is K2JSCompilerArguments) {
                    sourceMapPrefix = ""
                }
            }
        }

        private fun getDefaultValue(accessor: Accessor, bean: Any): Any? {
            if (bean is K2JSCompilerArguments && accessor.name == K2JSCompilerArguments::sourceMapEmbedSources.name) {
                return if (bean.sourceMap) K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING else null
            }

            val beanClass = bean.javaClass
            val defaultBean = defaultBeans.getOrPut(beanClass) { createDefaultBean(beanClass) }
            return accessor.read(defaultBean)
        }

        override fun accepts(accessor: Accessor, bean: Any, beanValue: Any?): Boolean {
            val defValue = getDefaultValue(accessor, bean)
            return if (defValue is Element && beanValue is Element) {
                !JDOMUtil.areElementsEqual(beanValue, defValue)
            } else {
                !Comparing.equal(beanValue, defValue)
            }
        }
    }

    @Suppress("LeakingThis", "UNCHECKED_CAST")
    private var _settings: T = createSettings().frozen() as T
        private set(value) {
            field = value.frozen() as T
        }

    var settings: T
        get() = _settings
        set(value) {
            validateNewSettings(value)
            _settings = value

            project.syncPublisherWithDisposeCheck(KotlinCompilerSettingsListener.TOPIC).settingsChanged(value)
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

    override fun getState() = XmlSerializer.serialize(_settings, DefaultValuesFilter)

    override fun loadState(state: Element) {
        _settings = ReflectionUtil.newInstance(_settings.javaClass).apply {
            if (this is CommonCompilerArguments) {
                freeArgs = mutableListOf()
                internalArguments = mutableListOf()
            }
            XmlSerializer.deserializeInto(this, state)
        }

        project.syncPublisherWithDisposeCheck(KotlinCompilerSettingsListener.TOPIC).settingsChanged(settings)
    }

    public override fun clone(): Any = super.clone()
}

interface KotlinCompilerSettingsListener {
    fun <T> settingsChanged(newSettings: T)

    companion object {
        val TOPIC = Topic.create("KotlinCompilerSettingsListener", KotlinCompilerSettingsListener::class.java)
    }
}