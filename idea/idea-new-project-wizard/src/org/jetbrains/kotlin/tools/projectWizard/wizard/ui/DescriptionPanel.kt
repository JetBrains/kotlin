package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.UIUtil
import java.awt.Font
import javax.swing.event.HyperlinkEvent

class DescriptionPanel(initialText: String? = null) : HtmlPanel() {
    private var bodyText: String? = initialText

    fun updateText(text: String) {
        bodyText = text.asHtml()
        update()
    }

    override fun getBody() = bodyText.orEmpty()

    override fun getBodyFont(): Font = UIUtil.getButtonFont().deriveFont(Font.PLAIN)
}