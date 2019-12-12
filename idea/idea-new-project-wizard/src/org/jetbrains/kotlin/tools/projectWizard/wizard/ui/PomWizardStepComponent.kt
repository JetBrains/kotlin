package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.StringSettingComponent
import javax.swing.Box
import javax.swing.JSeparator
import javax.swing.SwingConstants

class PomWizardStepComponent(valuesReadingContext: ValuesReadingContext) : WizardStepComponent(valuesReadingContext) {
    private val groupIdComponent = StringSettingComponent(
        StructurePlugin::groupId.reference,
        valuesReadingContext,
        showLabel = true
    ).asSubComponent()

    private val artifactIdComponent = StringSettingComponent(
        StructurePlugin::artifactId.reference,
        valuesReadingContext,
        showLabel = true
    ).asSubComponent()

    private val versionComponent = StringSettingComponent(
        StructurePlugin::version.reference,
        valuesReadingContext,
        showLabel = true
    ).asSubComponent()


    override val component = panel(VerticalLayout(0)) {
        add(Box.createVerticalStrut(10))
        add(JSeparator(SwingConstants.HORIZONTAL))
        add(Box.createVerticalStrut(10))
        add(groupIdComponent.component)
        add(artifactIdComponent.component)
        add(versionComponent.component)
    }
}