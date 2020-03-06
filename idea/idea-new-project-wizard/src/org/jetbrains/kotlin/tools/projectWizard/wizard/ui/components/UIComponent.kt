/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.FocusableComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.label
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ErrorAwareComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.IdeaBasedComponentValidator
import java.awt.BorderLayout
import javax.swing.JComponent

abstract class UIComponent<V : Any>(
    ideContext: IdeContext,
    labelText: String? = null,
    private val validator: SettingValidator<V>? = null,
    private val onValueUpdate: (V) -> Unit = {}
) : DynamicComponent(ideContext), ErrorAwareComponent, FocusableComponent, Disposable {
    private val validationIndicator by lazy(LazyThreadSafetyMode.NONE) {
        if (validator != null)
            IdeaBasedComponentValidator(this, getValidatorTarget())
        else null
    }

    override fun dispose() {}

    protected abstract val uiComponent: JComponent

    protected open fun getValidatorTarget() = uiComponent

    abstract fun updateUiValue(newValue: V)
    abstract fun getUiValue(): V?

    private var allowEventFiring = true

    protected fun fireValueUpdated(value: V) {
        if (allowEventFiring) {
            onValueUpdate(value)
        }
        validate(value)
    }

    override fun onInit() {
        super.onInit()
        getUiValue()?.let(::validate)
    }

    final override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            labelText?.let { add(label("$it:"), BorderLayout.NORTH) }
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
        read {
            validationIndicator?.updateValidationState(validator.validate(this, value))
        }
    }

    override fun focusOn() {
        uiComponent.requestFocus()
    }

    override fun findComponentWithError(error: ValidationResult.ValidationError): FocusableComponent? = takeIf {
        read {
            getUiValue()?.let { validator?.validate?.invoke(this, it)?.isSpecificError(error) } == true
        }
    }
}

fun <V : Any> ReadingContext.valueForSetting(
    uiComponent: UIComponent<V>,
    setting: SettingReference<V, SettingType<V>>
): V? = setting.savedOrDefaultValue ?: uiComponent.getUiValue()