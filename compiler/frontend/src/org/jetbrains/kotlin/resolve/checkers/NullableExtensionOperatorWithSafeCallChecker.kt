/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.util.OperatorNameConventions

object NullableExtensionOperatorWithSafeCallChecker : CallChecker {
    private val RELEVANT_OPERATORS = mutableSetOf<Name>().apply {
        addAll(OperatorNameConventions.ASSIGNMENT_OPERATIONS)
        add(OperatorNameConventions.INC)
        add(OperatorNameConventions.DEC)
        add(OperatorNameConventions.GET)
        add(OperatorNameConventions.SET)
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!resolvedCall.isReallySuccess()) return
        val name = resolvedCall.resultingDescriptor.name
        if (name !in RELEVANT_OPERATORS) return

        if (!isNullableSafeCallReceiver(resolvedCall.extensionReceiver)) return

        val callElement = resolvedCall.call.callElement

        // It's an operator call, not a regular one
        if (callElement is KtCallExpression && name.identifier == (callElement.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()) return

        context.trace.report(Errors.NULLABLE_EXTENSION_OPERATOR_WITH_SAFE_CALL_RECEIVER.on(reportOn))
    }

    private fun isNullableSafeCallReceiver(receiverValue: ReceiverValue?): Boolean {
        if (receiverValue !is ExpressionReceiver) return false
        if (!receiverValue.type.isNullable() || receiverValue.type.isFlexible()) return false

        val expression = receiverValue.expression

        if (expression !is KtSafeQualifiedExpression) return false
        if (expression.parent is KtParenthesizedExpression) return false

        return true
    }
}
