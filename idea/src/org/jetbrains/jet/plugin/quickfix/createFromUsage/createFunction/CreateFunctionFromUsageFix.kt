package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import com.intellij.psi.PsiElement
import org.jetbrains.jet.plugin.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.plugin.JetBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.plugin.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.jet.plugin.refactoring.getExtractionContainers
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*

public class CreateFunctionFromUsageFix(element: PsiElement, val functionInfo: CallableInfo) : CreateFromUsageFixBase(element) {
    override fun getText(): String {
        return JetBundle.message("create.function.from.usage", functionInfo.name)
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        val functionBuilder = CallableBuilderConfiguration(functionInfo, file!!, editor!!).createBuilder()

        fun runBuilder(placement: CallablePlacement) {
            functionBuilder.placement = placement
            CommandProcessor.getInstance().executeCommand(project, { functionBuilder.build() }, getText(), null)
        }

        val popupTitle = JetBundle.message("choose.target.class.or.trait.title")
        val receiverTypeCandidates = functionBuilder.computeTypeCandidates(functionInfo.receiverTypeInfo)
        if (receiverTypeCandidates.isNotEmpty()) {
            val toPsi: (TypeCandidate) -> JetClassOrObject = {
                val descriptor = DescriptorUtils.getClassDescriptorForType(it.theType)
                DescriptorToDeclarationUtil.getDeclaration(file, descriptor) as JetClassOrObject
            }
            chooseContainerElementIfNecessary(receiverTypeCandidates, editor, popupTitle, false, toPsi) {
                runBuilder(CallablePlacement.WithReceiver(it))
            }
        }
        else {
            assert(functionInfo.receiverTypeInfo is TypeInfo.Empty, "No receiver type candidates: ${element.getText()} in ${file.getText()}")

            chooseContainerElementIfNecessary(element.getExtractionContainers(), editor, popupTitle, true, { it }) {
                val container = if (it is JetClassBody) it.getParent() as JetClassOrObject else it
                runBuilder(CallablePlacement.NoReceiver(container))
            }
        }
    }
}
