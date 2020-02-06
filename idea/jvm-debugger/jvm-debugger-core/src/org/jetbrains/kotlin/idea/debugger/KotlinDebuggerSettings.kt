/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

@State(name = "KotlinDebuggerSettings", storages = [Storage("kotlin_debug.xml")])
class KotlinDebuggerSettings : XDebuggerSettings<KotlinDebuggerSettings>("kotlin_debugger"), Getter<KotlinDebuggerSettings> {
    var renderDelegatedProperties: Boolean = false
    var disableKotlinInternalClasses: Boolean = true
    var isFilterForStdlibAlreadyAdded: Boolean = false
    var debugDisableCoroutineAgent: Boolean = false

    companion object {
        fun getInstance(): KotlinDebuggerSettings {
            return XDebuggerUtil.getInstance()?.getDebuggerSettings(KotlinDebuggerSettings::class.java)!!
        }
    }

    override fun createConfigurables(category: DebuggerSettingsCategory): Collection<Configurable?> = when (category) {
        DebuggerSettingsCategory.STEPPING ->
            listOf(
                SimpleConfigurable.create(
                    "reference.idesettings.debugger.kotlin.stepping",
                    "Kotlin",
                    KotlinSteppingConfigurableUi::class.java,
                    this
                )
            )
        DebuggerSettingsCategory.DATA_VIEWS ->
            listOf(
                SimpleConfigurable.create(
                    "reference.idesettings.debugger.kotlin.data.view",
                    "Kotlin",
                    KotlinDelegatedPropertyRendererConfigurableUi::class.java,
                    this
                )
            )
        else -> listOf()
    }

    override fun getState() = this
    override fun get() = this

    override fun loadState(state: KotlinDebuggerSettings) {
        XmlSerializerUtil.copyBean<KotlinDebuggerSettings>(state, this)
    }
}
