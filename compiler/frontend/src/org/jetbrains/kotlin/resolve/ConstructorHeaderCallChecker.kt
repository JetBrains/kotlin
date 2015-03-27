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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.JetConstructorDelegationCall
import org.jetbrains.kotlin.psi.JetInstanceExpressionWithLabel
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.hasImplicitThisOrSuperDispatchReceiver
import org.jetbrains.kotlin.resolve.descriptorUtil.getOwnerForEffectiveDispatchReceiverParameter


public class ConstructorHeaderCallChecker(constructor: ConstructorDescriptor, private val wrapped: CallChecker) : CallChecker {
    private val containingClass = constructor.getContainingDeclaration()

    override fun <F : CallableDescriptor?> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        if (resolvedCall.getStatus().isSuccess() &&
            resolvedCall.hasImplicitThisOrSuperDispatchReceiver(context.trace.getBindingContext()) &&
            containingClass == resolvedCall.getResultingDescriptor().getOwnerForEffectiveDispatchReceiverParameter()
        ) {
            reportError(context, resolvedCall)
        }
        else {
            val callElement = resolvedCall.getCall().getCallElement()
            if (callElement is JetInstanceExpressionWithLabel) {
                val descriptor = context.trace.get(BindingContext.REFERENCE_TARGET, callElement.getInstanceReference())
                if (containingClass == descriptor) {
                    reportError(context, resolvedCall)
                }
            }
        }

        wrapped.check(resolvedCall, context)
    }

    private fun reportError(context: BasicCallResolutionContext, resolvedCall: ResolvedCall<*>) {
        context.trace.report(
                Errors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.on(context.call.getCalleeExpression(), resolvedCall.getResultingDescriptor()))
    }
}
