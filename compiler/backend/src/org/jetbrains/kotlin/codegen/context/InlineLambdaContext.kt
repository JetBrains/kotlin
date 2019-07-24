/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.context

import org.jetbrains.kotlin.backend.common.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.binding.MutableClosure
import org.jetbrains.kotlin.config.coroutinesPackageFqName
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.isTopLevelInPackage
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.source.getPsi

class InlineLambdaContext(
        functionDescriptor: FunctionDescriptor,
        contextKind: OwnerKind,
        parentContext: CodegenContext<*>,
        closure: MutableClosure?,
        val isCrossInline: Boolean,
        private val isPropertyReference: Boolean
) : MethodContext(functionDescriptor, contextKind, parentContext, closure, false) {

    override fun getFirstCrossInlineOrNonInlineContext(): CodegenContext<*> {
        if (isCrossInline && !isSuspendIntrinsicParameter()) return this

        val parent = if (isPropertyReference) parentContext as? AnonymousClassContext else  { parentContext as? ClosureContext } ?:
                     throw AssertionError(
                             "Parent of inlining lambda body should be " +
                             "${if (isPropertyReference) "ClosureContext" else "AnonymousClassContext"}, but: $parentContext"
                     )

        val grandParent = parent.parentContext ?:
                          throw AssertionError("Parent context of lambda class context should exist: $contextDescriptor")
        return grandParent.firstCrossInlineOrNonInlineContext
    }

    // suspendCoroutine and suspendCoroutineUninterceptedOrReturn accept crossinline parameter, but it is effectively inline
    private fun isSuspendIntrinsicParameter(): Boolean {
        if (contextDescriptor !is AnonymousFunctionDescriptor) return false
        val resolvedCall = (contextDescriptor.source.getPsi() as? KtElement).getParentResolvedCall(state.bindingContext) ?: return false
        val descriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return false
        return descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(state.languageVersionSettings)
                || descriptor.isTopLevelInPackage("suspendCoroutine", state.languageVersionSettings.coroutinesPackageFqName().asString())
    }
}