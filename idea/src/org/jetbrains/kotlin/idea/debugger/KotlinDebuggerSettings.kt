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

package org.jetbrains.kotlin.idea.debugger


import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SimpleConfigurable
import com.intellij.openapi.util.Getter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.settings.DebuggerSettingsCategory
import com.intellij.xdebugger.settings.XDebuggerSettings
import org.jetbrains.kotlin.idea.debugger.stepping.KotlinSteppingConfigurableUi

@State(name = "KotlinDebuggerSettings", storages = arrayOf(Storage(file = StoragePathMacros.APP_CONFIG + "/kotlin_debug.xml"))) class KotlinDebuggerSettings : XDebuggerSettings<KotlinDebuggerSettings>("kotlin_debugger"), Getter<KotlinDebuggerSettings> {
    var DEBUG_RENDER_DELEGATED_PROPERTIES: Boolean = true
    var DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES: Boolean = true
    var DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED: Boolean = false

    companion object {
        fun getInstance(): KotlinDebuggerSettings {
            return XDebuggerUtil.getInstance()?.getDebuggerSettings(KotlinDebuggerSettings::class.java)!!
        }
    }

    override fun createConfigurables(category: DebuggerSettingsCategory): Collection<Configurable?> {
        return when (category) {
            DebuggerSettingsCategory.STEPPING ->
                listOf(SimpleConfigurable.create(
                        "reference.idesettings.debugger.kotlin.stepping",
                        "Kotlin",
                        KotlinSteppingConfigurableUi::class.java,
                        this))
            DebuggerSettingsCategory.DATA_VIEWS ->
                listOf(SimpleConfigurable.create(
                        "reference.idesettings.debugger.kotlin.data.view",
                        "Kotlin",
                        KotlinDelegatedPropertyRendererConfigurableUi::class.java,
                        this))
            else -> listOf()
        }
    }

    override fun getState() = this
    override fun get() = this

    override fun loadState(state: KotlinDebuggerSettings?) {
        if (state != null) XmlSerializerUtil.copyBean<KotlinDebuggerSettings>(state, this)
    }
}
