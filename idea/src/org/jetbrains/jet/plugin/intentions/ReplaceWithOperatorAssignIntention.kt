/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import org.jetbrains.jet.lang.psi.JetBinaryExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.util.JetPsiMatcher
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

public class ReplaceWithOperatorAssignIntention : JetSelfTargetingIntention<JetBinaryExpression>("replace.with.operator.assign.intention", javaClass()) {
    /*
     * setText() is a protected function, which is inaccessible from this class.
     * This override is a workaround for this issue.
     *
     * (Related to Issue KT-4617)
     */
    override fun setText(text: String) {
        super<JetSelfTargetingIntention>.setText(text)
    }

    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        fun isWellFormedAssignment(element : JetBinaryExpression): Boolean {
            val leftExpression = element.getLeft()
            val rightExpression = element.getRight()

            return leftExpression is JetSimpleNameExpression &&
                    element.getOperationToken() == JetTokens.EQ &&
                    rightExpression is JetBinaryExpression &&
                    rightExpression.getLeft() != null &&
                    rightExpression.getRight() != null
        }

        fun checkExpressionRepeat(variableExpression: JetSimpleNameExpression, expression: JetBinaryExpression): Boolean {
            val context = AnalyzerFacadeWithCache.getContextForElement(expression)
            val descriptor = context[BindingContext.REFERENCE_TARGET, expression.getOperationReference()]?.getContainingDeclaration()
            val isPrimitiveOperation = descriptor is ClassDescriptor && KotlinBuiltIns.getInstance().isPrimitiveType(descriptor.getDefaultType())

            when {
                JetPsiMatcher.checkElementMatch(variableExpression, expression.getLeft()) -> {
                    val validity = expression.getOperationToken() == JetTokens.PLUS ||
                    expression.getOperationToken() == JetTokens.MINUS ||
                    expression.getOperationToken() == JetTokens.MUL ||
                    expression.getOperationToken() == JetTokens.DIV ||
                    expression.getOperationToken() == JetTokens.PERC

                    if (validity) {
                        setText("Replace with ${expression.getOperationReference().getText()}= Expression")
                    }

                    return validity
                }
                JetPsiMatcher.checkElementMatch(variableExpression, expression.getRight()) -> {
                    val validity = (expression.getOperationToken() == JetTokens.PLUS ||
                    expression.getOperationToken() == JetTokens.MUL) &&
                    isPrimitiveOperation

                    if (validity) {
                        setText("Replace with ${expression.getOperationReference().getText()}= Expression")
                    }

                    return validity
                }
                expression.getLeft() is JetBinaryExpression -> return isPrimitiveOperation && checkExpressionRepeat(variableExpression, expression.getLeft() as JetBinaryExpression)
                else -> return false
            }
        }

        if (isWellFormedAssignment(element)) {
            return checkExpressionRepeat(element.getLeft() as JetSimpleNameExpression, element.getRight() as JetBinaryExpression)
        } else {
            return false
        }
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        [tailRecursive]
        fun buildReplacement(variableExpression: JetSimpleNameExpression, expression: JetBinaryExpression, replacementBuilder: StringBuilder): String {
            when {
                JetPsiMatcher.checkElementMatch(variableExpression, expression.getLeft()) -> {
                    return "${variableExpression.getText()} ${expression.getOperationReference().getText()}= ${expression.getRight()!!.getText()} ${replacementBuilder.toString()}"
                }
                JetPsiMatcher.checkElementMatch(variableExpression, expression.getRight()) -> {
                    return "${variableExpression.getText()} ${expression.getOperationReference().getText()}= ${expression.getLeft()!!.getText()} ${replacementBuilder.toString()}"
                }
                expression.getLeft() is JetBinaryExpression -> {
                    return buildReplacement(variableExpression, expression.getLeft() as JetBinaryExpression, StringBuilder("${expression.getOperationReference().getText()} ${expression.getRight()!!.getText()} ${replacementBuilder.toString()}"))
                }
                else -> {
                    return replacementBuilder.toString()
                }
            }
        }

        element.replace(JetPsiFactory(element).createExpression(buildReplacement(element.getLeft() as JetSimpleNameExpression, element.getRight() as JetBinaryExpression, StringBuilder())))
    }
}