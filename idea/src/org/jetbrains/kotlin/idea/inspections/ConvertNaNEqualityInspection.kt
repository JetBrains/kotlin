/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class ConvertNaNEqualityInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return binaryExpressionVisitor { expression ->
            if (expression.left.isNaNExpression() || expression.right.isNaNExpression()) {
                val inverted = when (expression.operationToken) {
                    KtTokens.EXCLEQ -> true
                    KtTokens.EQEQ -> false
                    else -> return@binaryExpressionVisitor
                }
                holder.registerProblem(
                    expression,
                    "Equality check with NaN should be replaced with 'isNaN()'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ConvertNaNEqualityQuickFix(inverted)
                )
            }
        }
    }
}

private class ConvertNaNEqualityQuickFix(val inverted: Boolean) : LocalQuickFix {
    override fun getName() = "Replace with 'isNaN()'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? KtBinaryExpression ?: return

        val other = when {
            element.left.isNaNExpression() -> element.right ?: return
            element.right.isNaNExpression() -> element.left ?: return
            else -> return
        }
        val pattern = if (inverted) "!$0.isNaN()" else "$0.isNaN()"
        element.replace(KtPsiFactory(element).createExpressionByPattern(pattern, other))
    }
}

private val NaNSet = setOf("kotlin.Double.Companion.NaN", "java.lang.Double.NaN", "kotlin.Float.Companion.NaN", "java.lang.Float.NaN")

private fun KtExpression?.isNaNExpression(): Boolean {
    if (this?.text?.endsWith("NaN") != true) return false
    val fqName = this.resolveToCall()?.resultingDescriptor?.fqNameUnsafe?.asString()
    return NaNSet.contains(fqName)
}
