package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor.ModulesEditorComponent
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent

class SecondStepWizardComponent(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats
) : WizardStepComponent(wizard.ideContext) {
    private val moduleEditorSubStep =
        ModulesEditorSubStep(wizard.ideContext, uiEditorUsagesStats, ::onNodeSelected) {
            templatesSubStep.selectSettingWithError(it)
        }.asSubComponent()
    private val templatesSubStep = ModuleSettingsSubStep(wizard, uiEditorUsagesStats).asSubComponent()

    override val component = splitterFor(
        moduleEditorSubStep.component,
        templatesSubStep.component
    )

    private fun onNodeSelected(data: DisplayableSettingItem?) {
        templatesSubStep.selectedNode = data
    }
}


class ModulesEditorSubStep(
    ideContext: IdeContext,
    uiEditorUsagesStats: UiEditorUsageStats,
    onNodeSelected: (data: DisplayableSettingItem?) -> Unit,
    selectSettingWithError: (ValidationResult.ValidationError) -> Unit
) : SubStep(ideContext) {
    private val moduleSettingComponent = ModulesEditorComponent(
        ideContext,
        uiEditorUsagesStats,
        onNodeSelected,
        selectSettingWithError
    ).asSubComponent()

    override fun buildContent(): JComponent = panel {
        bordered(needInnerEmptyBorder = false, needLineBorder = false)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, UiConstants.GAP_BORDER_SIZE),
            border
        )
        add(moduleSettingComponent.component, BorderLayout.CENTER)
    }
}


class ModuleSettingsSubStep(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats
) : SubStep(wizard.ideContext) {
    private val sourcesetSettingsComponent = SourcesetSettingsComponent(wizard.ideContext).asSubComponent()
    private val moduleSettingsComponent = ModuleSettingsComponent(wizard.ideContext, uiEditorUsagesStats).asSubComponent()
    private val nothingSelectedComponent = NothingSelectedComponent().asSubComponent()

    private val panel = panel {
        bordered()
        add(nothingSelectedComponent.component, BorderLayout.CENTER)
    }

    var selectedNode: DisplayableSettingItem? = null
        set(value) {
            field = value
            sourcesetSettingsComponent.sourceset = value as? Sourceset
            moduleSettingsComponent.module = value as? Module
            changeComponent()
        }

    fun selectSettingWithError(error: ValidationResult.ValidationError) {
        moduleSettingsComponent.selectSettingWithError(error)
    }

    private fun changeComponent() {
        panel.removeAll()
        val component = when (selectedNode) {
            is Sourceset -> sourcesetSettingsComponent
            is Module -> moduleSettingsComponent
            else -> nothingSelectedComponent
        }
        panel.add(component.component, BorderLayout.CENTER)
        panel.updateUI()
    }

    override fun buildContent() = panel
}
