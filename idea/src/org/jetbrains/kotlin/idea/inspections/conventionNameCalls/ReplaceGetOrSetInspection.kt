/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections.conventionNameCalls

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.intentions.isReceiverExpressionWithValue
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.idea.util.calleeTextRangeInThis
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.isValidOperator

class ReplaceGetOrSetInspection : AbstractApplicabilityBasedInspection<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java
) {
    private fun FunctionDescriptor.isExplicitOperator(): Boolean {
        return if (overriddenDescriptors.isEmpty())
            containingDeclaration !is JavaClassDescriptor && isOperator
        else
            overriddenDescriptors.any { it.isExplicitOperator() }
    }

    private val operatorNames = setOf(OperatorNameConventions.GET, OperatorNameConventions.SET)

    override fun isApplicable(element: KtDotQualifiedExpression): Boolean {
        val callExpression = element.callExpression ?: return false
        val calleeName = (callExpression.calleeExpression as? KtSimpleNameExpression)?.getReferencedNameAsName()
        if (calleeName !in operatorNames) return false
        val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return false
        if (!resolvedCall.isReallySuccess()) return false

        val target = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return false
        if (!target.isValidOperator() || target.name !in operatorNames) return false

        if (callExpression.typeArgumentList != null) return false

        val arguments = callExpression.valueArguments
        if (arguments.isEmpty()) return false
        if (arguments.any { it.isNamed() }) return false

        if (!element.isReceiverExpressionWithValue()) return false

        return target.name != OperatorNameConventions.SET || !element.isUsedAsExpression(bindingContext)
    }

    override fun inspectionText(element: KtDotQualifiedExpression) = "Should be replaced with indexing"

    override fun inspectionHighlightType(element: KtDotQualifiedExpression): ProblemHighlightType {
        return if ((element.toResolvedCall(BodyResolveMode.PARTIAL)?.resultingDescriptor as? FunctionDescriptor)?.isExplicitOperator() == true) {
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        } else {
            ProblemHighlightType.INFORMATION
        }
    }

    override val defaultFixText: String
        get() = "Replace get or set call with indexing operator"

    override fun fixText(element: KtDotQualifiedExpression): String {
        val callExpression = element.callExpression ?: return defaultFixText
        val resolvedCall = callExpression.resolveToCall() ?: return defaultFixText
        return "Replace '${resolvedCall.resultingDescriptor.name.asString()}' call with indexing operator"
    }

    override fun inspectionHighlightRangeInElement(element: KtDotQualifiedExpression) = element.calleeTextRangeInThis()

    override fun applyTo(element: KtDotQualifiedExpression, project: Project, editor: Editor?) {
        val isSet = element.calleeName == OperatorNameConventions.SET.identifier
        val allArguments = element.callExpression!!.valueArguments
        assert(allArguments.isNotEmpty())

        val newExpression = KtPsiFactory(element).buildExpression {
            appendExpression(element.receiverExpression)

            appendFixedText("[")

            val arguments = if (isSet) allArguments.dropLast(1) else allArguments
            appendExpressions(arguments.map { it.getArgumentExpression() })

            appendFixedText("]")

            if (isSet) {
                appendFixedText("=")
                appendExpression(allArguments.last().getArgumentExpression())
            }
        }

        val newElement = element.replace(newExpression)

        if (editor != null) {
            moveCaret(editor, isSet, newElement)
        }
    }

    private fun moveCaret(editor: Editor, isSet: Boolean, newElement: PsiElement) {
        val arrayAccessExpression = if (isSet) {
            newElement.getChildOfType()
        } else {
            newElement as? KtArrayAccessExpression
        } ?: return

        arrayAccessExpression.leftBracket?.startOffset?.let { editor.caretModel.moveToOffset(it) }
    }
}
