/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.cast

class NewVariableAsFunctionResolvedCallImpl(
    override val variableCall: NewAbstractResolvedCall<VariableDescriptor>,
    override val functionCall: NewAbstractResolvedCall<FunctionDescriptor>,
) : VariableAsFunctionResolvedCall, NewAbstractResolvedCall<FunctionDescriptor>() {
    override val resolvedCallAtom = functionCall.resolvedCallAtom
    override val psiKotlinCall: PSIKotlinCall = functionCall.psiKotlinCall
    val baseCall get() = functionCall.psiKotlinCall.cast<PSIKotlinCallForInvoke>().baseCall
    override fun getStatus() = functionCall.status
    override fun getCandidateDescriptor() = functionCall.candidateDescriptor
    override fun getResultingDescriptor() = functionCall.resultingDescriptor
    override fun getExtensionReceiver() = functionCall.extensionReceiver
    override fun getDispatchReceiver() = functionCall.dispatchReceiver
    override fun getExplicitReceiverKind() = functionCall.explicitReceiverKind
    override fun getTypeArguments() = functionCall.typeArguments
    override fun getSmartCastDispatchReceiverType() = functionCall.smartCastDispatchReceiverType
    override val argumentMappingByOriginal = functionCall.argumentMappingByOriginal
    override val kotlinCall = functionCall.kotlinCall
    override val languageVersionSettings = functionCall.languageVersionSettings
    override fun containsOnlyOnlyInputTypesErrors() = functionCall.containsOnlyOnlyInputTypesErrors()
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ) = functionCall.argumentToParameterMap(resultingDescriptor, valueArguments)
    override val isCompleted: Boolean = functionCall.isCompleted
}