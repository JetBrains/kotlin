package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import org.jetbrains.kotlin.tools.projectWizard.core.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
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

class BuildSystemSubStep(readingContext: ReadingContext) :
    SubStep(readingContext) {
    private val buildSystemSetting = BuildSystemTypeSettingComponent(readingContext).asSubComponent()

    override fun buildContent(): JComponent = panel {
        add(buildSystemSetting.component, BorderLayout.CENTER)
    }
}

class TemplatesSubStep(readingContext: ReadingContext) :
    SubStep(readingContext) {
    private val projectTemplateSettingComponent =
        ProjectTemplateSettingComponent(readingContext) { projectTemplate ->
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
        read {
            allModules().forEach { module ->
                module.apply { initDefaultValuesForSettings() }
            }
        }
    }

    private fun allModules(): List<Module> {
        val modules = mutableListOf<Module>()

        fun addModule(module: Module) {
            modules += module
            module.subModules.forEach(::addModule)
        }

        read {
            KotlinPlugin::modules.reference.notRequiredSettingValue
        }?.forEach(::addModule)

        return modules
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