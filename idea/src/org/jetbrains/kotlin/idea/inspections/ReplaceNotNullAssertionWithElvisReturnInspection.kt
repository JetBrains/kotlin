/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.intentions.getParentLambdaLabelName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ReplaceNotNullAssertionWithElvisReturnInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = postfixExpressionVisitor(fun(postfix) {
        if (postfix.baseExpression == null) return
        val operationReference = postfix.operationReference
        if (operationReference.getReferencedNameElementType() != KtTokens.EXCLEXCL) return

        if ((postfix.getTopmostParentOfType<KtParenthesizedExpression>() ?: postfix).parent is KtReturnExpression) return

        val parent = postfix.getParentOfTypes(true, KtLambdaExpression::class.java, KtNamedFunction::class.java)
        if (parent !is KtNamedFunction && parent !is KtLambdaExpression) return
        val context = postfix.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        val (isNullable, returnLabelName) = when (parent) {
            is KtNamedFunction -> {
                val returnType = parent.descriptor(context)?.returnType ?: return
                val isNullable = returnType.isNullable()
                if (!returnType.isUnit() && !isNullable) return
                isNullable to null
            }
            is KtLambdaExpression -> {
                val functionLiteral = parent.functionLiteral
                val returnType = functionLiteral.descriptor(context)?.returnType ?: return
                if (!returnType.isUnit()) return
                val lambdaLabelName = functionLiteral.bodyBlockExpression?.getParentLambdaLabelName() ?: return
                false to lambdaLabelName
            }
            else -> return
        }

        if (context.diagnostics.forElement(operationReference).any { it.factory == Errors.UNNECESSARY_NOT_NULL_ASSERTION }) return

        holder.registerProblem(
            postfix.operationReference,
            "Replace '!!' with '?: return'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ReplaceWithElvisReturnFix(isNullable, returnLabelName)
        )
    })

    private fun KtFunction.descriptor(context: BindingContext): FunctionDescriptor? {
        return context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? FunctionDescriptor
    }

    private class ReplaceWithElvisReturnFix(
        private val returnNull: Boolean,
        private val returnLabelName: String?
    ) : LocalQuickFix, LowPriorityAction {
        override fun getName() = "Replace with '?: return${if (returnNull) " null" else ""}'"
        override fun getFamilyName() = name
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val postfix = descriptor.psiElement.parent as? KtPostfixExpression ?: return
            val base = postfix.baseExpression ?: return
            val psiFactory = KtPsiFactory(postfix)
            postfix.replaced(
                psiFactory.createExpressionByPattern(
                    "$0 ?: return$1$2",
                    base,
                    returnLabelName?.let { "@$it" } ?: "",
                    if (returnNull) " null" else ""
                )
            )
        }
    }
}