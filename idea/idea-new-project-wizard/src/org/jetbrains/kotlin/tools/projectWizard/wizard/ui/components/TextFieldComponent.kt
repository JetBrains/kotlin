package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.textField

class TextFieldComponent(
    context: Context,
    labelText: String? = null,
    initialValue: String? = null,
    validator: SettingValidator<String>? = null,
    onValueUpdate: (String) -> Unit = {}
) : UIComponent<String>(
    context,
    labelText,
    validator,
    onValueUpdate
) {
    private var isDisabled: Boolean = false
    private var cachedValueWhenDisabled: String? = null

    override val uiComponent: JBTextField = textField(initialValue.orEmpty(), ::fireValueUpdated)

    override fun updateUiValue(newValue: String) = safeUpdateUi {
        uiComponent.text = newValue
    }

    fun disable(@Nls message: String) {
        cachedValueWhenDisabled = getUiValue()
        uiComponent.isEditable = false
        uiComponent.foreground = UIUtil.getLabelDisabledForeground()
        isDisabled = true
        updateUiValue(message)
    }

    override fun validate(value: String) {
        if (isDisabled) return
        super.validate(value)
    }

    override fun getUiValue(): String = cachedValueWhenDisabled ?: uiComponent.text
}