package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.DropDownComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.PathFieldComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.TextFieldComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.label
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent


sealed class DefaultSettingComponent<V : Any, T : SettingType<V>>(
    reference: SettingReference<V, T>,
    valuesReadingContext: ValuesReadingContext
) : SettingComponent<V, T>(reference, valuesReadingContext) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <V : Any, T : SettingType<V>> create(
            setting: SettingReference<V, T>,
            valuesReadingContext: ValuesReadingContext
        ): DefaultSettingComponent<V, T> = when (setting.type) {
            VersionSettingType::class ->
                VersionSettingComponent(
                    setting as SettingReference<Version, VersionSettingType>,
                    valuesReadingContext
                ) as DefaultSettingComponent<V, T>
            BooleanSettingType::class ->
                BooleanSettingComponent(
                    setting as SettingReference<Boolean, BooleanSettingType>,
                    valuesReadingContext
                ) as DefaultSettingComponent<V, T>
            DropDownSettingType::class ->
                DropdownSettingComponent(
                    setting as SettingReference<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>,
                    valuesReadingContext
                ) as DefaultSettingComponent<V, T>
            StringSettingType::class ->
                StringSettingComponent(
                    setting as SettingReference<String, StringSettingType>,
                    valuesReadingContext
                ) as DefaultSettingComponent<V, T>
            PathSettingType::class ->
                PathSettingComponent(
                    setting as SettingReference<Path, PathSettingType>,
                    valuesReadingContext
                ) as DefaultSettingComponent<V, T>
            else -> TODO(setting.type.qualifiedName!!)
        }
    }
}


class VersionSettingComponent(
    reference: SettingReference<Version, VersionSettingType>,
    valuesReadingContext: ValuesReadingContext
) : DefaultSettingComponent<Version, VersionSettingType>(reference, valuesReadingContext) {
    private val settingLabel = label(setting.title)
    private val comboBox = ComboBox<Version>().apply {
        addItemListener { e ->
            if (e?.stateChange == ItemEvent.SELECTED) {
                value = e.item as Version
            }
        }
    }

    override fun onInit() {
        super.onInit()
        val values = setting.defaultValue?.let(::listOf).orEmpty()
        comboBox.model = DefaultComboBoxModel<Version>(values.toTypedArray())

        if (values.isNotEmpty()) {
            value = values.first()
        }
    }

    override val validationIndicator = ValidationIndicator(showText = false)
    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            add(
                panel {
                    add(validationIndicator, BorderLayout.WEST)
                    add(settingLabel, BorderLayout.CENTER)
                },
                BorderLayout.WEST
            )
            add(comboBox, BorderLayout.CENTER)
        }
    }
}

class DropdownSettingComponent(
    reference: SettingReference<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>,
    valuesReadingContext: ValuesReadingContext
) : DefaultSettingComponent<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>(
    reference,
    valuesReadingContext
) {
    private val dropDownComponent = DropDownComponent(
        valuesReadingContext,
        setting.type.values,
        labelText = setting.title,
        onAnyValueUpdate = { newValue ->
            value = newValue
        }
    ).asSubComponent()


    override fun onInit() {
        super.onInit()
        val valuesFiltered = setting.type.values.filter { setting.type.filter(reference, it) }
        dropDownComponent.updateValues(valuesFiltered)
        if (valuesFiltered.isNotEmpty()) {
            value = valuesFiltered.first()
        }
    }

    override val validationIndicator: ValidationIndicator? = null
    override val component: JComponent = dropDownComponent.component
}

class BooleanSettingComponent(
    reference: SettingReference<Boolean, BooleanSettingType>,
    valuesReadingContext: ValuesReadingContext
) : DefaultSettingComponent<Boolean, BooleanSettingType>(reference, valuesReadingContext) {
    private val checkBox = JBCheckBox(setting.title).apply {
        addChangeListener {
            value = this@apply.isSelected
        }
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        checkBox.isSelected = value ?: false
    }

    override val validationIndicator = ValidationIndicator(showText = false)

    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            // TODO add validation indicator
            add(checkBox, BorderLayout.CENTER)
        }
    }
}

class StringSettingComponent(
    reference: SettingReference<String, StringSettingType>,
    valuesReadingContext: ValuesReadingContext,
    showLabel: Boolean = true
) : DefaultSettingComponent<String, StringSettingType>(reference, valuesReadingContext) {
    private val textFieldComponent = TextFieldComponent(
        valuesReadingContext,
        setting.defaultValue.orEmpty(),
        labelText = if (showLabel) setting.title else null,
        validator = setting.validator,
        onAnyValueUpdate = {
            value = it
        }
    ).asSubComponent()

    override val validationIndicator: ValidationIndicator? = null

    override fun onInit() {
        super.onInit()
        textFieldComponent.value = value.orEmpty()
    }

    override val component = textFieldComponent.component
}

class PathSettingComponent(
    reference: SettingReference<Path, PathSettingType>,
    valuesReadingContext: ValuesReadingContext
) : DefaultSettingComponent<Path, PathSettingType>(reference, valuesReadingContext) {
    private val pathFieldComponent = PathFieldComponent(
        valuesReadingContext,
        "",
        setting.title,
        validator = settingValidator { path ->
            setting.validator.validate(this, Paths.get(path))
        },
        onAnyValueUpdate = {
            value = Paths.get(it)
        }
    ).asSubComponent()

    override val validationIndicator: ValidationIndicator? = null

    override val component = pathFieldComponent.component
}