/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.builtins.getValueParameterTypesFromCallableReflectionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType

fun <T> FlatSignature.Companion.createFromReflectionType(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    // Reflection type for callable references with bound receiver doesn't contain receiver type
    hasBoundExtensionReceiver: Boolean,
    reflectionType: UnwrappedType
): FlatSignature<T> {
    // Note that receiver is taking over descriptor, not reflection type
    // This is correct as extension receiver can't have any defaults/varargs/coercions, so there is no need to use reflection type
    // Plus, currently, receiver for reflection type is taking from *candidate*, see buildReflectionType, this candidate can
    // have transient receiver which is not the same in its signature
    val receiver = descriptor.extensionReceiverParameter?.type
    val contextReceiversTypes = descriptor.contextReceiverParameters.mapNotNull { it.type }
    val parameters = reflectionType.getValueParameterTypesFromCallableReflectionType(
        receiver != null && !hasBoundExtensionReceiver
    ).map { it.type }

    return FlatSignature(
        origin,
        descriptor.typeParameters,
        contextReceiversTypes + listOfNotNull(receiver) + parameters,
        hasExtensionReceiver = receiver != null,
        contextReceiverCount = contextReceiversTypes.size,
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isExpect = descriptor is MemberDescriptor && descriptor.isExpect,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}

fun <T> FlatSignature.Companion.create(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    parameterTypes: List<KotlinType?>
): FlatSignature<T> {
    val extensionReceiverType = descriptor.extensionReceiverParameter?.type
    val contextReceiverTypes = descriptor.contextReceiverParameters.mapNotNull { it.type }

    return FlatSignature(
        origin,
        descriptor.typeParameters,
        valueParameterTypes = contextReceiverTypes + listOfNotNull(extensionReceiverType) + parameterTypes,
        hasExtensionReceiver = extensionReceiverType != null,
        contextReceiverCount = contextReceiverTypes.size,
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isExpect = descriptor is MemberDescriptor && descriptor.isExpect,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}

fun <D : CallableDescriptor> FlatSignature.Companion.createFromCallableDescriptor(
    descriptor: D
): FlatSignature<D> =
    create(descriptor, descriptor, numDefaults = 0, parameterTypes = descriptor.valueParameters.map { it.argumentValueType })

fun <D : CallableDescriptor> FlatSignature.Companion.createForPossiblyShadowedExtension(descriptor: D): FlatSignature<D> =
    FlatSignature(
        descriptor,
        descriptor.typeParameters,
        valueParameterTypes = descriptor.valueParameters.map { it.argumentValueType },
        hasExtensionReceiver = false,
        contextReceiverCount = 0,
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = descriptor.valueParameters.count { it.hasDefaultValue() },
        isExpect = descriptor is MemberDescriptor && descriptor.isExpect,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )

val ValueParameterDescriptor.argumentValueType get() = varargElementType ?: type
