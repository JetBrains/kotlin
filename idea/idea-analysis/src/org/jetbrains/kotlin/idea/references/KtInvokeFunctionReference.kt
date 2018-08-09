/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.references

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

class KtInvokeFunctionReference(expression: KtCallExpression) : KtSimpleReference<KtCallExpression>(expression), MultiRangeReference {

    override val resolvesByNames: Collection<Name>
        get() = NAMES

    override fun getRangeInElement(): TextRange {
        return element.textRange.shiftRight(-element.textOffset)
    }

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val call = element.getCall(context)
        val resolvedCall = call.getResolvedCall(context)
        return when {
            resolvedCall is VariableAsFunctionResolvedCall ->
                setOf<DeclarationDescriptor>((resolvedCall as VariableAsFunctionResolvedCall).functionCall.candidateDescriptor)
            call != null && resolvedCall != null && call.callType == Call.CallType.INVOKE ->
                setOf<DeclarationDescriptor>(resolvedCall.candidateDescriptor)
            else ->
                emptyList()
        }
    }

    override fun getRanges(): List<TextRange> {
        val list = ArrayList<TextRange>()
        val valueArgumentList = expression.valueArgumentList
        if (valueArgumentList != null) {
            if (valueArgumentList.arguments.isNotEmpty()) {
                val valueArgumentListNode = valueArgumentList.node
                val lPar = valueArgumentListNode.findChildByType(KtTokens.LPAR)
                if (lPar != null) {
                    list.add(getRange(lPar))
                }

                val rPar = valueArgumentListNode.findChildByType(KtTokens.RPAR)
                if (rPar != null) {
                    list.add(getRange(rPar))
                }
            } else {
                list.add(getRange(valueArgumentList.node))
            }
        }

        val functionLiteralArguments = expression.lambdaArguments
        for (functionLiteralArgument in functionLiteralArguments) {
            val functionLiteralExpression = functionLiteralArgument.getLambdaExpression() ?: continue
            list.add(getRange(functionLiteralExpression.leftCurlyBrace))
            val rightCurlyBrace = functionLiteralExpression.rightCurlyBrace
            if (rightCurlyBrace != null) {
                list.add(getRange(rightCurlyBrace))
            }
        }

        return list
    }

    private fun getRange(node: ASTNode): TextRange {
        val textRange = node.textRange
        return textRange.shiftRight(-expression.textOffset)
    }

    override fun canRename(): Boolean = true

    override fun handleElementRename(newElementName: String): PsiElement? {
        val callExpression = expression
        val fullCallExpression = callExpression.getQualifiedExpressionForSelectorOrThis()
        if (newElementName == OperatorNameConventions.GET.asString() && callExpression.typeArguments.isEmpty()) {
            val arrayAccessExpression = KtPsiFactory(callExpression).buildExpression {
                if (fullCallExpression is KtQualifiedExpression) {
                    appendExpression(fullCallExpression.receiverExpression)
                    appendFixedText(fullCallExpression.operationSign.value)
                }
                appendExpression(callExpression.calleeExpression)
                appendFixedText("[")
                appendExpressions(callExpression.valueArguments.map { it.getArgumentExpression() })
                appendFixedText("]")
            }
            return fullCallExpression.replace(arrayAccessExpression)
        }

        return renameImplicitConventionalCall(newElementName)
    }

    companion object {

        private val NAMES = listOf(OperatorNameConventions.INVOKE)
    }
}
