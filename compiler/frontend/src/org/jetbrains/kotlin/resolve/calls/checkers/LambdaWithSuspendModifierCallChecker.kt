/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInfixCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.serialization.deserialization.KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object LambdaWithSuspendModifierCallChecker : CallChecker {

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor
        val call = resolvedCall.call
        val calleeName = call.referencedName()
        val variableCalleeName = resolvedCall.safeAs<VariableAsFunctionResolvedCall>()?.variableCall?.call?.referencedName()

        if (calleeName != "suspend" && variableCalleeName != "suspend" && descriptor.name.asString() != "suspend") return

        when (descriptor.fqNameOrNull()) {
            KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME -> {
                if (calleeName != "suspend" || !call.hasFormOfSuspendModifierForLambda() || call.explicitReceiver != null) {
                    context.trace.report(Errors.NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND.on(reportOn))
                }
            }
            else -> {
                if ((calleeName == "suspend" || variableCalleeName == "suspend") && call.hasFormOfSuspendModifierForLambda()) {
                    context.trace.report(Errors.MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND.on(reportOn))
                }
            }
        }
    }

    private fun Call.hasFormOfSuspendModifierForLambda() =
        !isCallableReference()
                && typeArguments.isEmpty()
                && (hasNoArgumentListButDanglingLambdas() || isInfixWithRightLambda())

    private fun Call.referencedName() =
        calleeExpression?.safeAs<KtSimpleNameExpression>()?.getReferencedName()

    private fun Call.hasNoArgumentListButDanglingLambdas() =
        valueArgumentList?.leftParenthesis == null && functionLiteralArguments.isNotEmpty()

    private fun Call.isInfixWithRightLambda() =
        isInfixCall(this)
                && callElement.safeAs<KtBinaryExpression>()?.right is KtLambdaExpression
}
