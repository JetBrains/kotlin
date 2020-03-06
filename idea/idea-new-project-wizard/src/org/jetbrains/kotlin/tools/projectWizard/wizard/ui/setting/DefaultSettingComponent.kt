package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.openapi.ui.ComboBox
import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
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
        ideContext: IdeContext
    ): SettingComponent<V, T> = when (setting.type) {
        VersionSettingType::class ->
            VersionSettingComponent(
                setting as SettingReference<Version, VersionSettingType>,
                ideContext
            ) as SettingComponent<V, T>
        BooleanSettingType::class ->
            BooleanSettingComponent(
                setting as SettingReference<Boolean, BooleanSettingType>,
                ideContext
            ) as SettingComponent<V, T>
        DropDownSettingType::class ->
            DropdownSettingComponent(
                setting as SettingReference<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>,
                ideContext
            ) as SettingComponent<V, T>
        StringSettingType::class ->
            StringSettingComponent(
                setting as SettingReference<String, StringSettingType>,
                ideContext
            ) as SettingComponent<V, T>
        PathSettingType::class ->
            PathSettingComponent(
                setting as SettingReference<Path, PathSettingType>,
                ideContext
            ) as SettingComponent<V, T>
        else -> TODO(setting.type.qualifiedName!!)
    }
}

class VersionSettingComponent(
    reference: SettingReference<Version, VersionSettingType>,
    ideContext: IdeContext
) : SettingComponent<Version, VersionSettingType>(reference, ideContext) {
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
        val values = read { reference.savedOrDefaultValue }?.let(::listOf).orEmpty()
        comboBox.model = DefaultComboBoxModel<Version>(values.toTypedArray())

        if (values.isNotEmpty()) {
            value = values.first()
        }
    }

    override val validationIndicator = null
    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            add(
                panel {
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
    ideContext: IdeContext
) : UIComponentDelegatingSettingComponent<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>(
    reference,
    ideContext
) {
    override val uiComponent = DropDownComponent(
        ideContext = ideContext,
        initialValues = setting.type.values,
        validator = setting.validator,
        filter = { value ->
            setting.type.filter(ideContext, reference, value)
        },
        labelText = setting.title,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class BooleanSettingComponent(
    reference: SettingReference<Boolean, BooleanSettingType>,
    ideContext: IdeContext
) : UIComponentDelegatingSettingComponent<Boolean, BooleanSettingType>(
    reference,
    ideContext
) {
    override val uiComponent = CheckboxComponent(
        ideContext = ideContext,
        labelText = setting.title,
        initialValue = null,
        validator = setting.validator,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class StringSettingComponent(
    reference: SettingReference<String, StringSettingType>,
    ideContext: IdeContext,
    showLabel: Boolean = true
) : UIComponentDelegatingSettingComponent<String, StringSettingType>(
    reference,
    ideContext
) {
    override val uiComponent = TextFieldComponent(
        ideContext = ideContext,
        initialValue = null,
        labelText = if (showLabel) setting.title else null,
        validator = setting.validator,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class PathSettingComponent(
    reference: SettingReference<Path, PathSettingType>,
    ideContext: IdeContext
) : UIComponentDelegatingSettingComponent<Path, PathSettingType>(
    reference,
    ideContext
) {
    override val uiComponent = PathFieldComponent(
        ideContext = ideContext,
        labelText = setting.title,
        validator = settingValidator { path -> setting.validator.validate(this, path) },
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}