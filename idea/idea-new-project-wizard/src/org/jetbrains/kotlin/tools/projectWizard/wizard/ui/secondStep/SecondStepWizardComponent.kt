package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor.ModulesEditorComponent
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent

class SecondStepWizardComponent(wizard: IdeWizard) : WizardStepComponent(wizard.valuesReadingContext) {
    private val moduleEditorSubStep =
        ModulesEditorSubStep(wizard.valuesReadingContext, ::onNodeSelected).asSubComponent()
    private val templatesSubStep = ModuleSettingsSubStep(wizard).asSubComponent()

    override val component = splitterFor(
        moduleEditorSubStep.component,
        templatesSubStep.component
    )

    private fun onNodeSelected(data: DisplayableSettingItem?) {
        templatesSubStep.selectedNode = data
    }
}


class ModulesEditorSubStep(
    valuesReadingContext: ValuesReadingContext,
    onNodeSelected: (data: DisplayableSettingItem?) -> Unit
) : SubStep(valuesReadingContext) {
    private val moduleSettingComponent = ModulesEditorComponent(
        valuesReadingContext,
        onNodeSelected
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


class ModuleSettingsSubStep(wizard: IdeWizard) : SubStep(wizard.valuesReadingContext) {
    private val sourcesetSettingsComponent = SourcesetSettingsComponent(wizard.valuesReadingContext).asSubComponent()
    private val moduleSettingsComponent = ModuleSettingsComponent(valuesReadingContext).asSubComponent()
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
