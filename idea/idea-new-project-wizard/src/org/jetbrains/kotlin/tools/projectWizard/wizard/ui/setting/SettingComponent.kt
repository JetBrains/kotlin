package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Displayable
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent

abstract class SettingComponent<V : Any, T: SettingType<V>>(
    val reference: SettingReference<V, T>,
    private val valuesReadingContext: ValuesReadingContext
) : DynamicComponent(valuesReadingContext), Displayable {
    var value: V?
        get() = reference.value
        set(value) {
            reference.value = value
        }

    val setting: Setting<V, T>
        get() = with(valuesReadingContext.context) {
            with(reference) { getSetting() }
        }

    abstract val validationIndicator: ValidationIndicator?

    override fun onInit() {
        super.onInit()
        updateValidationState()
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        component.isVisible = setting.isActive(valuesReadingContext)
        updateValidationState()
    }

    private fun updateValidationState() {
        val value = value
        if (validationIndicator != null && value != null) {
            validationIndicator!!.validationState = setting.validator.validate(valuesReadingContext, value)
        }
    }
}
