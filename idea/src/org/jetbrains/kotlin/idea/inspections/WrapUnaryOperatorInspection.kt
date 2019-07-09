/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
        return prefixExpressionVisitor { expression ->
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
    }

    private fun IElementType.isUnaryMinusOrPlus() = this == KtTokens.MINUS || this == KtTokens.PLUS

    private class WrapUnaryOperatorQuickfix : LocalQuickFix {
        override fun getName() = "Wrap unary operator and value with ()"

        override fun getFamilyName() = name

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