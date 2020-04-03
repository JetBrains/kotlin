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

import org.jetbrains.kotlin.builtins.getValueParameterTypesFromCallableReflectionType
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.model.*

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeNotLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean
}

@DefaultImplementation(impl = TypeSpecificityComparator.NONE::class)
interface TypeSpecificityComparator : PlatformSpecificExtension<TypeSpecificityComparator> {
    fun isDefinitelyLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean

    object NONE : TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker) = false
    }
}

class FlatSignature<out T> constructor(
    val origin: T,
    val typeParameters: Collection<TypeParameterMarker>,
    val valueParameterTypes: List<KotlinTypeMarker?>,
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
            // Reflection type for callable references with bound receiver doesn't contain receiver type
            hasBoundExtensionReceiver: Boolean,
            reflectionType: UnwrappedType
        ): FlatSignature<T> {
            // Note that receiver is taking over descriptor, not reflection type
            // This is correct as extension receiver can't have any defaults/varargs/coercions, so there is no need to use reflection type
            // Plus, currently, receiver for reflection type is taking from *candidate*, see buildReflectionType, this candidate can
            // have transient receiver which is not the same in its signature
            val receiver = descriptor.extensionReceiverParameter?.type
            val parameters = reflectionType.getValueParameterTypesFromCallableReflectionType(
                receiver != null && !hasBoundExtensionReceiver
            ).map { it.type }

            return FlatSignature(
                origin,
                descriptor.typeParameters,
                listOfNotNull(receiver) + parameters,
                hasExtensionReceiver = receiver != null,
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
    fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker
    fun addSubtypeConstraint(subType: KotlinTypeMarker, superType: KotlinTypeMarker)
    fun hasContradiction(): Boolean

    // todo hack for migration
    val captureFromArgument get() = false

    val context: TypeSystemInferenceExtensionContext
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

        if (typeParameters.isEmpty() || !generalType.dependsOnTypeParameters(context, typeParameters)) {
            if (!AbstractTypeChecker.isSubtypeOf(context, specificType, generalType)) {
                if (!callbacks.isNonSubtypeNotLessSpecific(specificType, generalType)) {
                    return false
                }
            }
        } else {
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(context, generalType)

            /**
             * Example:
             * fun <X> Array<out X>.sort(): Unit {}
             * fun <Y: Comparable<Y>> Array<out Y>.sort(): Unit {}
             * Here, when we try solve this CS(Y is variables) then Array<out X> <: Array<out Y> and this system impossible to solve,
             * so we capture types from receiver and value parameters.
             */
            val specificCapturedType =
                context.prepareType(specificType).let { if (captureFromArgument) context.captureFromExpression(it) ?: it else it }
            addSubtypeConstraint(specificCapturedType, substitutedGeneralType)
        }
    }

    return !hasContradiction()
}

