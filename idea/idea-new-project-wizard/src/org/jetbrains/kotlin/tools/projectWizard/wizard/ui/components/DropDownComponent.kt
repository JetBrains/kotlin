package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JComboBox
import javax.swing.JList

class DropDownComponent<T : DisplayableSettingItem>(
    private val valuesReadingContext: ValuesReadingContext,
    initialValues: List<T> = emptyList(),
    labelText: String? = null,
    private val filter: (T) -> Boolean = { true },
    private val validator: SettingValidator<T> = settingValidator { ValidationResult.OK },
    private val iconProvider: (T) -> Icon? = { null },
    onValueUpdate: (T) -> Unit = {}
) : UIComponent<T>(
    valuesReadingContext,
    labelText,
    validator,
    onValueUpdate
) {
    @Suppress("UNCHECKED_CAST")
    override val uiComponent: ComboBox<T> = ComboBox<T>(
        initialValues.filter(filter).toTypedArray<DisplayableSettingItem>() as Array<T>
    ).apply {
        renderer = object : ColoredListCellRenderer<T>() {
            override fun customizeCellRenderer(
                list: JList<out T>,
                value: T?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                if (value == null) return
                icon = iconProvider(value)
                append(value.text)
                value.greyText?.let {
                    append(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                if (this@apply.selectedItem != value) {
                    validator.validate(valuesReadingContext, value)
                        .safeAs<ValidationResult.ValidationError>()
                        ?.messages
                        ?.firstOrNull()
                        ?.let { error ->
                            append(" $error.", SimpleTextAttributes.ERROR_ATTRIBUTES)
                        }
                }
            }
        }

        addItemListener { event ->
            if (event?.stateChange == ItemEvent.SELECTED) {
                @Suppress("UNCHECKED_CAST")
                (event.item as? T)?.let(::fireValueUpdated)
            }
        }
    }

    fun setValues(newValues: List<T>) {
        @Suppress("UNCHECKED_CAST")
        uiComponent.model = DefaultComboBoxModel(newValues.filter(filter).toTypedArray<DisplayableSettingItem>() as Array<T>)
    }

    override fun updateUiValue(newValue: T) = safeUpdateUi {
        uiComponent.selectedItem = newValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getUiValue(): T? = uiComponent.selectedItem as? T
}

