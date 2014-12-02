/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util.extensionsUtils

import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor
import org.jetbrains.jet.lang.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl
import java.util.LinkedHashMap
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintsUtil
import org.jetbrains.jet.utils.addIfNotNull
import java.util.HashSet
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.types.TypeSubstitutor

//TODO: what if multiple receiver types match? this can result in different substitutions

public fun CallableDescriptor.substituteExtensionIfCallable(receivers: Collection<ReceiverValue>,
                                                  context: BindingContext,
                                                  dataFlowInfo: DataFlowInfo,
                                                  isInfixCall: Boolean): CallableDescriptor? {
    return receivers.stream()
            .map { substituteExtensionIfCallable(it, isInfixCall, context, dataFlowInfo) }
            .firstOrNull { it != null }
}

public fun CallableDescriptor.substituteExtensionIfCallableWithImplicitReceiver(scope: JetScope, context: BindingContext, dataFlowInfo: DataFlowInfo): CallableDescriptor?
        = substituteExtensionIfCallable(scope.getImplicitReceiversHierarchy().map { it.getValue() }, context, dataFlowInfo, false)

public fun CallableDescriptor.substituteExtensionIfCallable(
        receiver: ReceiverValue,
        isInfixCall: Boolean,
        bindingContext: BindingContext,
        dataFlowInfo: DataFlowInfo
): CallableDescriptor? {
    val receiverParameter = getExtensionReceiverParameter()!!
    if (!receiver.exists()) return null

    if (isInfixCall && (this !is SimpleFunctionDescriptor || getValueParameters().size() != 1)) {
        return null
    }

    for (type in SmartCastUtils.getSmartCastVariants(receiver, bindingContext, dataFlowInfo)) {
        val substitutor = checkReceiverResolution(type, receiverParameter, getTypeParameters())
        if (substitutor != null) {
            return substitute(substitutor)
        }
    }
    return null
}

private fun checkReceiverResolution(
        receiverType: JetType,
        receiverParameter: ReceiverParameterDescriptor,
        typeParameters: List<TypeParameterDescriptor>
): TypeSubstitutor? {
    val typeParamsInReceiver = HashSet<TypeParameterDescriptor>()
    typeParamsInReceiver.addUsedTypeParameters(receiverParameter.getType())

    val constraintSystem = ConstraintSystemImpl()
    val typeVariables = LinkedHashMap<TypeParameterDescriptor, Variance>()
    for (typeParameter in typeParameters) {
        if (typeParamsInReceiver.contains(typeParameter)) {
            typeVariables[typeParameter] = Variance.INVARIANT
        }
    }
    constraintSystem.registerTypeVariables(typeVariables)

    constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), ConstraintPosition.RECEIVER_POSITION)

    if (constraintSystem.getStatus().isSuccessful() && ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem, true)) {
        return constraintSystem.getResultingSubstitutor()
    }
    else {
        return null
    }
}

private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(jetType: JetType) {
    addIfNotNull(jetType.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor)

    for (argument in jetType.getArguments()) {
        addUsedTypeParameters(argument.getType())
    }
}

