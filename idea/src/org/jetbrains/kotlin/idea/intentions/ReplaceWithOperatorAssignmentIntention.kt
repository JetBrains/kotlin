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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext

public class ReplaceWithOperatorAssignmentInspection : IntentionBasedInspection<JetBinaryExpression>(ReplaceWithOperatorAssignmentIntention())

public class ReplaceWithOperatorAssignmentIntention : JetSelfTargetingOffsetIndependentIntention<JetBinaryExpression>(javaClass(), "Replace with operator-assignment") {

    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (element.getOperationToken() != JetTokens.EQ) return false
        val left = element.getLeft() as? JetSimpleNameExpression ?: return false
        val right = element.getRight() as? JetBinaryExpression ?: return false
        if (right.getLeft() == null || right.getRight() == null) return false

        return checkExpressionRepeat(left, right)
    }

    private fun checkExpressionRepeat(variableExpression: JetSimpleNameExpression, expression: JetBinaryExpression): Boolean {
        val context = expression.analyze()
        val descriptor = context[BindingContext.REFERENCE_TARGET, expression.getOperationReference()]?.getContainingDeclaration()
        val isPrimitiveOperation = descriptor is ClassDescriptor && KotlinBuiltIns.isPrimitiveType(descriptor.getDefaultType())

        val operationToken = expression.getOperationToken()
        setText("Replace with ${expression.getOperationReference().getText()}= expression")

        return when {
            variableExpression.matches(expression.getLeft()) -> {
                operationToken == JetTokens.PLUS || operationToken == JetTokens.MINUS || operationToken == JetTokens.MUL || operationToken == JetTokens.DIV || operationToken == JetTokens.PERC
            }

            variableExpression.matches(expression.getRight()) -> {
                isPrimitiveOperation && (operationToken == JetTokens.PLUS || operationToken == JetTokens.MUL)
            }

            expression.getLeft() is JetBinaryExpression -> {
                isPrimitiveOperation && checkExpressionRepeat(variableExpression, expression.getLeft() as JetBinaryExpression)
            }

            else -> {
                false
            }
        }
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val replacement = buildOperatorAssignmentText(
                element.getLeft() as JetSimpleNameExpression,
                element.getRight() as JetBinaryExpression,
                ""
        )
        element.replace(JetPsiFactory(element).createExpression(replacement))
    }

    [tailRecursive]
    private fun buildOperatorAssignmentText(variableExpression: JetSimpleNameExpression, expression: JetBinaryExpression, tail: String): String {
        val operationText = expression.getOperationReference().getText()
        val variableName = variableExpression.getText()
        return when {
            variableExpression.matches(expression.getLeft()) -> "$variableName $operationText= ${expression.getRight()!!.getText()} $tail"

            variableExpression.matches(expression.getRight()) -> "$variableName $operationText= ${expression.getLeft()!!.getText()} $tail"

            expression.getLeft() is JetBinaryExpression ->
                buildOperatorAssignmentText(variableExpression, expression.getLeft() as JetBinaryExpression, "$operationText ${expression.getRight()!!.getText()} $tail")

            else -> tail
        }
    }
}
