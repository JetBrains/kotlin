/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isDynamic

class UnsafeCastFromDynamicInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return expressionVisitor(fun(expression) {
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, expression] ?: return
            val actualType = expression.getType(context) ?: return

            if (actualType.isDynamic() && !expectedType.isDynamic() && !TypeUtils.noExpectedType(expectedType)) {
                holder.registerProblem(
                    expression,
                    "Implicit (unsafe) cast from dynamic to $expectedType",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    CastExplicitlyFix(expectedType))
            }
        })
    }
}

private class CastExplicitlyFix(private val type: KotlinType) : LocalQuickFix {
    override fun getName() = "Cast explicitly"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? KtExpression ?: return
        val typeName = type.constructor.declarationDescriptor?.name ?: return
        val pattern = if (type.isMarkedNullable) "$0 as? $1" else "$0 as $1"
        val newExpression = KtPsiFactory(expression).createExpressionByPattern(pattern, expression, typeName)
        expression.replace(newExpression)
    }
}