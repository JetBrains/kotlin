/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isSuperOrDelegatingConstructorCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

object SelfCallInNestedObjectConstructorChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val candidateDescriptor = resolvedCall.candidateDescriptor
        val call = resolvedCall.call

        if (candidateDescriptor !is ConstructorDescriptor || !isSuperOrDelegatingConstructorCall(call)) return
        val constructedObject = context.resolutionContext.scope.ownerDescriptor.containingDeclaration as? ClassDescriptor ?: return
        if (constructedObject.kind != ClassKind.OBJECT) return
        val containingClass = constructedObject.containingDeclaration as? ClassDescriptor ?: return
        if (candidateDescriptor.constructedClass == containingClass) {
            val reportError = context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitSelfCallsInNestedObjects)
            val visitor = Visitor(containingClass, context.trace, reportError)
            resolvedCall.call.valueArgumentList?.accept(visitor)
        }
    }

    private class Visitor(
        val containingClass: ClassDescriptor,
        val trace: BindingTrace,
        val reportError: Boolean
    ) : KtVisitorVoid() {
        override fun visitKtElement(element: KtElement) {
            element.acceptChildren(this, null)
        }

        override fun visitExpression(expression: KtExpression) {
            checkArgument(expression)
            expression.acceptChildren(this, null)
        }

        private fun checkArgument(argumentExpression: KtExpression) {
            val call = argumentExpression.getCall(trace.bindingContext) ?: return
            val resolvedCall = call.getResolvedCall(trace.bindingContext) ?: return
            checkReceiver(resolvedCall.dispatchReceiver, argumentExpression)
        }

        private fun checkReceiver(
            receiver: ReceiverValue?,
            argument: KtExpression
        ) {
            val receiverType = receiver?.type ?: return
            val receiverClass = receiverType.constructor.declarationDescriptor as? ClassDescriptor ?: return
            if (DescriptorUtils.isSubclass(receiverClass, containingClass)) {
                val factory = if (reportError) {
                    Errors.SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR
                } else {
                    Errors.SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_WARNING
                }
                trace.report(factory.on(argument))
            }
        }
    }
}
