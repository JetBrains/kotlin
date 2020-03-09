package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import TemplateTag
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.applyProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import java.awt.Component as AwtComponent

class FirstWizardStepComponent(wizard: IdeWizard) : WizardStepComponent(wizard.context) {
    private val buildSystemSubStep = BuildSystemSubStep(wizard.context).asSubComponent()
    private val templatesSubStep = TemplatesSubStep(wizard.context).asSubComponent()

    override val component: JComponent = panel {
        add(templatesSubStep.component, BorderLayout.CENTER)
        add(buildSystemSubStep.component, BorderLayout.SOUTH)
    }
}

class BuildSystemSubStep(context: Context) : SubStep(context) {
    private val buildSystemSetting = BuildSystemTypeSettingComponent(context).asSubComponent()

    override fun buildContent(): JComponent = panel {
        add(buildSystemSetting.component, BorderLayout.CENTER)
    }
}

class TemplatesSubStep(context: Context) : SubStep(context) {
    private val projectTemplateSettingComponent =
        ProjectTemplateSettingComponent(context) { projectTemplate ->
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