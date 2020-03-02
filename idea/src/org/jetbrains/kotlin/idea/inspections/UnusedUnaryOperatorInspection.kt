/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.prefixExpressionVisitor
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class UnusedUnaryOperatorInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = prefixExpressionVisitor(fun(prefix) {
        if (prefix.baseExpression == null) return
        val operationToken = prefix.operationToken
        if (operationToken != KtTokens.PLUS && operationToken != KtTokens.MINUS) return

        val context = prefix.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
        if (prefix.isUsedAsExpression(context)) return
        val operatorDescriptor = prefix.operationReference.getResolvedCall(context)?.resultingDescriptor as? DeclarationDescriptor ?: return
        if (!KotlinBuiltIns.isUnderKotlinPackage(operatorDescriptor)) return

        holder.registerProblem(prefix, KotlinBundle.message("unused.unary.operator"), RemoveUnaryOperatorFix())
    })

    private class RemoveUnaryOperatorFix : LocalQuickFix {
        override fun getName() = KotlinBundle.message("remove.unary.operator.fix.text")

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val prefixExpression = descriptor.psiElement as? KtPrefixExpression ?: return
            val baseExpression = prefixExpression.baseExpression ?: return
            prefixExpression.replace(baseExpression)
        }
    }
}
