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
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.utils.addToStdlib.singletonOrEmptyList

public class CreateCallableFromUsageFix(
        originalExpression: JetExpression,
        val callableInfos: List<CallableInfo>) : CreateFromUsageFixBase(originalExpression) {
    {
        if (callableInfos.size > 1) {
            val callableInfo = callableInfos.first()

            val receiver = callableInfo.receiverTypeInfo
            if (!callableInfos.all { it.receiverTypeInfo == receiver }) throw AssertionError("All functions must have common receiver")

            val containers = callableInfo.possibleContainers
            if (!callableInfos.all { it.possibleContainers == containers }) throw AssertionError("All functions must have common containers")
        }
    }

    override fun getText(): String {
        val renderedCallables = callableInfos.map {
            val kind = when (it.kind) {
                CallableKind.FUNCTION -> "function"
                CallableKind.PROPERTY -> "property"
            }
            "$kind '${it.name}'"
        }

        return JetBundle.message("create.0.from.usage", renderedCallables.joinToString())
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        val callableInfo = callableInfos.firstOrNull() ?: return

        val callableBuilder = CallableBuilderConfiguration(callableInfos, element as JetExpression, file!!, editor!!).createBuilder()

        fun runBuilder(placement: CallablePlacement) {
            callableBuilder.placement = placement
            CommandProcessor.getInstance().executeCommand(project, { callableBuilder.build() }, getText(), null)
        }

        val popupTitle = JetBundle.message("choose.target.class.or.trait.title")
        val receiverTypeCandidates = callableBuilder.computeTypeCandidates(callableInfo.receiverTypeInfo)
        if (receiverTypeCandidates.isNotEmpty()) {
            // TODO: Support generation of Java class members
            val containers = receiverTypeCandidates
                    .map { candidate ->
                        val descriptor = DescriptorUtils.getClassDescriptorForType(candidate.theType)
                        (DescriptorToDeclarationUtil.getDeclaration(file, descriptor) as? JetClassOrObject)?.let { candidate to it }
                    }
                    .filterNotNull()

            chooseContainerElementIfNecessary(containers, editor, popupTitle, false, { it.second }) {
                runBuilder(CallablePlacement.WithReceiver(it.first))
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

public fun CreateCallableFromUsageFix(
        originalExpression: JetExpression,
        callableInfo: CallableInfo
) : CreateCallableFromUsageFix {
    return CreateCallableFromUsageFix(originalExpression, callableInfo.singletonOrEmptyList())
}