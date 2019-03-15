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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable

class RedundantRequireNotNullCallInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(callExpression) {
        val callee = callExpression.calleeExpression ?: return
        val context = callExpression.analyze(BodyResolveMode.PARTIAL)
        if (!callExpression.isCalling(FqName("kotlin.requireNotNull"), context)
            && !callExpression.isCalling(FqName("kotlin.checkNotNull"), context)
        ) return

        val argument = callExpression.valueArguments.firstOrNull()?.getArgumentExpression()?.referenceExpression() ?: return
        val descriptor = argument.getResolvedCall(context)?.resultingDescriptor ?: return
        val type = descriptor.returnType ?: return
        if (argument.isNullable(descriptor, type, context)) return

        val functionName = callee.text
        holder.registerProblem(
            callee,
            "Redundant '$functionName' call",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            RemoveRequireNotNullCallFix(functionName)
        )
    })

    private fun KtReferenceExpression.isNullable(descriptor: CallableDescriptor, type: KotlinType, context: BindingContext): Boolean {
        if (!type.isNullable()) return false
        val dataFlowValueFactory = this.getResolutionFacade().getFrontendService(DataFlowValueFactory::class.java)
        val dataFlow = dataFlowValueFactory.createDataFlowValue(this, type, context, descriptor)
        val stableTypes = context.getDataFlowInfoBefore(this).getStableTypes(dataFlow, this.languageVersionSettings)
        return stableTypes.none { !it.isNullable() }
    }
}

private class RemoveRequireNotNullCallFix(private val functionName: String) : LocalQuickFix {
    override fun getName() = "Remove '$functionName' call"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val callExpression = descriptor.psiElement.getStrictParentOfType<KtCallExpression>() ?: return
        val qualifiedExpression = callExpression.getQualifiedExpressionForSelector()
        (qualifiedExpression ?: callExpression).delete()
    }
}