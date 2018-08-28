/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class SetterBackingFieldAssignmentInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return propertyAccessorVisitor(fun(accessor) {
            if (!accessor.isSetter) return
            val bodyExpression = accessor.bodyExpression as? KtBlockExpression ?: return
            val parameter = accessor.valueParameters.singleOrNull()
            val parameterDescriptor = parameter?.descriptor
            val context = accessor.analyze(BodyResolveMode.PARTIAL)
            if (bodyExpression.anyDescendantOfType<KtExpression> {
                    when (it) {
                        is KtBinaryExpression ->
                            it.left?.text == KtTokens.FIELD_KEYWORD.value && it.operationToken in assignmentOperators
                        is KtUnaryExpression ->
                            it.baseExpression?.text == KtTokens.FIELD_KEYWORD.value && it.operationToken in incrementAndDecrementOperators
                        is KtCallExpression ->
                            it.valueArguments.any { arg ->
                                arg.text == parameter?.text
                                        && arg.getArgumentExpression().getResolvedCall(context)?.resultingDescriptor == parameterDescriptor
                            }
                        else -> false
                    }
                }) return
            holder.registerProblem(
                accessor,
                "Setter backing field should be assigned",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                AssignBackingFieldFix()
            )
        })
    }
}

private val assignmentOperators = listOf(KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ)

private val incrementAndDecrementOperators = listOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS)

private class AssignBackingFieldFix : LocalQuickFix {
    override fun getName() = "Assign backing filed"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val setter = descriptor.psiElement as? KtPropertyAccessor ?: return
        val parameter = setter.valueParameters.firstOrNull() ?: return
        val bodyExpression = setter.bodyExpression as? KtBlockExpression ?: return
        setter.hasBlockBody()
        bodyExpression.addBefore(
            KtPsiFactory(setter).createExpression("field = ${parameter.text}"),
            bodyExpression.rBrace
        )
    }
}