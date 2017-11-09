/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.resolvedCallUtil

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.getOwnerForEffectiveDispatchReceiverParameter
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

// it returns true if call has no dispatch receiver (e.g. resulting descriptor is top-level function or local variable)
// or call receiver is effectively `this` instance (explicitly or implicitly) of resulting descriptor
// class A(other: A) {
//   val x
//   val y = other.x // return false for `other.x` as it's receiver is not `this`
// }
fun ResolvedCall<*>.hasThisOrNoDispatchReceiver(
        context: BindingContext
): Boolean {
    val dispatchReceiverValue = dispatchReceiver
    if (resultingDescriptor.dispatchReceiverParameter == null || dispatchReceiverValue == null) return true

    var dispatchReceiverDescriptor: DeclarationDescriptor? = null
    when (dispatchReceiverValue) {
        is ImplicitReceiver -> // foo() -- implicit receiver
            dispatchReceiverDescriptor = dispatchReceiverValue.declarationDescriptor
        is ClassValueReceiver -> {
            dispatchReceiverDescriptor = dispatchReceiverValue.classQualifier.descriptor
        }
        is ExpressionReceiver -> {
            val expression = KtPsiUtil.deparenthesize(dispatchReceiverValue.expression)
            if (expression is KtThisExpression) {
                // this.foo() -- explicit receiver
                dispatchReceiverDescriptor = context.get(BindingContext.REFERENCE_TARGET, expression.instanceReference)
            }
        }
    }

    return dispatchReceiverDescriptor == resultingDescriptor.getOwnerForEffectiveDispatchReceiverParameter()
}

fun ResolvedCall<*>.getExplicitReceiverValue(): ReceiverValue? {
    return when (explicitReceiverKind) {
        ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiver!!
        ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> extensionReceiver!!
        else -> null
    }
}

fun ResolvedCall<*>.getImplicitReceiverValue(): ImplicitReceiver? =
        getImplicitReceivers().firstOrNull() as? ImplicitReceiver

fun ResolvedCall<*>.getImplicitReceivers(): Collection<ReceiverValue> =
        when (explicitReceiverKind) {
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> listOfNotNull(extensionReceiver, dispatchReceiver)
            ExplicitReceiverKind.DISPATCH_RECEIVER -> listOfNotNull(extensionReceiver)
            ExplicitReceiverKind.EXTENSION_RECEIVER -> listOfNotNull(dispatchReceiver)
            ExplicitReceiverKind.BOTH_RECEIVERS -> emptyList()
        }

private fun ResolvedCall<*>.hasSafeNullableReceiver(context: CallResolutionContext<*>): Boolean {
    if (!call.isSafeCall()) return false
    val receiverValue = getExplicitReceiverValue()?.let { DataFlowValueFactory.createDataFlowValue(it, context) }
                        ?: return false
    return context.dataFlowInfo.getStableNullability(receiverValue).canBeNull()
}

fun ResolvedCall<*>.makeNullableTypeIfSafeReceiver(type: KotlinType?, context: CallResolutionContext<*>) =
        type?.let { TypeUtils.makeNullableIfNeeded(type, hasSafeNullableReceiver(context)) }

fun ResolvedCall<*>.hasBothReceivers() = dispatchReceiver != null && extensionReceiver != null

fun ResolvedCall<*>.getDispatchReceiverWithSmartCast(): ReceiverValue?
        = getReceiverValueWithSmartCast(dispatchReceiver, smartCastDispatchReceiverType)

fun KtCallElement.getArgumentByParameterIndex(index: Int, context: BindingContext): List<ValueArgument> {
    val resolvedCall = getResolvedCall(context) ?: return emptyList()
    val parameterToProcess = resolvedCall.resultingDescriptor.valueParameters.getOrNull(index) ?: return emptyList()
    return resolvedCall.valueArguments[parameterToProcess]?.arguments ?: emptyList()
}