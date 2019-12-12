package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.textField
import java.awt.BorderLayout
import javax.swing.JComponent

class PathFieldComponent(
    private val valuesReadingContext: ValuesReadingContext,
    private val initialText: String = "",
    labelText: String? = "",
    private val validator: StringValidator = org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator {  ValidationResult.OK },
    private val onSuccessValueUpdate: (String) -> Unit = {},
    private val onAnyValueUpdate: (String) -> Unit = {}
) : Component() {
    private val validationIndicator = ValidationIndicator(defaultText = labelText, showText = true)
    private val pathField = TextFieldWithBrowseButton(
        textField(initialText, this::fireValueChanged)
    ).apply {
        addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                null
            )
        )
    }

    override val component: JComponent = panel {
        add(validationIndicator, BorderLayout.NORTH)
        add(pathField, BorderLayout.CENTER)
    }


    var value: String
        get() = pathField.text
        set(value) {
            pathField.text = value
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