package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.DropDownSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.applyProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.BorderLayout
import javax.swing.JComponent

class ProjectTemplateSettingComponent(
    context: Context
) : SettingComponent<ProjectTemplate, DropDownSettingType<ProjectTemplate>>(
    ProjectTemplatesPlugin::template.reference,
    context
) {
    override val validationIndicator: ValidationIndicator? get() = null
    private val templateDescriptionComponent = TemplateDescriptionComponent().asSubComponent()

    private val list = ImmutableSingleSelectableListWithIcon(
        setting.type.values,
        renderValue = { value ->
            icon = value.projectKind.icon
            append(value.title)
        },
        onValueSelected = { value = it }
    ).bordered(needTopEmptyBorder = false, needInnerEmptyBorder = false, needBottomEmptyBorder = false)


    override val component: JComponent = panel {
        add(list, BorderLayout.CENTER)
        add(
            templateDescriptionComponent.component.withBorder(JBUI.Borders.emptyTop(5)),
            BorderLayout.SOUTH
        )
    }

    private fun applySelectedTemplate() = modify {
        value?.let(::applyProjectTemplate)
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == ProjectTemplatesPlugin::template.reference) {
            applySelectedTemplate()
            value?.let(templateDescriptionComponent::setTemplate)
        }
    }

    override fun onInit() {
        super.onInit()
        if (setting.type.values.isNotEmpty()) {
            list.selectedIndex = 0
            value = setting.type.values.firstOrNull()
        }
    }
}

class TemplateDescriptionComponent : Component() {
    private val descriptionLabel = label("").apply {
        fontColor = UIUtil.FontColor.BRIGHTER
    }

    fun setTemplate(template: ProjectTemplate) {
        descriptionLabel.text = template.htmlDescription
    }

    override val component: JComponent = descriptionLabel
}