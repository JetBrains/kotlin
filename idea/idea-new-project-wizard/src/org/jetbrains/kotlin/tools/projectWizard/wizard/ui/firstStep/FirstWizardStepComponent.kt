package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import TemplateTag
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.applyProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import java.awt.Component as AwtComponent

class FirstWizardStepComponent(wizard: IdeWizard) : WizardStepComponent(wizard.ideContext) {
    private val buildSystemSubStep = BuildSystemSubStep(wizard.ideContext).asSubComponent()
    private val templatesSubStep = TemplatesSubStep(wizard.ideContext).asSubComponent()

    override val component: JComponent = panel {
        add(templatesSubStep.component, BorderLayout.CENTER)
        add(buildSystemSubStep.component, BorderLayout.SOUTH)
    }
}

class BuildSystemSubStep(ideContext: IdeContext) : SubStep(ideContext) {
    private val buildSystemSetting = BuildSystemTypeSettingComponent(ideContext).asSubComponent()

    override fun buildContent(): JComponent = panel {
        add(buildSystemSetting.component, BorderLayout.CENTER)
    }
}

class TemplatesSubStep(ideContext: IdeContext) : SubStep(ideContext) {
    private val projectTemplateSettingComponent =
        ProjectTemplateSettingComponent(ideContext) { projectTemplate ->
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
        modify {
            val selectedProjectTemplate = projectTemplateSettingComponent.value ?: return@modify
            applyProjectTemplate(selectedProjectTemplate)
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
    private val tagsPanel = panel {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        alignmentX = AwtComponent.LEFT_ALIGNMENT
        alignmentY = AwtComponent.TOP_ALIGNMENT
        border = JBUI.Borders.emptyBottom(6)
    }
    private val descriptionPanel = DescriptionPanel()

    fun setTemplate(template: ProjectTemplate) {
        addTagsToPanel(template.tags)
        descriptionPanel.updateText(template.htmlDescription)
    }

    private fun addTagsToPanel(tags: List<TemplateTag>) {
        tagsPanel.removeAll()
        for (tag in tags) {
            tagsPanel.add(TemplateTagUIComponent(tag))
            tagsPanel.add(Box.createHorizontalStrut(6))
        }
        tagsPanel.updateUI()
    }

    override val component: JComponent = panel {
        bordered()
        add(tagsPanel, BorderLayout.NORTH)
        add(descriptionPanel, BorderLayout.CENTER)
    }
}