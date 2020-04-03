package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import java.awt.Font

class CheckboxComponent(
    context: Context,
    labelText: String? = null,
    initialValue: Boolean? = null,
    validator: SettingValidator<Boolean>? = null,
    onValueUpdate: (Boolean) -> Unit = {}
) : UIComponent<Boolean>(
    context,
    labelText = null,
    validator = validator,
    onValueUpdate = onValueUpdate
) {
    override val uiComponent: JBCheckBox = JBCheckBox(labelText, initialValue ?: false).apply {
        font = UIUtil.getButtonFont()
        addChangeListener {
            fireValueUpdated(this@apply.isSelected)
        }
    }

    override fun updateUiValue(newValue: Boolean) = safeUpdateUi {
        uiComponent.isSelected = newValue
    }

    override fun getUiValue(): Boolean = uiComponent.isSelected
}