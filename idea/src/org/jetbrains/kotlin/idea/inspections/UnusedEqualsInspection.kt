/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.conventionNameCalls.isAnyEquals
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.util.OperatorNameConventions

class UnusedEqualsInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            private fun reportIfNotUsedAsExpression(expression: KtExpression) {
                val context = expression.analyze()
                if (!expression.isUsedAsExpression(context)) {
                    holder.registerProblem(expression, KotlinBundle.message("unused.equals.expression"))
                }
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)
                if (expression.operationToken == KtTokens.EQEQ &&
                    (expression.parent is KtBlockExpression || expression.parent.parent is KtIfExpression)
                ) {
                    reportIfNotUsedAsExpression(expression)
                }
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                val calleeExpression = expression.calleeExpression as? KtSimpleNameExpression ?: return
                if (calleeExpression.getReferencedNameAsName() != OperatorNameConventions.EQUALS) return

                if (!expression.isAnyEquals()) return
                reportIfNotUsedAsExpression(expression.getQualifiedExpressionForSelectorOrThis())
            }
        }
    }

}