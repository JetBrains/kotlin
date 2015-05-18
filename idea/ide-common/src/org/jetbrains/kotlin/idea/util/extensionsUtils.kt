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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

public enum class CallType {
    NORMAL,
    SAFE,

    INFIX {
        override fun canCall(descriptor: DeclarationDescriptor)
                = descriptor is SimpleFunctionDescriptor && descriptor.getValueParameters().size() == 1
    },

    UNARY {
        override fun canCall(descriptor: DeclarationDescriptor)
                = descriptor is SimpleFunctionDescriptor && descriptor.getValueParameters().size() == 0
    };

    public open fun canCall(descriptor: DeclarationDescriptor): Boolean = true
}

public fun CallableDescriptor.substituteExtensionIfCallable(
        receivers: Collection<ReceiverValue>,
        context: BindingContext,
        dataFlowInfo: DataFlowInfo,
        callType: CallType,
        containingDeclarationOrModule: DeclarationDescriptor
): Collection<CallableDescriptor> {
    val sequence = receivers.sequence().flatMap { substituteExtensionIfCallable(it, callType, context, dataFlowInfo, containingDeclarationOrModule).sequence() }
    if (getTypeParameters().isEmpty()) { // optimization for non-generic callables
        return sequence.firstOrNull()?.let { listOf(it) } ?: listOf()
    }
    else {
        return sequence.toList()
    }
}

public fun CallableDescriptor.substituteExtensionIfCallableWithImplicitReceiver(
        scope: JetScope,
        context: BindingContext,
        dataFlowInfo: DataFlowInfo
): Collection<CallableDescriptor> {
    val receiverValues = scope.getImplicitReceiversWithInstance().map { it.getValue() }
    return substituteExtensionIfCallable(receiverValues, context, dataFlowInfo, CallType.NORMAL, scope.getContainingDeclaration())
}

public fun CallableDescriptor.substituteExtensionIfCallable(
        receiver: ReceiverValue,
        callType: CallType,
        bindingContext: BindingContext,
        dataFlowInfo: DataFlowInfo,
        containingDeclarationOrModule: DeclarationDescriptor
): Collection<CallableDescriptor> {
    if (!receiver.exists()) return listOf()
    if (!callType.canCall(this)) return listOf()

    var types = SmartCastUtils.getSmartCastVariants(receiver, bindingContext, containingDeclarationOrModule, dataFlowInfo).sequence()

    if (callType == CallType.SAFE) {
        types = types.map { it.makeNotNullable() }
    }

    val extensionReceiverType = fuzzyExtensionReceiverType()!!
    val substitutors = types
            .map {
                var substitutor = extensionReceiverType.checkIsSuperTypeOf(it)
                // check if we may fail due to receiver expression being nullable
                if (substitutor == null && it.nullability() == TypeNullability.NULLABLE && extensionReceiverType.nullability() == TypeNullability.NOT_NULL) {
                    substitutor = extensionReceiverType.checkIsSuperTypeOf(it.makeNotNullable())
                }
                substitutor
            }
            .filterNotNull()
    if (getTypeParameters().isEmpty()) { // optimization for non-generic callables
        return if (substitutors.any()) listOf(this) else listOf()
    }
    else {
        return substitutors.map { substitute(it) }.toList()
    }
}

