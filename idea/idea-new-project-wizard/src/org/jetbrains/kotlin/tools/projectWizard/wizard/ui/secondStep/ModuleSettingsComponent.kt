package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.ui.components.JBTabbedPane
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidators
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.configuratorSettings
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module.Companion.ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.UiConstants
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.TextFieldComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingsList
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent

class ModuleSettingsComponent(valuesReadingContext: ValuesReadingContext) : DynamicComponent(valuesReadingContext) {
    private val validateModuleName =
        StringValidators.shouldNotBeBlank("Module name") and
                StringValidators.shouldBeValidIdentifier("Module Name", ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES)

    private val moduleConfiguratorSettingsList = SettingsList(emptyList(), valuesReadingContext).asSubComponent()
    private val templateComponent = TemplatesComponent(valuesReadingContext).asSubComponent()

    private val tabPanel = JBTabbedPane().apply {
        add("Template", templateComponent.component)
        add("Module Settings", moduleConfiguratorSettingsList.component)
    }

    private val nameField = TextFieldComponent(
        valuesReadingContext,
        labelText = "Name",
        onValueUpdate = { value ->
            module?.name = value
            eventManager.fireListeners(null)
        },
        validator = validateModuleName
    ).asSubComponent()


    override val component: JComponent = panel {
        border = BorderFactory.createEmptyBorder(
            UiConstants.GAP_BORDER_SIZE,
            UiConstants.GAP_BORDER_SIZE,
            UiConstants.GAP_BORDER_SIZE,
            UiConstants.GAP_BORDER_SIZE
        )
        add(nameField.component, BorderLayout.NORTH)
        add(tabPanel, BorderLayout.CENTER)
    }

    var module: Module? = null
        set(value) {
            field = value
            if (value != null) {
                updateModule(value)
            }
        }

    private fun updateModule(module: Module) {
        nameField.updateUiValue(module.name)
        nameField.component.isVisible = module.kind != ModuleKind.target
                || module.configurator.moduleType != ModuleType.common

        moduleConfiguratorSettingsList.setSettings(module.configuratorSettings)
        templateComponent.module = module
    }
}

