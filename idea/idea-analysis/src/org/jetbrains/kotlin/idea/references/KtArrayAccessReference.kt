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

import com.google.common.collect.Lists
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.canMoveLambdaOutsideParentheses
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.INDEXED_LVALUE_GET
import org.jetbrains.kotlin.resolve.BindingContext.INDEXED_LVALUE_SET
import org.jetbrains.kotlin.util.OperatorNameConventions

class KtArrayAccessReference(
    expression: KtArrayAccessExpression
) : KtSimpleReference<KtArrayAccessExpression>(expression), MultiRangeReference {
    override val resolvesByNames: Collection<Name>
        get() = NAMES

    override fun getRangeInElement() = element.textRange.shiftRight(-element.textOffset)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val getFunctionDescriptor = context[INDEXED_LVALUE_GET, expression]?.candidateDescriptor
        val setFunctionDescriptor = context[INDEXED_LVALUE_SET, expression]?.candidateDescriptor
        return listOfNotNull(getFunctionDescriptor, setFunctionDescriptor)
    }

    private fun getBracketRange(bracketToken: KtToken) =
        expression.indicesNode.node.findChildByType(bracketToken)?.textRange?.shiftRight(-expression.textOffset)

    override fun getRanges() = listOfNotNull(getBracketRange(KtTokens.LBRACKET), getBracketRange(KtTokens.RBRACKET))

    override fun canRename() = true

    override fun handleElementRename(newElementName: String): PsiElement? {
        val arrayAccessExpression = expression
        if (OperatorNameConventions.INVOKE.asString() == newElementName) {
            val replacement = KtPsiFactory(arrayAccessExpression.project).buildExpression {
                val arrayExpression = arrayAccessExpression.arrayExpression
                if (arrayExpression is KtQualifiedExpression) {
                    appendExpression(arrayExpression.receiverExpression)
                    appendFixedText(arrayExpression.operationSign.value)
                    appendExpression(arrayExpression.selectorExpression)
                }
                else {
                    appendExpression(arrayExpression)
                }

                appendFixedText("(")
                appendExpressions(arrayAccessExpression.indexExpressions, ",")
                appendFixedText(")")
            }
            val fullCallExpression = arrayAccessExpression.replaced(replacement)
            val callExpression = fullCallExpression.getPossiblyQualifiedCallExpression()
            if (callExpression != null && callExpression.canMoveLambdaOutsideParentheses()) {
                callExpression.moveFunctionLiteralOutsideParentheses()
            }
            return fullCallExpression
        }

        return this.renameImplicitConventionalCall(newElementName)
    }

    companion object {
        private val NAMES = Lists.newArrayList(OperatorNameConventions.GET, OperatorNameConventions.SET)
    }
}
