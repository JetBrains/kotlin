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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.intentions.getLeftMostReceiverExpression
import org.jetbrains.kotlin.idea.intentions.replaceFirstReceiver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class WrapUnaryOperatorInspection : AbstractKotlinInspection() {

    val numberTypes = listOf(KtNodeTypes.INTEGER_CONSTANT, KtNodeTypes.FLOAT_CONSTANT)

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitPrefixExpression(expression: KtPrefixExpression) {
                super.visitPrefixExpression(expression)
                if (expression.operationToken.isUnaryMinusOrPlus()) {
                    val baseExpression = expression.baseExpression
                    if (baseExpression is KtDotQualifiedExpression) {
                        val receiverExpression = baseExpression.receiverExpression
                        if (receiverExpression is KtConstantExpression &&
                            receiverExpression.node.elementType in numberTypes) {
                            holder.registerProblem(expression,
                                                   "Wrap unary operator and value with ()",
                                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                   WrapUnaryOperatorQuickfix())
                        }
                    }
                }
            }

            private fun IElementType.isUnaryMinusOrPlus() = this == KtTokens.MINUS || this == KtTokens.PLUS
        }
    }

    private class WrapUnaryOperatorQuickfix : LocalQuickFix {
        override fun getFamilyName() = "Wrap unary operator and value with ()"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? KtPrefixExpression ?: return
            val dotQualifiedExpression = expression.baseExpression as? KtDotQualifiedExpression ?: return
            val factory = KtPsiFactory(project)
            val newReceiver = factory.createExpressionByPattern("($0$1)", expression.operationReference.text, dotQualifiedExpression.getLeftMostReceiverExpression())
            val newExpression = dotQualifiedExpression.replaceFirstReceiver(factory, newReceiver)
            expression.replace(newExpression)
        }
    }
}