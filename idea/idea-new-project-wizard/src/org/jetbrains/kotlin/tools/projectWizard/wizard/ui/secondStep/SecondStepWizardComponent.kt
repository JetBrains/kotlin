package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardUIBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor.ModulesEditorComponent
import java.awt.BorderLayout

class SecondStepWizardComponent(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats
) : WizardStepComponent(wizard.context) {
    private val moduleEditorComponent =
        ProjectStructureEditorComponent(wizard.context, uiEditorUsagesStats, ::onNodeSelected).asSubComponent()
    private val moduleSettingsComponent = ModuleSettingsSubStep(wizard, uiEditorUsagesStats).asSubComponent()

    override val component = SmartTwoComponentPanel(
        moduleSettingsComponent.component,
        moduleEditorComponent.component,
        sideIsOnTheRight = false
    )

    override fun navigateTo(error: ValidationResult.ValidationError) {
        moduleEditorComponent.navigateTo(error)
        moduleSettingsComponent.navigateTo(error)
    }

    private fun onNodeSelected(data: DisplayableSettingItem?) {
        moduleSettingsComponent.selectedNode = data
    }
}


class ProjectStructureEditorComponent(
    context: Context,
    uiEditorUsagesStats: UiEditorUsageStats,
    onNodeSelected: (data: DisplayableSettingItem?) -> Unit
) : DynamicComponent(context) {
    private val moduleSettingComponent = ModulesEditorComponent(
        context,
        uiEditorUsagesStats,
        needBorder = true,
        editable = true,
        oneEntrySelected = onNodeSelected
    ).asSubComponent()

    override val component = borderPanel {
        addToCenter(moduleSettingComponent.component.addBorder(JBUI.Borders.empty(UIConstants.PADDING)))
    }
}


class ModuleSettingsSubStep(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats
) : SubStep(wizard.context) {
    private val moduleSettingsComponent =
        ModuleSettingsComponent(wizard.context, uiEditorUsagesStats).asSubComponent()
    private val nothingSelected = PanelWithStatusText(
        BorderLayout(),
        KotlinNewProjectWizardUIBundle.message("error.nothing.selected"),
        isStatusTextVisible = true
    )

    private val panel = customPanel {
        add(nothingSelected, BorderLayout.CENTER)
    }

    var selectedNode: DisplayableSettingItem? = null
        set(value) {
            field = value
            moduleSettingsComponent.module = value as? Module
            changeComponent()
        }

    private fun changeComponent() {
        panel.removeAll()
        val component = when (selectedNode) {
            is Module -> moduleSettingsComponent.component
            else -> nothingSelected
        }
        panel.add(component, BorderLayout.CENTER)
        panel.updateUI()
    }

    override fun buildContent() = panel
}
