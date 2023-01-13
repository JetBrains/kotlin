/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

/**
 * Deprecate callable references in a form of (SomeClass)::name when SomeClass has a companion
 * and `(SomeClass)` is being used just like a value reference to the companion
 */
object CompanionInParenthesesLHSCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val callableReference = resolvedCall.call.callElement.parent as? KtCallableReferenceExpression ?: return
        val parenthesizedExpression = callableReference.lhs as? KtParenthesizedExpression ?: return
        val unwrappedLhs = parenthesizedExpression.expression ?: return
        val expressionReceiver = resolvedCall.call.explicitReceiver as? ExpressionReceiver ?: return

        if (!isReferenceToShortFormCompanion(expressionReceiver, unwrappedLhs, context)) return

        context.trace.report(Errors.PARENTHESIZED_COMPANION_LHS_DEPRECATION.on(parenthesizedExpression))
    }
}

/**
 * Report warnings on top-level callable references (one that are not nested in any call) that have short-formed LHS resolved to companion
 * Because they have wrong function type shape at K1
 * (see relevant testData)
 */
object CompanionIncorrectlyUnboundedWhenUsedAsLHSCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val callableReference = resolvedCall.call.callElement.parent as? KtCallableReferenceExpression ?: return
        val classQualifier = resolvedCall.call.explicitReceiver as? ClassQualifier ?: return
        val dispatchReceiver = resolvedCall.dispatchReceiver ?: return

        if (dispatchReceiver != classQualifier.classValueReceiver) return

        // Only top-level callable references may have callPosition like this
        // References nested in calls have CallPosition.Unknown
        if (context.resolutionContext.callPosition !is CallPosition.CallableReferenceRhs) return

        val referencedClass = dispatchReceiver.type.constructor.declarationDescriptor as? ClassDescriptor ?: return
        if (!referencedClass.isCompanionObject) return

        context.trace.report(Errors.INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS.on(callableReference))
    }

}

private fun isReferenceToShortFormCompanion(
    receiver: ReceiverValue,
    lhs: PsiElement?,
    context: CallCheckerContext
): Boolean {
    val referencedClass = receiver.type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
    if (!referencedClass.isCompanionObject) return false

    // We should also consider cases like (package.MyClassWithCompanion)::foo
    val simpleReference =
        ((lhs as? KtDotQualifiedExpression)?.selectorExpression ?: lhs) as? KtReferenceExpression
            ?: return false

    return context.trace.bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, simpleReference] != null
}
