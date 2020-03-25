package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardUIBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.PanelWithStatusText
import java.awt.BorderLayout

class NothingSelectedComponent : Component() {
    override val component = PanelWithStatusText(
        BorderLayout(),
        KotlinNewProjectWizardUIBundle.message("error.nothing.selected"),
        isStatusTextVisible = true
    )
}