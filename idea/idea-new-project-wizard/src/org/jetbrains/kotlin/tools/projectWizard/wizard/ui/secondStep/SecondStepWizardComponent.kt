package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor.ModulesEditorComponent
import java.awt.BorderLayout

class SecondStepWizardComponent(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats
) : WizardStepComponent(wizard.context) {
    private val moduleEditorComponent =
        ProjectStructureEditorComponent(wizard.context, uiEditorUsagesStats, ::onNodeSelected) {
            moduleSettingsComponent.selectSettingWithError(it)
        }.asSubComponent()
    private val moduleSettingsComponent = ModuleSettingsSubStep(wizard, uiEditorUsagesStats).asSubComponent()

    override val component = borderPanel {
        addToLeft(moduleEditorComponent.component)
        addToCenter(moduleSettingsComponent.component)
    }

    private fun onNodeSelected(data: DisplayableSettingItem?) {
        moduleSettingsComponent.selectedNode = data
    }
}


class ProjectStructureEditorComponent(
    context: Context,
    uiEditorUsagesStats: UiEditorUsageStats,
    onNodeSelected: (data: DisplayableSettingItem?) -> Unit,
    selectSettingWithError: (ValidationResult.ValidationError) -> Unit
) : DynamicComponent(context) {
    private val moduleSettingComponent = ModulesEditorComponent(
        context,
        uiEditorUsagesStats,
        needBorder = true,
        editable = true,
        oneEntrySelected = onNodeSelected,
        selectSettingWithError = selectSettingWithError
    ).asSubComponent()

    override val component = borderPanel {
        addToCenter(moduleSettingComponent.component)
    }
}


class ModuleSettingsSubStep(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats
) : SubStep(wizard.context) {
    private val moduleSettingsComponent = ModuleSettingsComponent(wizard.context, uiEditorUsagesStats).asSubComponent()
    private val nothingSelectedComponent = NothingSelectedComponent().asSubComponent()

    private val panel = panel {
        add(nothingSelectedComponent.component, BorderLayout.CENTER)
    }

    var selectedNode: DisplayableSettingItem? = null
        set(value) {
            field = value
            moduleSettingsComponent.module = value as? Module
            changeComponent()
        }

    fun selectSettingWithError(error: ValidationResult.ValidationError) {
//        moduleSettingsComponent.selectSettingWithError(error)
    }

    private fun changeComponent() {
        panel.removeAll()
        val component = when (selectedNode) {
            is Module -> moduleSettingsComponent
            else -> nothingSelectedComponent
        }
        panel.add(component.component, BorderLayout.CENTER)
        panel.updateUI()
    }

    override fun buildContent() = panel
}
