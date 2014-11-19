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
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.utils.addIfNotNull
import java.util.HashSet

public fun CallableDescriptor.isExtensionCallable(receivers: Collection<ReceiverValue>,
                                                  context: BindingContext,
                                                  dataFlowInfo: DataFlowInfo,
                                                  isInfixCall: Boolean): Boolean
        = receivers.any { isExtensionCallable(it, isInfixCall, context, dataFlowInfo) }

public fun CallableDescriptor.isExtensionCallableWithImplicitReceiver(scope: JetScope, context: BindingContext, dataFlowInfo: DataFlowInfo): Boolean
        = isExtensionCallable(scope.getImplicitReceiversHierarchy().map { it.getValue() }, context, dataFlowInfo, false)

public fun CallableDescriptor.isExtensionCallable(
        receiver: ReceiverValue,
        isInfixCall: Boolean,
        bindingContext: BindingContext,
        dataFlowInfo: DataFlowInfo
): Boolean {
    if (isInfixCall && (this !is SimpleFunctionDescriptor || getValueParameters().size() != 1)) {
        return false
    }

    return SmartCastUtils.getSmartCastVariants(receiver, bindingContext, dataFlowInfo)
            .any { checkReceiverResolution(receiver, it, this) }
}

private fun checkReceiverResolution(receiverArgument: ReceiverValue, receiverType: JetType, callableDescriptor: CallableDescriptor): Boolean {
    val receiverParameter = callableDescriptor.getExtensionReceiverParameter()

    if (!receiverArgument.exists() && receiverParameter == null) {
        // Both receivers do not exist
        return true
    }

    if (!(receiverArgument.exists() && receiverParameter != null)) {
        return false
    }

    val typeNamesInReceiver = HashSet<Name>()
    typeNamesInReceiver.addUsedTypeNames(receiverParameter.getType())

    val constraintSystem = ConstraintSystemImpl()
    val typeVariables = LinkedHashMap<TypeParameterDescriptor, Variance>()
    for (typeParameterDescriptor in callableDescriptor.getTypeParameters()) {
        if (typeNamesInReceiver.contains(typeParameterDescriptor.getName())) {
            typeVariables.put(typeParameterDescriptor, Variance.INVARIANT)
        }
    }
    constraintSystem.registerTypeVariables(typeVariables)

    constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), ConstraintPosition.RECEIVER_POSITION)
    return constraintSystem.getStatus().isSuccessful() && ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem, true)
}

private fun MutableSet<Name>.addUsedTypeNames(jetType: JetType) {
    val descriptor = jetType.getConstructor().getDeclarationDescriptor()
    addIfNotNull(descriptor?.getName())

    for (argument in jetType.getArguments()) {
        addUsedTypeNames(argument.getType())
    }
}

