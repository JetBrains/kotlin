/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Setting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.BorderLayout
import javax.swing.JComponent

abstract class UIComponent<V : Any>(
    private val valuesReadingContext: ValuesReadingContext,
    labelText: String? = null,
    private val validator: SettingValidator<V>? = null,
    private val onValueUpdate: (V) -> Unit = {}
) : DynamicComponent(valuesReadingContext) {
    private val validationIndicator = if (validator != null)
        ValidationIndicator(defaultText = labelText, showText = true)
    else null

    protected abstract val uiComponent: JComponent

    abstract fun updateUiValue(newValue: V)
    abstract fun getUiValue(): V

    private var allowEventFiring = true

    protected fun fireValueUpdated(value: V) {
        if (allowEventFiring) {
            onValueUpdate(value)
        }
        validate(value)
    }

    override fun onInit() {
        super.onInit()
        validate(getUiValue())
    }

    final override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            validationIndicator?.let { add(it, BorderLayout.NORTH) }
            add(uiComponent, BorderLayout.CENTER)
        }
    }

    protected fun safeUpdateUi(updater: () -> Unit) {
        val allowEventFiringSaved = allowEventFiring
        allowEventFiring = false
        updater()
        allowEventFiring = allowEventFiringSaved
    }

    fun validate(value: V) {
        if (validator == null) return
        if (validationIndicator == null) return
        validationIndicator.validationState = validator.validate(valuesReadingContext, value)
    }
}

fun <V: Any> UIComponent<V>.valueForSetting(setting: Setting<V, SettingType<V>>): V =
    setting.defaultValue ?:  getUiValue()