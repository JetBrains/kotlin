package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor.ModulesEditorComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingsList
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.createSettingComponent
import javax.swing.JComponent

class FirstWizardStepComponent(wizard: IdeWizard) : WizardStepComponent(wizard.context) {
    private val context = wizard.context
    private val projectSettingsComponent = ProjectSettingsComponent(context).asSubComponent()
    private val projectPreviewComponent = ProjectPreviewComponent(context).asSubComponent()

    override val component: JComponent = borderPanel {
        addToCenter(projectSettingsComponent.component)
        addToRight(projectPreviewComponent.component)
    }
}

class ProjectSettingsComponent(context: Context) : DynamicComponent(context) {
    private val projectTemplateComponent = ProjectTemplateSettingComponent(context).asSubComponent()
    private val buildSystemSetting = BuildSystemTypeSettingComponent(context).asSubComponent()

    private val nameAndLocationComponent = SettingsList(
        listOf(
            StructurePlugin::name.reference.createSettingComponent(context),
            StructurePlugin::projectPath.reference.createSettingComponent(context),
            projectTemplateComponent,
            buildSystemSetting
        ),
        context
    ).asSubComponent()

    override val component: JComponent = nameAndLocationComponent.component.apply {
        border = JBUI.Borders.empty(10)
    }
}


class ProjectPreviewComponent(context: Context) : DynamicComponent(context) {
    private val modulesEditorComponent = ModulesEditorComponent(
        context,
        null,
        needBorder = false,
        editable = false,
        oneEntrySelected = {},
        selectSettingWithError = {}
    ).asSubComponent()

    override val component: JComponent = borderPanel {
        border = JBUI.Borders.empty(10)
        addToTop(label("Preview", bold = true).addBorder(JBUI.Borders.emptyBottom(5)))
        addToCenter(modulesEditorComponent.component)
    }.addBorder(JBUI.Borders.customLine(JBColor.border(), 0, /*left*/1, 0, 0))

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == ProjectTemplatesPlugin::template.reference) {
            modulesEditorComponent.updateModel()
        }
    }
}