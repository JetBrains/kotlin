/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ReplaceNotNullAssertionWithElvisReturnInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = postfixExpressionVisitor(fun(postfixExpression) {
        if (postfixExpression.baseExpression == null) return
        val operationReference = postfixExpression.operationReference
        if (operationReference.getReferencedNameElementType() != KtTokens.EXCLEXCL) return

        if (postfixExpression.getStrictParentOfType<KtReturnExpression>() != null) return

        val function = postfixExpression.getStrictParentOfType<KtNamedFunction>() ?: return
        val functionReturnType = (function.descriptor as? FunctionDescriptor ?: return).returnType ?: return
        val isNullable = functionReturnType.isNullable()
        if (!isNullable && !functionReturnType.isUnit()) return

        val context = postfixExpression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        if (context.diagnostics.forElement(operationReference).any { it.factory == Errors.UNNECESSARY_NOT_NULL_ASSERTION }) return

        holder.registerProblem(
            postfixExpression.operationReference,
            "Replace '!!' with '?: return'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ReplaceWithElvisReturnFix(isNullable)
        )
    })

    private class ReplaceWithElvisReturnFix(private val returnNull: Boolean) : LocalQuickFix, LowPriorityAction {
        override fun getName() = "Replace with '?: return${if (returnNull) " null" else ""}'"
        override fun getFamilyName() = name
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val postfixExpression = descriptor.psiElement.parent as? KtPostfixExpression ?: return
            val baseExpression = postfixExpression.baseExpression ?: return
            val psiFactory = KtPsiFactory(postfixExpression)
            postfixExpression.replaced(
                psiFactory.createExpressionByPattern("$0 ?: return$1", baseExpression, if (returnNull) " null" else "")
            )
        }
    }
}