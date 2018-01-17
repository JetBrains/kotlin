/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalTypeOrSubtype
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.ConvertLambdaToReferenceIntention
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.lambdaExpressionVisitor
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class MoveSuspiciousCallableReferenceIntoParenthesesInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return lambdaExpressionVisitor(fun(lambdaExpression) {
            val callableReference = lambdaExpression.bodyExpression?.statements?.singleOrNull() as? KtCallableReferenceExpression
            if (callableReference != null) {
                val context = lambdaExpression.analyze()
                val parentResolvedCall = lambdaExpression.getParentResolvedCall(context)
                if (parentResolvedCall != null) {
                    val originalParameterDescriptor =
                            parentResolvedCall.getParameterForArgument(lambdaExpression.parent as? ValueArgument)?.original
                    if (originalParameterDescriptor != null) {
                        val expectedType = originalParameterDescriptor.type
                        if (expectedType.isBuiltinFunctionalType) {
                            val returnType = expectedType.getReturnTypeFromFunctionType()
                            if (returnType.isBuiltinFunctionalTypeOrSubtype) return
                        }
                    }
                }
                holder.registerProblem(
                        lambdaExpression,
                        "Suspicious callable reference as the only lambda element",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        IntentionWrapper(MoveIntoParenthesesIntention(), lambdaExpression.containingFile)
                )

            }
        })
    }

    class MoveIntoParenthesesIntention : ConvertLambdaToReferenceIntention(
            "Move suspicious callable reference into parentheses '()'"
    ) {
        override fun buildReferenceText(element: KtLambdaExpression): String? {
            val callableReferenceExpression =
                    element.bodyExpression?.statements?.singleOrNull() as? KtCallableReferenceExpression ?: return null
            val callableReference = callableReferenceExpression.callableReference
            val resolvedCall = callableReference.getResolvedCall(callableReference.analyze())
            val receiverValue = resolvedCall?.let { it.extensionReceiver ?: it.dispatchReceiver }
            return (receiverValue?.let { "${it.type}" } ?: "") + "::${callableReference.text}"
        }

        override fun isApplicableTo(element: KtLambdaExpression) = true
    }
}

