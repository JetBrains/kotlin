/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.isJs
import org.jetbrains.kotlin.types.DynamicType

class SuspiciousAsDynamicInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        callExpressionVisitor(fun(call) {
            if (!call.platform.isJs()) return
            if (call.calleeExpression?.text != "asDynamic") return
            if (call.getQualifiedExpressionForSelector()?.receiverExpression?.getCallableDescriptor()?.returnType !is DynamicType) return
            holder.registerProblem(
                call,
                "Suspicious 'asDynamic' member invocation",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                RemoveAsDynamicCallFix()
            )
        })
}

private class RemoveAsDynamicCallFix : LocalQuickFix {
    override fun getName() = "Remove 'asDynamic' invocation"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val qualifiedExpression = (descriptor.psiElement as? KtCallExpression)?.getQualifiedExpressionForSelector() ?: return
        qualifiedExpression.replace(qualifiedExpression.receiverExpression)
    }
}
