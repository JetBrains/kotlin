package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.PanelWithStatusText
import java.awt.BorderLayout

class NothingSelectedComponent : Component() {
    override val component = PanelWithStatusText(
        BorderLayout(),
        KotlinNewProjectWizardBundle.message("error.nothing.selected"),
        isStatusTextVisible = true
    )
}