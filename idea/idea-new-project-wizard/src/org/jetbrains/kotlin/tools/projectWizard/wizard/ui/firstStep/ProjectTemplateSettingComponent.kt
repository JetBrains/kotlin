package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.DropDownSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent

class ProjectTemplateSettingComponent(
    ideContext: IdeContext,
    private val onSelected: (ProjectTemplate) -> Unit
) : SettingComponent<ProjectTemplate, DropDownSettingType<ProjectTemplate>>(
    ProjectTemplatesPlugin::template.reference,
    ideContext
) {
    override val validationIndicator: ValidationIndicator? get() = null

    private val list = ImmutableSingleSelectableListWithIcon(
        setting.type.values,
        renderValue = { value ->
            icon = value.projectKind.icon
            append(value.title)
        },
        onValueSelected = {
            onValueSelected(it!!)
        }
    ).apply {
        border = BorderFactory.createEmptyBorder(
            UiConstants.GAP_BORDER_SIZE,
            UiConstants.GAP_BORDER_SIZE,
            UiConstants.GAP_BORDER_SIZE,
            UiConstants.GAP_BORDER_SIZE
        )
    }

    private fun onValueSelected(selected: ProjectTemplate) {
        value = selected
        onSelected(selected)
    }

    override val component: JComponent = panel {
        bordered(needTopEmptyBorder = false, needInnerEmptyBorder = false)
        add(list, BorderLayout.CENTER)
    }

    override fun onInit() {
        super.onInit()
        if (setting.type.values.isNotEmpty()) {
            list.selectedIndex = 0
            onValueSelected(setting.type.values.first())
        }
    }
}