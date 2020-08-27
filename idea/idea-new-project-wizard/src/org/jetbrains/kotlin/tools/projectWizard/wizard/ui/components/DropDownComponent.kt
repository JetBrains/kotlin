package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.componentWithCommentAtBottom
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JList

class DropDownComponent<T : DisplayableSettingItem>(
    context: Context,
    private val initialValues: List<T> = emptyList(),
    description: String? = null,
    initiallySelectedValue: T? = null,
    labelText: String? = null,
    private val filter: (T) -> Boolean = { true },
    private val validator: SettingValidator<T> = settingValidator { ValidationResult.OK },
    private val iconProvider: (T) -> Icon? = { null },
    onValueUpdate: (T) -> Unit = {}
) : UIComponent<T>(
    context,
    labelText,
    validator,
    onValueUpdate
) {

    @Suppress("UNCHECKED_CAST")
    private val combobox = ComboBox(
        initialValues.filter(filter).toTypedArray<DisplayableSettingItem>() as Array<T>
    ).apply {
        selectedItem = initiallySelectedValue
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
                if (this@apply.selectedItem != value) read {
                    validator.validate(this, value)
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

    override val alignTarget: JComponent? get() = combobox

    override val uiComponent = componentWithCommentAtBottom(combobox, description)

    fun filterValues() {
        val selectedItem = model.selectedItem

        safeUpdateUi {
            model.removeAllElements()
            initialValues.filter(filter).forEach(model::addElement)
            when {
                model.getIndexOf(selectedItem) != -1 -> {
                    model.selectedItem = selectedItem
                }
                model.size != 0 -> {
                    combobox.selectedIndex = 0
                    combobox.selectedItem?.let {
                        ApplicationManager.getApplication().invokeLater {
                            @Suppress("UNCHECKED_CAST")
                            forceValueUpdate(it as T)
                        }
                    }
                }
            }
        }
        uiComponent.repaint()
    }

    private val model get() = combobox.model as DefaultComboBoxModel

    val valuesCount
        get() = combobox.model.size

    override fun updateUiValue(newValue: T) = safeUpdateUi {
        combobox.selectedItem = newValue
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        filterValues()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getUiValue(): T? = combobox.selectedItem as? T
}
