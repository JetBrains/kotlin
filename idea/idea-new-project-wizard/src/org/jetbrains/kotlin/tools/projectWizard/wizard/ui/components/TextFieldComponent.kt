package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.textField
import java.awt.BorderLayout
import javax.swing.JComponent

class TextFieldComponent(
    private val valuesReadingContext: ValuesReadingContext,
    private val initialText: String = "",
    labelText: String? = "",
    private val validator: StringValidator = settingValidator { ValidationResult.OK },
    private val onSuccessValueUpdate: (String) -> Unit = {},
    private val onAnyValueUpdate: (String) -> Unit = {}
) : Component() {
    private val validationIndicator = ValidationIndicator(defaultText = labelText, showText = true)
    private val textField = textField(initialText, this::fireValueChanged)

    override val component: JComponent = panel {
        add(validationIndicator, BorderLayout.NORTH)
        add(textField, BorderLayout.CENTER)
    }


    var value: String
        get() = textField.text
        set(value) {
            textField.text = value
            fireValueChanged(value)
        }

    private fun fireValueChanged(text: String) {
        validate(text)
        if (validationIndicator.validationState == ValidationResult.OK) {
            onSuccessValueUpdate(text)
        }
        onAnyValueUpdate(text)
    }

    override fun onInit() {
        super.onInit()
        validate(initialText)
    }

    private fun validate(text: String) {
        validationIndicator.validationState = validator.validate(valuesReadingContext, text)
    }
}