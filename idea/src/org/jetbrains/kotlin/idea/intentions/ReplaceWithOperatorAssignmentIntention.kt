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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext

class ReplaceWithOperatorAssignmentInspection : IntentionBasedInspection<KtBinaryExpression>(ReplaceWithOperatorAssignmentIntention())

class ReplaceWithOperatorAssignmentIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "Replace with operator-assignment") {
    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        if (element.operationToken != KtTokens.EQ) return false
        val left = element.left as? KtNameReferenceExpression ?: return false
        val right = element.right as? KtBinaryExpression ?: return false
        if (right.left == null || right.right == null) return false

        return checkExpressionRepeat(left, right)
    }

    private fun checkExpressionRepeat(variableExpression: KtNameReferenceExpression, expression: KtBinaryExpression): Boolean {
        val context = expression.analyze()
        val descriptor = context[BindingContext.REFERENCE_TARGET, expression.operationReference]?.containingDeclaration
        val isPrimitiveOperation = descriptor is ClassDescriptor && KotlinBuiltIns.isPrimitiveType(descriptor.defaultType)

        val operationToken = expression.operationToken
        text = "Replace with ${expression.operationReference.text}= expression"

        val expressionLeft = expression.left
        val expressionRight = expression.right
        return when {
            variableExpression.matches(expressionLeft) -> {
                isArithmeticOperation(operationToken)
            }

            variableExpression.matches(expressionRight) -> {
                isPrimitiveOperation && isCommutative(operationToken)
            }

            expressionLeft is KtBinaryExpression -> {
                val sameCommutativeOperation = expressionLeft.operationToken == operationToken && isCommutative(operationToken)
                isPrimitiveOperation && sameCommutativeOperation && checkExpressionRepeat(variableExpression, expressionLeft)
            }

            else -> {
                false
            }
        }
    }

    private fun isCommutative(operationToken: IElementType) = operationToken == KtTokens.PLUS || operationToken == KtTokens.MUL
    private fun isArithmeticOperation(operationToken: IElementType) = operationToken == KtTokens.PLUS ||
                                                                       operationToken == KtTokens.MINUS ||
                                                                       operationToken == KtTokens.MUL ||
                                                                       operationToken == KtTokens.DIV ||
                                                                       operationToken == KtTokens.PERC

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val replacement = buildOperatorAssignmentText(
                element.left as KtNameReferenceExpression,
                element.right as KtBinaryExpression,
                ""
        )
        element.replace(KtPsiFactory(element).createExpression(replacement))
    }

    tailrec private fun buildOperatorAssignmentText(variableExpression: KtNameReferenceExpression, expression: KtBinaryExpression, tail: String): String {
        val operationText = expression.operationReference.text
        val variableName = variableExpression.text

        fun String.appendTail() = if (tail.isEmpty()) this else "$this $tail"

        return when {
            variableExpression.matches(expression.left) -> "$variableName $operationText= ${expression.right!!.text}".appendTail()

            variableExpression.matches(expression.right) -> "$variableName $operationText= ${expression.left!!.text}".appendTail()

            expression.left is KtBinaryExpression ->
                buildOperatorAssignmentText(variableExpression, expression.left as KtBinaryExpression, "$operationText ${expression.right!!.text}".appendTail())

            else -> tail
        }
    }
}
