/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.intentions.ConvertLambdaToReferenceIntention
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.lambdaExpressionVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.idea.references.resolveToDescriptors

class MoveSuspiciousCallableReferenceIntoParenthesesInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor =
        lambdaExpressionVisitor(fun(lambdaExpression) {
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
                            if (parentResolvedCall.call.callElement.getParentCall(context) != null) return
                        }
                    }
                }

                val quickFix = if (canMove(lambdaExpression, callableReference, context))
                    IntentionWrapper(MoveIntoParenthesesIntention(), lambdaExpression.containingFile)
                else
                    null

                holder.registerProblem(
                    lambdaExpression,
                    KotlinBundle.message("suspicious.callable.reference.as.the.only.lambda.element"),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    quickFix
                )
            }
        })

    private fun canMove(
        lambdaExpression: KtLambdaExpression,
        callableReference: KtCallableReferenceExpression,
        context: BindingContext
    ): Boolean {
        val lambdaDescriptor = context[BindingContext.FUNCTION, lambdaExpression.functionLiteral] ?: return false
        val lambdaParameter = lambdaDescriptor.extensionReceiverParameter ?: lambdaDescriptor.valueParameters.singleOrNull()

        val functionReceiver = callableReference.receiverExpression?.mainReference?.resolveToDescriptors(context)?.firstOrNull()
        if (functionReceiver == lambdaParameter) return true
        val lambdaParameterType = lambdaParameter?.type
        if (functionReceiver is VariableDescriptor && functionReceiver.type == lambdaParameterType) return true
        if (functionReceiver is ClassDescriptor && functionReceiver == lambdaParameterType?.constructor?.declarationDescriptor) return true

        if (lambdaParameterType == null) return false
        val functionDescriptor =
            callableReference.callableReference.mainReference.resolveToDescriptors(context).firstOrNull() as? FunctionDescriptor
        val functionParameterType = functionDescriptor?.valueParameters?.firstOrNull()?.type ?: return false
        return functionParameterType == lambdaParameterType
    }

    class MoveIntoParenthesesIntention : ConvertLambdaToReferenceIntention(
        KotlinBundle.lazyMessage("move.suspicious.callable.reference.into.parentheses")
    ) {
        override fun buildReferenceText(element: KtLambdaExpression): String? {
            val callableReferenceExpression =
                element.bodyExpression?.statements?.singleOrNull() as? KtCallableReferenceExpression ?: return null
            val callableReference = callableReferenceExpression.callableReference
            val receiverExpression = callableReferenceExpression.receiverExpression
            val receiver = if (receiverExpression == null) {
                ""
            } else {
                val descriptor = receiverExpression.getCallableDescriptor()
                val literal = element.functionLiteral
                if (descriptor == null ||
                    descriptor is ValueParameterDescriptor && descriptor.containingDeclaration == literal.resolveToDescriptorIfAny()
                ) {
                    callableReference.resolveToCall(BodyResolveMode.FULL)
                        ?.let { it.extensionReceiver ?: it.dispatchReceiver }
                        ?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(it.type) } ?: ""
                } else {
                    receiverExpression.text
                }
            }

            return "$receiver::${callableReference.text}"
        }

        override fun isApplicableTo(element: KtLambdaExpression) = true
    }
}

