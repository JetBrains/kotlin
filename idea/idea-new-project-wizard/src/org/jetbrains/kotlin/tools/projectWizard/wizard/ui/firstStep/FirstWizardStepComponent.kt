package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.WizardStepComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingsList
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.createSettingComponent
import javax.swing.JComponent

class FirstWizardStepComponent(wizard: IdeWizard) : WizardStepComponent(wizard.context) {
    private val context = wizard.context
    private val projectTemplateComponent = ProjectTemplateSettingComponent(context).asSubComponent()
    private val buildSystemSetting = BuildSystemTypeSettingComponent(context).asSubComponent()

    private val nameAndLocationComponent = SettingsList(
        listOf(
            StructurePlugin::name.reference.createSettingComponent(context),
            StructurePlugin::projectPath.reference.createSettingComponent(context),
            projectTemplateComponent,
            buildSystemSetting
        ),
        wizard.context
    ).asSubComponent()

    override val component: JComponent = nameAndLocationComponent.component
}
