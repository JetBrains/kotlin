package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import com.intellij.ide.util.PsiElementListCellRenderer
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.plugin.presentation.JetClassPresenter

import javax.swing.*
import java.awt.*

class ClassCandidateListCellRenderer : PsiElementListCellRenderer<JetClass>() {
    private val presenter = JetClassPresenter()

    override fun getElementText(element: JetClass): String? =
            presenter.getPresentation(element)?.getPresentableText()

    protected override fun getContainerText(element: JetClass?, name: String?): String? =
            element?.let { presenter.getPresentation(it) }?.getLocationString()

    override fun getIconFlags(): Int = 0

    public override fun getListCellRendererComponent(
            list: JList<out Any?>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component? =
            super.getListCellRendererComponent(list, (value as ClassCandidate).jetClass, index, isSelected, cellHasFocus)
}
