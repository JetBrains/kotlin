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

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.singletonOrEmptyList

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeNotLessSpecific(specific: KotlinType, general: KotlinType): Boolean
}

interface TypeSpecificityComparator {
    fun isDefinitelyLessSpecific(specific: KotlinType, general: KotlinType): Boolean

    object NONE: TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: KotlinType, general: KotlinType) = false
    }
}

class FlatSignature<out T>(
        val origin: T,
        val typeParameters: Collection<TypeParameterDescriptor>,
        val valueParameterTypes: List<KotlinType?>,
        val hasExtensionReceiver: Boolean,
        val hasVarargs: Boolean,
        val numDefaults: Int,
        val isPlatform: Boolean
) {
    val isGeneric = typeParameters.isNotEmpty()

    companion object {
        fun <D : CallableDescriptor> createFromCallableDescriptor(descriptor: D): FlatSignature<D> =
                FlatSignature(descriptor,
                              descriptor.typeParameters,
                              valueParameterTypes = descriptor.extensionReceiverTypeOrEmpty() + descriptor.valueParameters.map { it.argumentValueType },
                              hasExtensionReceiver = descriptor.extensionReceiverParameter != null,
                              hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
                              numDefaults = 0,
                              isPlatform = descriptor is MemberDescriptor && descriptor.isPlatform)

        val ValueParameterDescriptor.argumentValueType: KotlinType
            get() = varargElementType ?: type

        fun CallableDescriptor.extensionReceiverTypeOrEmpty() =
                extensionReceiverParameter?.type.singletonOrEmptyList()
    }
}


interface SimpleConstraintSystem {
    fun registerTypeVariables(typeParameters: Collection<TypeParameterDescriptor>): TypeSubstitutor
    fun addSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType)
    fun hasContradiction(): Boolean
}

fun <T> SimpleConstraintSystem.isSignatureNotLessSpecific(
        specific: FlatSignature<T>,
        general: FlatSignature<T>,
        callbacks: SpecificityComparisonCallbacks,
        specificityComparator: TypeSpecificityComparator
): Boolean {
    if (specific.hasExtensionReceiver != general.hasExtensionReceiver) return false
    if (specific.valueParameterTypes.size != general.valueParameterTypes.size) return false

    val typeParameters = general.typeParameters
    val typeSubstitutor = registerTypeVariables(typeParameters)

    for ((specificType, generalType) in specific.valueParameterTypes.zip(general.valueParameterTypes)) {
        if (specificType == null || generalType == null) continue

        if (specificityComparator.isDefinitelyLessSpecific(specificType, generalType)) {
            return false
        }

        if (typeParameters.isEmpty() || !TypeUtils.dependsOnTypeParameters(generalType, typeParameters)) {
            if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(specificType, generalType)) {
                if (!callbacks.isNonSubtypeNotLessSpecific(specificType, generalType)) {
                    return false
                }
            }
        }
        else {
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(generalType, Variance.INVARIANT)
            addSubtypeConstraint(specificType.unwrap(), substitutedGeneralType.unwrap())
        }
    }

    return !hasContradiction()
}

