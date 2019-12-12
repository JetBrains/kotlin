package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.util.ui.StatusText
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import java.awt.Graphics
import javax.swing.JPanel

class NothingSelectedComponent : Component() {
    override val component: JPanel = object : JPanel() {
        override fun paint(g: Graphics?) {
            super.paint(g)
            statusText.paint(this, g)
        }
    }

    private val statusText = object : StatusText(component) {
        override fun isStatusVisible() = true

        init {
            appendText("Neither Module nor Sourceset is selected")
        }
    }
}