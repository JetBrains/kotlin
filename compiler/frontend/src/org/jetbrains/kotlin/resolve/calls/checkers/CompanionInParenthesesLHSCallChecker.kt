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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

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

        val referencedClass = expressionReceiver.type.constructor.declarationDescriptor as? ClassDescriptor ?: return
        if (!referencedClass.isCompanionObject) return

        // We should also consider cases like (package.MyClassWithCompanion)::foo
        val simpleReference =
            ((unwrappedLhs as? KtDotQualifiedExpression)?.selectorExpression ?: unwrappedLhs) as? KtReferenceExpression
                ?: return

        if (context.trace.bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, simpleReference] == null) return

        context.trace.report(Errors.PARENTHESIZED_COMPANION_LHS_DEPRECATION.on(parenthesizedExpression))
    }
}
