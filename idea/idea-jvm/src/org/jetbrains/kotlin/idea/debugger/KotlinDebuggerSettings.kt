/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger


import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SimpleConfigurable
import com.intellij.openapi.util.Getter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.settings.DebuggerSettingsCategory
import com.intellij.xdebugger.settings.XDebuggerSettings
import org.jetbrains.kotlin.idea.debugger.stepping.KotlinSteppingConfigurableUi

@State(name = "KotlinDebuggerSettings", storages = arrayOf(Storage("kotlin_debug.xml")))
class KotlinDebuggerSettings : XDebuggerSettings<KotlinDebuggerSettings>("kotlin_debugger"), Getter<KotlinDebuggerSettings> {
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

    override fun loadState(state: KotlinDebuggerSettings) {
        XmlSerializerUtil.copyBean<KotlinDebuggerSettings>(state, this)
    }
}
