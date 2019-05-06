/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

class SimplifiableCallInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        qualifiedExpressionVisitor(fun(expression) {
            val callExpression = expression.selectorExpression as? KtCallExpression ?: return
            val calleeExpression = callExpression.calleeExpression ?: return
            val conversion = callExpression.findConversion() ?: return
            val conversionSuffix = conversion.analyzer(callExpression) ?: return

            holder.registerProblem(
                calleeExpression,
                "${conversion.fqName.shortName()} call could be simplified to ${conversion.replacement}$conversionSuffix",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                SimplifyCallFix(conversion, conversionSuffix)
            )
        })

    private fun KtCallExpression.findConversion(): Conversion? = conversions.firstOrNull { isCalling(it.fqName) }

    private data class Conversion(val callFqName: String, val replacement: String, val analyzer: (KtCallExpression) -> String?) {
        val fqName = FqName(callFqName)
    }

    companion object {
        private val conversions = listOf(
            Conversion("kotlin.collections.flatMap", "flatten", fun(callExpression: KtCallExpression): String? {
                val argument = callExpression.valueArguments.singleOrNull() ?: return null
                val lambdaExpression = (argument as? KtLambdaArgument)?.getLambdaExpression()
                    ?: argument.getArgumentExpression() as? KtLambdaExpression
                    ?: return null
                val reference = lambdaExpression.bodyExpression?.statements?.singleOrNull() as? KtNameReferenceExpression ?: return null
                val lambdaParameters = lambdaExpression.valueParameters
                val lambdaParameterName = if (lambdaParameters.isNotEmpty()) lambdaParameters.singleOrNull()?.name else "it"
                if (reference.text != lambdaParameterName) return null
                return "()"
            })
        )
    }

    private class SimplifyCallFix(val conversion: Conversion, val conversionSuffix: String) : LocalQuickFix {
        override fun getName() = "Convert '${conversion.fqName.shortName()}' call to '${conversion.replacement}$conversionSuffix'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val callExpression = descriptor.psiElement.parent as? KtCallExpression ?: return
            callExpression.replace(KtPsiFactory(callExpression).createExpression("${conversion.replacement}$conversionSuffix"))
        }
    }
}

