/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.NewAbstractResolvedCall
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.getOwnerForEffectiveDispatchReceiverParameter
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.typeUtil.contains

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
    val receiverValue = getExplicitReceiverValue()?.let { context.dataFlowValueFactory.createDataFlowValue(it, context) }
        ?: return false
    return context.dataFlowInfo.getStableNullability(receiverValue).canBeNull()
}

fun ResolvedCall<*>.makeNullableTypeIfSafeReceiver(type: KotlinType?, context: CallResolutionContext<*>) =
    type?.let { TypeUtils.makeNullableIfNeeded(type, hasSafeNullableReceiver(context)) }

fun ResolvedCall<*>.hasBothReceivers() = dispatchReceiver != null && extensionReceiver != null

fun ResolvedCall<*>.getDispatchReceiverWithSmartCast(): ReceiverValue? =
    getReceiverValueWithSmartCast(dispatchReceiver, smartCastDispatchReceiverType)

fun KtCallElement.getArgumentByParameterIndex(index: Int, context: BindingContext): List<ValueArgument> {
    val resolvedCall = getResolvedCall(context) ?: return emptyList()
    val parameterToProcess = resolvedCall.resultingDescriptor.valueParameters.getOrNull(index) ?: return emptyList()
    return resolvedCall.valueArguments[parameterToProcess]?.arguments ?: emptyList()
}

fun CallableDescriptor.isNotSimpleCall(): Boolean =
    typeParameters.isNotEmpty() ||
            (returnType?.let { type ->
                type.contains {
                    it is NewCapturedType ||
                            it.constructor is IntegerLiteralTypeConstructor ||
                            it is DefinitelyNotNullType ||
                            it is StubTypeForBuilderInference
                }
            } ?: false)

fun ResolvedCall<*>.isNewNotCompleted(): Boolean = if (this is NewAbstractResolvedCall) !isCompleted() else false

fun ResolvedCall<*>.hasInferredReturnType(): Boolean {
    if (isNewNotCompleted()) return false

    val returnType = this.resultingDescriptor.returnType ?: return false
    return !returnType.contains { ErrorUtils.isUninferredTypeVariable(it) }
}

fun CandidateApplicability.toResolutionStatus(): ResolutionStatus = when (this) {
    CandidateApplicability.RESOLVED,
    CandidateApplicability.RESOLVED_LOW_PRIORITY,
    CandidateApplicability.RESOLVED_WITH_ERROR,
    CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY -> ResolutionStatus.SUCCESS
    CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> ResolutionStatus.RECEIVER_TYPE_ERROR
    CandidateApplicability.UNSAFE_CALL -> ResolutionStatus.UNSAFE_CALL_ERROR
    else -> ResolutionStatus.OTHER_ERROR
}