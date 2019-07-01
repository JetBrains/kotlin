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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNotNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfStatement
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceValueForCondition
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableSimpleExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils

class ElvisToIfThenIntention : SelfTargetingRangeIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "Replace elvis expression with 'if' expression"), LowPriorityAction {
    override fun applicabilityRange(element: KtBinaryExpression): TextRange? {
        return if (element.operationToken == KtTokens.ELVIS && element.left != null && element.right != null)
            element.operationReference.textRange
        else
            null
    }

    private fun KtExpression.findSafeCastReceiver(context: BindingContext): KtBinaryExpressionWithTypeRHS? {
        var current = this
        while (current is KtQualifiedExpression) {
            val resolvedCall = current.selectorExpression.getResolvedCall(context) ?: return null
            val type = resolvedCall.resultingDescriptor.returnType
            if (type != null && TypeUtils.isNullableType(type)) {
                return null
            }
            current = current.receiverExpression
        }
        current = KtPsiUtil.safeDeparenthesize(current)
        return (current as? KtBinaryExpressionWithTypeRHS)?.takeIf {
            it.operationReference.getReferencedNameElementType() === KtTokens.AS_SAFE &&
            it.right != null
        }
    }

    private fun KtExpression.buildExpressionWithReplacedReceiver(
            factory: KtPsiFactory,
            newReceiver: KtExpression,
            topLevel: Boolean = true
    ): KtExpression {
        if (this !is KtQualifiedExpression) {
            return newReceiver
        }
        return factory.buildExpression(reformat = topLevel) {
            appendExpression(receiverExpression.buildExpressionWithReplacedReceiver(factory, newReceiver, topLevel = false))
            appendFixedText(".")
            appendExpression(selectorExpression)
        }
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val left = KtPsiUtil.safeDeparenthesize(element.left!!)
        val right = KtPsiUtil.safeDeparenthesize(element.right!!)

        val leftSafeCastReceiver = left.findSafeCastReceiver(context)
        if (leftSafeCastReceiver == null) {
            val property = (KtPsiUtil.safeDeparenthesize(element).parent as? KtProperty)
            val propertyName = property?.name
            if ((right is KtReturnExpression || right is KtBreakExpression || right is KtContinueExpression || right is KtThrowExpression)
                && propertyName != null
            ) {
                val parent = property.parent
                val factory = KtPsiFactory(element)
                factory.createExpressionByPattern("if ($0 == null) $1", propertyName, right)
                parent.addAfter(factory.createExpressionByPattern("if ($0 == null) $1", propertyName, right), property)
                parent.addAfter(factory.createNewLine(), property)
                element.replace(left)
                return
            }
        }

        val (leftIsStable, ifStatement) = if (leftSafeCastReceiver != null) {
            val newReceiver = leftSafeCastReceiver.left
            val typeReference = leftSafeCastReceiver.right!!
            val factory = KtPsiFactory(element)
            newReceiver.isStableSimpleExpression(context) to element.convertToIfStatement(
                    factory.createExpressionByPattern("$0 is $1", newReceiver, typeReference),
                    left.buildExpressionWithReplacedReceiver(factory, newReceiver),
                    right
            )
        }
        else {
            left.isStableSimpleExpression(context) to element.convertToIfNotNullExpression(left, left, right)
        }

        if (!leftIsStable) {
            ifStatement.introduceValueForCondition(ifStatement.then!!, editor)
        }
    }

}
