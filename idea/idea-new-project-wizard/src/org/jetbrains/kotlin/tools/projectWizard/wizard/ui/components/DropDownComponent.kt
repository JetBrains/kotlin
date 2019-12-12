package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.layout.selectedValueIs
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import java.awt.BorderLayout
import javax.swing.JComponent
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JList

class DropDownComponent<T : DisplayableSettingItem>(
    private val valuesReadingContext: ValuesReadingContext,
    private val initialValues: List<T> = emptyList(),
    labelText: String? = null,
    private val filter: (T) -> Boolean = { true },
    private val validator: SettingValidator<T> = settingValidator { ValidationResult.OK },
    private val iconProvider: (T) -> Icon? = { null },
    private val onAnyValueUpdate: (T) -> Unit = {}
) : Component() {
    @Suppress("UNCHECKED_CAST")

    private val validationIndicator = ValidationIndicator(defaultText = labelText, showText = true)

    private var allowFireEventWhenValueUpdated: Boolean = true

    private fun withoutActionFiring(action: () -> Unit) {
        val initialAllowFireEventWhenValueUpdated = allowFireEventWhenValueUpdated
        allowFireEventWhenValueUpdated = false
        action()
        allowFireEventWhenValueUpdated = initialAllowFireEventWhenValueUpdated
    }

    @Suppress("UNCHECKED_CAST")
    private val comboBox = ComboBox<T>().apply {
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
                if (!this@apply.selectedValueIs(value).invoke()) {
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

        addItemListener { e ->
            if (e?.stateChange == ItemEvent.SELECTED) {
                value = e.item as T
            }
        }
    }

    override fun onInit() {
        super.onInit()
        updateValues(initialValues)
    }

    fun updateValues(newValues: List<T>) {
        val newValuesFiltered = newValues.filter(filter)
        val oldValue = comboBox.selectedItem
        withoutActionFiring {
            @Suppress("UNCHECKED_CAST")
            comboBox.model = DefaultComboBoxModel(newValuesFiltered.toTypedArray<DisplayableSettingItem>() as Array<T>)
        }

        if (oldValue !in newValuesFiltered) {
            newValuesFiltered.firstOrNull()?.let { newValue ->
                value = newValuesFiltered.first()
            }
        }
    }

    override val component: JComponent = panel {
        add(validationIndicator, BorderLayout.NORTH)
        add(comboBox, BorderLayout.CENTER)
    }

    @Suppress("UNCHECKED_CAST")
    var value: T
        get() = comboBox.selectedItem as T
        set(value) {
            comboBox.selectedItem = value
            fireValueChanged(value)
        }

    private fun fireValueChanged(newValue: T) {
        validate(newValue)
        if (allowFireEventWhenValueUpdated) {
            onAnyValueUpdate(newValue)
        }
    }

    fun validate(value: T = this.value) {
        validationIndicator.validationState = validator.validate(valuesReadingContext, value)
    }
}

