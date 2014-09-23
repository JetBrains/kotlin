package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import com.intellij.psi.PsiElement
import org.jetbrains.jet.plugin.quickfix.createFromUsage.CreateFromUsageFixBase
import com.intellij.ide.util.PsiElementListCellRenderer
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.plugin.presentation.JetClassPresenter
import javax.swing.JList
import java.awt.Component
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.plugin.JetBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBList
import javax.swing.ListSelectionModel
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil

public class CreateFunctionFromUsageFix(element: PsiElement, val functionInfo: FunctionInfo) : CreateFromUsageFixBase(element) {
    /**
     * Represents an element in the class selection list.
     */
    private class ClassCandidate(val typeCandidate: TypeCandidate, file: JetFile) {
        val jetClass: JetClass = DescriptorToDeclarationUtil.getDeclaration(
                file, DescriptorUtils.getClassDescriptorForType(typeCandidate.theType)
        ) as JetClass
    }

    private class ClassCandidateListCellRenderer : PsiElementListCellRenderer<JetClass>() {
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

    override fun getText(): String {
        return JetBundle.message("create.function.from.usage", functionInfo.name)
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        val functionBuilder = FunctionBuilderConfiguration(functionInfo, file!!, editor!!).createBuilder()

        val ownerTypeCandidates = functionBuilder.computeTypeCandidates(functionInfo.receiverTypeInfo)
        assert(!ownerTypeCandidates.empty)

        if (ownerTypeCandidates.size == 1 || ApplicationManager.getApplication()!!.isUnitTestMode()) {
            functionBuilder.receiverTypeCandidate = ownerTypeCandidates.first!!
            functionBuilder.build()
        }
        else {
            // class selection
            val list = JBList(ownerTypeCandidates.map { ClassCandidate(it, file) })
            val renderer = ClassCandidateListCellRenderer()
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            list.setCellRenderer(renderer)
            val builder = PopupChooserBuilder(list)
            renderer.installSpeedSearch(builder)

            builder.setTitle(JetBundle.message("choose.target.class.or.trait.title"))
                    .setItemChoosenCallback {
                        val selectedCandidate = list.getSelectedValue() as ClassCandidate?
                        if (selectedCandidate != null) {
                            functionBuilder.receiverTypeCandidate = selectedCandidate.typeCandidate
                            CommandProcessor.getInstance().executeCommand(project, { functionBuilder.build() }, getText(), null)
                        }
                    }
                    .createPopup()
                    .showInBestPositionFor(editor)
        }
    }
}
