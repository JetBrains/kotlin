/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class SetterBackingFieldAssignmentInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return propertyAccessorVisitor(fun(accessor) {
            if (!accessor.isSetter) return
            val bodyExpression = accessor.bodyBlockExpression ?: return

            val property = accessor.property
            val propertyContext = property.analyze()
            val propertyDescriptor = propertyContext[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? PropertyDescriptor ?: return
            if (propertyContext[BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor] == false) return

            val accessorContext = accessor.analyze()
            val parameter = accessor.valueParameters.singleOrNull()
            val parameterDescriptor = accessorContext[BindingContext.VALUE_PARAMETER, parameter] as? ValueParameterDescriptor
            if (bodyExpression.anyDescendantOfType<KtExpression> {
                    when (it) {
                        is KtBinaryExpression ->
                            it.left.isBackingFieldReference(property) && it.operationToken in assignmentOperators
                        is KtUnaryExpression ->
                            it.baseExpression.isBackingFieldReference(property) && it.operationToken in incrementAndDecrementOperators
                        is KtCallExpression ->
                            it.valueArguments.any { arg ->
                                arg.text == parameter?.text && run {
                                    val argumentResultingDescriptor =
                                        arg.getArgumentExpression().getResolvedCall(accessorContext)?.resultingDescriptor
                                    argumentResultingDescriptor == parameterDescriptor
                                }

                            }
                        else -> false
                    }
                }) return

            holder.registerProblem(
                accessor,
                "Existing backing field is not assigned by the setter",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                AssignBackingFieldFix()
            )
        })
    }

    private fun KtExpression?.isBackingFieldReference(property: KtProperty) = with(SuspiciousVarPropertyInspection) {
        this@isBackingFieldReference != null && isBackingFieldReference(property)
    }
}

private val assignmentOperators = listOf(KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ)

private val incrementAndDecrementOperators = listOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS)

private class AssignBackingFieldFix : LocalQuickFix {
    override fun getName() = "Assign backing field"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val setter = descriptor.psiElement as? KtPropertyAccessor ?: return
        val parameter = setter.valueParameters.firstOrNull() ?: return
        val bodyExpression = setter.bodyBlockExpression ?: return
        bodyExpression.lBrace
            ?.siblings(withItself = false)
            ?.takeWhile { it != bodyExpression.rBrace }
            ?.singleOrNull { it is PsiWhiteSpace }
            ?.also { it.delete() }
        bodyExpression.addBefore(KtPsiFactory(setter).createExpression("field = ${parameter.text}"), bodyExpression.rBrace)
    }
}