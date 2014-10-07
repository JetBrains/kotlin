package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

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
import org.jetbrains.jet.lang.psi.JetExpression

public class CreateCallableFromUsageFix(
        originalExpression: JetExpression,
        val callableInfo: CallableInfo) : CreateFromUsageFixBase(originalExpression) {
    override fun getText(): String {
        val key = when (callableInfo.kind) {
            CallableKind.FUNCTION -> "create.function.from.usage"
            CallableKind.PROPERTY -> "create.property.from.usage"
        }
        return JetBundle.message(key, callableInfo.name)
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        val callableBuilder = CallableBuilderConfiguration(callableInfo, element as JetExpression, file!!, editor!!).createBuilder()

        fun runBuilder(placement: CallablePlacement) {
            callableBuilder.placement = placement
            CommandProcessor.getInstance().executeCommand(project, { callableBuilder.build() }, getText(), null)
        }

        val popupTitle = JetBundle.message("choose.target.class.or.trait.title")
        val receiverTypeCandidates = callableBuilder.computeTypeCandidates(callableInfo.receiverTypeInfo)
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
            assert(callableInfo.receiverTypeInfo is TypeInfo.Empty, "No receiver type candidates: ${element.getText()} in ${file.getText()}")

            chooseContainerElementIfNecessary(callableInfo.possibleContainers, editor, popupTitle, true, { it }) {
                val container = if (it is JetClassBody) it.getParent() as JetClassOrObject else it
                runBuilder(CallablePlacement.NoReceiver(container))
            }
        }
    }
}
