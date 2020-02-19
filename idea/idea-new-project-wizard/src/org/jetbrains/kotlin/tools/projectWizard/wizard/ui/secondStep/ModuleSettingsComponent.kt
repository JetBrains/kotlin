package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.ui.components.JBTabbedPane
import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidators
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.getConfiguratorSettings
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.moduleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module.Companion.ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.TextFieldComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingsList
import java.awt.BorderLayout
import javax.swing.JComponent

class ModuleSettingsComponent(
    ideContext: IdeContext,
    uiEditorUsagesStats: UiEditorUsageStats
) : DynamicComponent(ideContext) {
    private val validateModuleName = run {
        val entityName = KotlinNewProjectWizardBundle.message("editor.entity.module.name")
        StringValidators.shouldNotBeBlank(entityName) and
                StringValidators.shouldBeValidIdentifier(entityName, ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES)
    }

    private val moduleConfiguratorSettingsList = SettingsList(emptyList(), ideContext).asSubComponent()
    private val templateComponent = TemplatesComponent(ideContext, uiEditorUsagesStats).asSubComponent()

    private val tabPanel = JBTabbedPane().apply {
        add(KotlinNewProjectWizardBundle.message("editor.tab.template"), templateComponent.component)
        add(KotlinNewProjectWizardBundle.message("editor.tab.module.settings"), moduleConfiguratorSettingsList.component)
    }

    fun selectSettingWithError(error: ValidationResult.ValidationError) {
        val componentWithError = nameField.findComponentWithError(error)
            ?: moduleConfiguratorSettingsList.findComponentWithError(error)?.also { tabPanel.selectedIndex = 1 }
            ?: templateComponent.findComponentWithError(error)?.also { tabPanel.selectedIndex = 0 }
        componentWithError?.focusOn()
    }

    private val nameField = TextFieldComponent(
        ideContext,
        labelText = KotlinNewProjectWizardBundle.message("editor.field.name"),
        onValueUpdate = { value ->
            module?.name = value
            ideContext.eventManager.fireListeners(null)
        },
        validator = validateModuleName
    ).asSubComponent()


    override val component: JComponent = panel {
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

        moduleConfiguratorSettingsList.setSettings(module.getConfiguratorSettings())
        templateComponent.module = module
    }
}

