/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ConvertFlatMapToFlattenInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        qualifiedExpressionVisitor(fun(expression) {
            val callExpression = expression.selectorExpression as? KtCallExpression ?: return
            val calleeExpression = callExpression.calleeExpression ?: return
            if (calleeExpression.text != "flatMap") return
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            if (FqName("kotlin.collections.flatMap") != callExpression.getResolvedCall(context)?.resultingDescriptor?.fqNameSafe) return

            val argument = callExpression.valueArguments.singleOrNull() ?: return
            val lambdaExpression = (argument as? KtLambdaArgument)?.getLambdaExpression()
                ?: argument.getArgumentExpression() as? KtLambdaExpression
                ?: return
            val reference = lambdaExpression.bodyExpression?.statements?.singleOrNull() as? KtNameReferenceExpression ?: return
            val lambdaParameters = lambdaExpression.valueParameters
            val lambdaParameterName = if (lambdaParameters.isNotEmpty()) lambdaParameters.singleOrNull()?.name else "it"
            if (reference.text != lambdaParameterName) return

            holder.registerProblem(
                calleeExpression,
                "flatMap call should be simplified to flatten()",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ConvertFlatMapToFlattenFix()
            )
        })
}

private class ConvertFlatMapToFlattenFix : LocalQuickFix {
    override fun getName() = "Convert flatMap to flatten"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val callExpression = descriptor.psiElement.parent as? KtCallExpression ?: return
        callExpression.replace(KtPsiFactory(callExpression).createExpression("flatten()"))
    }
}