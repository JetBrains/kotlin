/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor

class ReplaceToWithInfixFormInspection : AbstractKotlinInspection() {
    private val compatibleNames = setOf("to")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return dotQualifiedExpressionVisitor(fun(expression) {
            val callExpression = expression.callExpression ?: return
            if (callExpression.valueArguments.size != 1 || callExpression.typeArgumentList != null) return
            if (expression.calleeName !in compatibleNames) return

            val resolvedCall = expression.resolveToCall() ?: return
            val function = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return

            if (!function.isInfix) return

            holder.registerProblem(
                expression,
                KotlinBundle.message("replace.to.with.infix.form.quickfix.text"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ReplaceToWithInfixFormQuickfix()
            )
        })
    }
}

class ReplaceToWithInfixFormQuickfix : LocalQuickFix {
    override fun getName() = KotlinBundle.message("replace.to.with.infix.form.quickfix.text")

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtDotQualifiedExpression
        element.replace(
            KtPsiFactory(element).createExpressionByPattern(
                "$0 to $1", element.receiverExpression,
                element.callExpression?.valueArguments?.get(0)?.getArgumentExpression() ?: return
            )
        )
    }
}