/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtInstanceExpressionWithLabel
import org.jetbrains.kotlin.resolve.calls.checkers.SimpleCallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf

object ConstructorHeaderCallChecker : SimpleCallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        val dispatchReceiverClass = resolvedCall.dispatchReceiver.classDescriptorForImplicitReceiver
        val extensionReceiverClass = resolvedCall.extensionReceiver.classDescriptorForImplicitReceiver

        val labelReferenceClass =
                (resolvedCall.call.callElement as? KtInstanceExpressionWithLabel)?.let {
                    instanceExpressionWithLabel ->
                    context.trace.get(BindingContext.REFERENCE_TARGET, instanceExpressionWithLabel.instanceReference) as? ClassDescriptor
                }

        if (dispatchReceiverClass == null && extensionReceiverClass == null && labelReferenceClass == null) return

        if (context.scope.parentsWithSelf.any() {
            it is LexicalScope && it.kind == LexicalScopeKind.CONSTRUCTOR_HEADER
                && (it.ownerDescriptor as ConstructorDescriptor).containingDeclaration in
                    setOf(dispatchReceiverClass, extensionReceiverClass, labelReferenceClass)
        }) {
            context.trace.report(
                    Errors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.on(context.call.calleeExpression ?: return, resolvedCall.resultingDescriptor))
        }
    }
}

private val Receiver?.classDescriptorForImplicitReceiver: ClassDescriptor?
    get() = (this as? ImplicitReceiver)?.declarationDescriptor as? ClassDescriptor
