package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.PanelWithStatusText
import java.awt.BorderLayout

class NothingSelectedComponent : Component() {
    override val component = PanelWithStatusText(
        BorderLayout(),
        "Neither Module nor Sourceset is selected",
        isStatusTextVisible = true
    )
}