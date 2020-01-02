package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import java.awt.BorderLayout
import javax.swing.JComponent

class FirstWizardStepComponent(wizard: IdeWizard) : WizardStepComponent(wizard.valuesReadingContext) {
    private val buildSystemSubStep = BuildSystemSubStep(wizard.valuesReadingContext).asSubComponent()
    private val templatesSubStep = TemplatesSubStep(wizard.valuesReadingContext).asSubComponent()

    override val component: JComponent = panel {
        add(templatesSubStep.component, BorderLayout.CENTER)
        add(buildSystemSubStep.component, BorderLayout.SOUTH)
    }
}

class BuildSystemSubStep(valuesReadingContext: ValuesReadingContext) :
    SubStep(valuesReadingContext) {
    private val buildSystemSetting = BuildSystemTypeSettingComponent(valuesReadingContext).asSubComponent()

    override fun buildContent(): JComponent = panel {
        bordered(needBottomEmptyBorder = false)
        add(buildSystemSetting.component, BorderLayout.CENTER)
    }
}

class TemplatesSubStep(valuesReadingContext: ValuesReadingContext) :
    SubStep(valuesReadingContext) {
    private val projectTemplateSettingComponent =
        ProjectTemplateSettingComponent(valuesReadingContext) { projectTemplate ->
            templateDescriptionComponent.setTemplate(projectTemplate)
        }.asSubComponent()

    private val templateDescriptionComponent = TemplateDescriptionComponent().asSubComponent()

    override fun buildContent(): JComponent = panel {
        add(projectTemplateSettingComponent.component, BorderLayout.CENTER)
        add(templateDescriptionComponent.component, BorderLayout.SOUTH)
    }

    override fun onInit() {
        super.onInit()
        applySelectedTemplate()
    }

    private fun applySelectedTemplate() {
        projectTemplateSettingComponent.value?.setsValues?.forEach { (setting, value) ->
            // TODO do not use settingContext directly
            context.settingContext[setting] = value
        }
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == ProjectTemplatesPlugin::template.reference) {
            applySelectedTemplate()
        }
    }
}

class TemplateDescriptionComponent : Component() {
    private val descriptionPanel = DescriptionPanel()

    fun setTemplate(template: ProjectTemplate) {
        descriptionPanel.updateText(template.htmlDescription)
    }

    override val component: JComponent = panel {
        bordered()
        add(descriptionPanel, BorderLayout.CENTER)
    }
}