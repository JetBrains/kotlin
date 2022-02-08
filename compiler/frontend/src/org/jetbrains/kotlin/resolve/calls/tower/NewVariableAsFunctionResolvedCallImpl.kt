/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallAtom
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.utils.addToStdlib.cast

class NewVariableAsFunctionResolvedCallImpl(
    override val variableCall: NewAbstractResolvedCall<VariableDescriptor>,
    override val functionCall: NewAbstractResolvedCall<FunctionDescriptor>,
) : VariableAsFunctionResolvedCall, NewAbstractResolvedCall<FunctionDescriptor>() {
    val baseCall: PSIKotlinCallImpl = functionCall.psiKotlinCall.cast<PSIKotlinCallForInvoke>().baseCall

    override val resolvedCallAtom: ResolvedCallAtom? = functionCall.resolvedCallAtom
    override val psiKotlinCall: PSIKotlinCall = functionCall.psiKotlinCall
    override val typeApproximator: TypeApproximator = functionCall.typeApproximator
    override val freshSubstitutor: FreshVariableNewTypeSubstitutor? = functionCall.freshSubstitutor
    override val argumentMappingByOriginal = functionCall.argumentMappingByOriginal
    override val kotlinCall = functionCall.kotlinCall
    override val languageVersionSettings = functionCall.languageVersionSettings

    override fun getStatus() = functionCall.status
    override fun getCandidateDescriptor() = functionCall.candidateDescriptor
    override fun getResultingDescriptor() = functionCall.resultingDescriptor
    override fun getExtensionReceiver() = functionCall.extensionReceiver
    override fun getContextReceivers() = functionCall.contextReceivers
    override fun getDispatchReceiver() = functionCall.dispatchReceiver
    override fun getExplicitReceiverKind() = functionCall.explicitReceiverKind
    override fun getTypeArguments() = functionCall.typeArguments
    override fun getSmartCastDispatchReceiverType() = functionCall.smartCastDispatchReceiverType
    override fun containsOnlyOnlyInputTypesErrors() = functionCall.containsOnlyOnlyInputTypesErrors()
    override fun updateDispatchReceiverType(newType: KotlinType) = functionCall.updateDispatchReceiverType(newType)
    override fun updateExtensionReceiverType(newType: KotlinType) = functionCall.updateExtensionReceiverType(newType)
    override fun updateContextReceiverTypes(newTypes: List<KotlinType>) = functionCall.updateContextReceiverTypes(newTypes)
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ) = functionCall.argumentToParameterMap(resultingDescriptor, valueArguments)

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        functionCall.setResultingSubstitutor(substitutor)
        variableCall.setResultingSubstitutor(substitutor)
    }
}
