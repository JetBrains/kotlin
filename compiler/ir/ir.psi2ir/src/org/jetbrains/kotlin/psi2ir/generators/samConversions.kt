/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.FunctionInterfaceAdapterDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.FunctionInterfaceAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.FunctionInterfaceConstructorDescriptor
import org.jetbrains.kotlin.resolve.sam.getFunctionTypeForAbstractMethod
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.types.*

internal fun GeneratorExtensions.SamConversion.isSamType(kotlinType: KotlinType): Boolean {
    val descriptor = kotlinType.constructor.declarationDescriptor
    return descriptor is ClassDescriptor && descriptor.isFun ||
            isPlatformSamType(kotlinType)
}

internal fun DeclarationDescriptor.isSamConstructor() = this is FunctionInterfaceConstructorDescriptor

internal fun CallableDescriptor.getOriginalForFunctionInterfaceAdapter() =
    when (this) {
        is FunctionInterfaceAdapterDescriptor<*> ->
            baseDescriptorForSynthetic
        is FunctionInterfaceAdapterExtensionFunctionDescriptor ->
            baseDescriptorForSynthetic
        else ->
            null
    }

internal fun KotlinType.getSubstitutedFunctionTypeForSamType(): KotlinType =
    when (val unwrapped = this.unwrap()) {
        is SimpleType -> unwrapped.getSubstitutedFunctionTypeForSamType()
        is FlexibleType -> KotlinTypeFactory.flexibleType(
            unwrapped.lowerBound.getSubstitutedFunctionTypeForSamType(),
            unwrapped.upperBound.getSubstitutedFunctionTypeForSamType(),
        )
    }

private fun SimpleType.getSubstitutedFunctionTypeForSamType(): SimpleType {
    val descriptor = constructor.declarationDescriptor as? ClassDescriptor
        ?: throw AssertionError("SAM should be represented by a class: $this")
    val singleAbstractMethod = getSingleAbstractMethodOrNull(descriptor)
        ?: throw AssertionError("$descriptor should have a single abstract method")
    val unsubstitutedFunctionType = getFunctionTypeForAbstractMethod(singleAbstractMethod, false).makeNullableAsSpecified(isMarkedNullable)
    val result = TypeSubstitutor.create(this).substitute(unsubstitutedFunctionType, Variance.INVARIANT)
        ?: throw AssertionError("Failed to substitute function type $unsubstitutedFunctionType corresponding to $this")
    return result as? SimpleType
        ?: throw AssertionError("SAM type substitution result is not a simple type: $this -> $result")
}
