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

import org.jetbrains.kotlin.container.PlatformExtensionsClashResolver
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.captureFromExpression

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeNotLessSpecific(specific: KotlinType, general: KotlinType): Boolean
}

interface TypeSpecificityComparator : PlatformSpecificExtension<TypeSpecificityComparator> {
    fun isDefinitelyLessSpecific(specific: KotlinType, general: KotlinType): Boolean

    object NONE : TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: KotlinType, general: KotlinType) = false
    }
}

class TypeSpecificityComparatorClashesResolver : PlatformExtensionsClashResolver.UseAnyOf<TypeSpecificityComparator>(
    TypeSpecificityComparator.NONE,
    TypeSpecificityComparator::class.java
)

class FlatSignature<out T> private constructor(
    val origin: T,
    val typeParameters: Collection<TypeParameterDescriptor>,
    val valueParameterTypes: List<KotlinType?>,
    val hasExtensionReceiver: Boolean,
    val hasVarargs: Boolean,
    val numDefaults: Int,
    val isExpect: Boolean,
    val isSyntheticMember: Boolean
) {
    val isGeneric = typeParameters.isNotEmpty()

    companion object {
        fun <T> createFromReflectionType(
            origin: T,
            descriptor: CallableDescriptor,
            numDefaults: Int,
            reflectionType: UnwrappedType
        ): FlatSignature<T> {
            return FlatSignature(
                origin,
                descriptor.typeParameters,
                reflectionType.arguments.map { it.type }, // should we drop return type?
                hasExtensionReceiver = false,
                hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
                numDefaults = numDefaults,
                isExpect = descriptor is MemberDescriptor && descriptor.isExpect,
                isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
            )
        }

        fun <T> create(
            origin: T,
            descriptor: CallableDescriptor,
            numDefaults: Int,
            parameterTypes: List<KotlinType?>
        ): FlatSignature<T> {
            val extensionReceiverType = descriptor.extensionReceiverParameter?.type

            return FlatSignature(
                origin,
                descriptor.typeParameters,
                valueParameterTypes =
                listOfNotNull(extensionReceiverType) + parameterTypes,
                hasExtensionReceiver = extensionReceiverType != null,
                hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
                numDefaults = numDefaults,
                isExpect = descriptor is MemberDescriptor && descriptor.isExpect,
                isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
            )
        }

        fun <D : CallableDescriptor> createFromCallableDescriptor(
            descriptor: D
        ): FlatSignature<D> =
            create(descriptor, descriptor, numDefaults = 0, parameterTypes = descriptor.valueParameters.map { it.argumentValueType })

        fun <D : CallableDescriptor> createForPossiblyShadowedExtension(descriptor: D): FlatSignature<D> =
            FlatSignature(
                descriptor,
                descriptor.typeParameters,
                valueParameterTypes = descriptor.valueParameters.map { it.argumentValueType },
                hasExtensionReceiver = false,
                hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
                numDefaults = descriptor.valueParameters.count { it.hasDefaultValue() },
                isExpect = descriptor is MemberDescriptor && descriptor.isExpect,
                isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
            )

        val ValueParameterDescriptor.argumentValueType get() = varargElementType ?: type
    }
}


interface SimpleConstraintSystem {
    fun registerTypeVariables(typeParameters: Collection<TypeParameterDescriptor>): TypeSubstitutor
    fun addSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType)
    fun hasContradiction(): Boolean

    // todo hack for migration
    val captureFromArgument get() = false
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
        } else {
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(generalType, Variance.INVARIANT)

            /**
             * Example:
             * fun <X> Array<out X>.sort(): Unit {}
             * fun <Y: Comparable<Y>> Array<out Y>.sort(): Unit {}
             * Here, when we try solve this CS(Y is variables) then Array<out X> <: Array<out Y> and this system impossible to solve,
             * so we capture types from receiver and value parameters.
             */
            val specificCapturedType = specificType.unwrap().let { if (captureFromArgument) captureFromExpression(it) ?: it else it }
            addSubtypeConstraint(specificCapturedType, substitutedGeneralType.unwrap())
        }
    }

    return !hasContradiction()
}

