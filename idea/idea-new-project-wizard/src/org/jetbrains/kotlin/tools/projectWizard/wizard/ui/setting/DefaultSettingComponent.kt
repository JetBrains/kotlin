package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.openapi.ui.ComboBox
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.label
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.nio.file.Path
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent


object DefaultSettingComponent {
    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> create(
        setting: SettingReference<V, T>,
        valuesReadingContext: ValuesReadingContext
    ): SettingComponent<V, T> = when (setting.type) {
        VersionSettingType::class ->
            VersionSettingComponent(
                setting as SettingReference<Version, VersionSettingType>,
                valuesReadingContext
            ) as SettingComponent<V, T>
        BooleanSettingType::class ->
            BooleanSettingComponent(
                setting as SettingReference<Boolean, BooleanSettingType>,
                valuesReadingContext
            ) as SettingComponent<V, T>
        DropDownSettingType::class ->
            DropdownSettingComponent(
                setting as SettingReference<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>,
                valuesReadingContext
            ) as SettingComponent<V, T>
        StringSettingType::class ->
            StringSettingComponent(
                setting as SettingReference<String, StringSettingType>,
                valuesReadingContext
            ) as SettingComponent<V, T>
        PathSettingType::class ->
            PathSettingComponent(
                setting as SettingReference<Path, PathSettingType>,
                valuesReadingContext
            ) as SettingComponent<V, T>
        else -> TODO(setting.type.qualifiedName!!)
    }
}

class VersionSettingComponent(
    reference: SettingReference<Version, VersionSettingType>,
    valuesReadingContext: ValuesReadingContext
) : SettingComponent<Version, VersionSettingType>(reference, valuesReadingContext) {
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
    private val valuesReadingContext: ValuesReadingContext
) : UIComponentDelegatingSettingComponent<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>(
    reference,
    valuesReadingContext
) {
    override val uiComponent = DropDownComponent(
        valuesReadingContext = valuesReadingContext,
        initialValues = setting.type.values,
        validator = setting.validator,
        filter = { value -> setting.type.filter(valuesReadingContext, reference, value) },
        labelText = setting.title,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class BooleanSettingComponent(
    reference: SettingReference<Boolean, BooleanSettingType>,
    valuesReadingContext: ValuesReadingContext
) : UIComponentDelegatingSettingComponent<Boolean, BooleanSettingType>(
    reference,
    valuesReadingContext
) {
    override val uiComponent = CheckboxComponent(
        valuesReadingContext = valuesReadingContext,
        labelText = setting.title,
        initialValue = null,
        validator = setting.validator,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class StringSettingComponent(
    reference: SettingReference<String, StringSettingType>,
    valuesReadingContext: ValuesReadingContext,
    showLabel: Boolean = true
) : UIComponentDelegatingSettingComponent<String, StringSettingType>(
    reference,
    valuesReadingContext
) {
    override val uiComponent = TextFieldComponent(
        valuesReadingContext = valuesReadingContext,
        initialValue = null,
        labelText = if (showLabel) setting.title else null,
        validator = setting.validator,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class PathSettingComponent(
    reference: SettingReference<Path, PathSettingType>,
    valuesReadingContext: ValuesReadingContext
) : UIComponentDelegatingSettingComponent<Path, PathSettingType>(
    reference,
    valuesReadingContext
) {
    override val uiComponent = PathFieldComponent(
        valuesReadingContext = valuesReadingContext,
        labelText = setting.title,
        validator = settingValidator { path -> setting.validator.validate(this, path) },
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}