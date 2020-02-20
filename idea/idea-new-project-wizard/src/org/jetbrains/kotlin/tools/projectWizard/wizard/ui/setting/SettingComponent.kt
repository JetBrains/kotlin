package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import org.jetbrains.kotlin.tools.projectWizard.core.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Displayable
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.FocusableComponent

abstract class SettingComponent<V : Any, T: SettingType<V>>(
    val reference: SettingReference<V, T>,
    private val readingContext: ReadingContext
) : DynamicComponent(readingContext), Displayable, FocusableComponent {
    var value: V?
        get() = reference.value
        set(value) {
            reference.value = value
        }

    val setting: Setting<V, T>
        get() = with(readingContext.context) {
            with(reference) { getSetting() }
        }

    abstract val validationIndicator: ValidationIndicator?

    override fun onInit() {
        super.onInit()
        updateValidationState()
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        component.isVisible = setting.isActive(readingContext)
        updateValidationState()
    }

    private fun updateValidationState() {
        val value = value
        if (validationIndicator != null && value != null) {
            validationIndicator!!.updateValidationState(setting.validator.validate(readingContext, value))
        }
    }
}
