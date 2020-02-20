package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import org.jetbrains.kotlin.idea.debugger.readAction
import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor.ModulesEditorComponent
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent

class SecondStepWizardComponent(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats
) : WizardStepComponent(wizard.valuesReadingContext) {
    private val moduleEditorSubStep =
        ModulesEditorSubStep(wizard.valuesReadingContext, uiEditorUsagesStats, ::onNodeSelected) {
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
    readingContext: ReadingContext,
    uiEditorUsagesStats: UiEditorUsageStats,
    onNodeSelected: (data: DisplayableSettingItem?) -> Unit,
    selectSettingWithError: (ValidationResult.ValidationError) -> Unit
) : SubStep(readingContext) {
    private val moduleSettingComponent = ModulesEditorComponent(
        readingContext,
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
) : SubStep(wizard.valuesReadingContext) {
    private val sourcesetSettingsComponent = SourcesetSettingsComponent(wizard.valuesReadingContext).asSubComponent()
    private val moduleSettingsComponent = ModuleSettingsComponent(wizard.valuesReadingContext, uiEditorUsagesStats).asSubComponent()
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
