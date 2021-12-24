/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.CallableReferenceKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.LHSResult
import org.jetbrains.kotlin.resolve.calls.model.SubKotlinCallArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun ConstraintStorage.buildResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): NewTypeSubstitutor {
    return buildAbstractResultingSubstitutor(context, transformTypeVariablesToErrorTypes) as NewTypeSubstitutor
}

val CallableDescriptor.returnTypeOrNothing: UnwrappedType
    get() {
        returnType?.let { return it.unwrap() }

        return builtIns.nothingType
    }

fun TypeSubstitutor.substitute(type: UnwrappedType): UnwrappedType = safeSubstitute(type, Variance.INVARIANT).unwrap()

fun CallableDescriptor.substitute(substitutor: NewTypeSubstitutor): CallableDescriptor {
    if (substitutor.isEmpty) return this

    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: KotlinType): TypeProjection? = null
        override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) = substitutor.safeSubstitute(topLevelType.unwrap())
    }
    return substitute(TypeSubstitutor.create(wrappedSubstitution))
}

fun CallableDescriptor.substituteAndApproximateTypes(
    substitutor: NewTypeSubstitutor,
    typeApproximator: TypeApproximator?,
    positionDependentApproximation: Boolean = false
): CallableDescriptor {
    if (substitutor.isEmpty) return this

    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: KotlinType): TypeProjection? = null

        override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) =
            substitutor.safeSubstitute(topLevelType.unwrap()).let { substitutedType ->
                typeApproximator?.approximateTo(
                    substitutedType,
                    TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference,
                    position != Variance.IN_VARIANCE || !positionDependentApproximation
                ) ?: substitutedType
            }
    }

    return substitute(TypeSubstitutor.create(wrappedSubstitution)) ?: this
}

fun PostponedArgumentsAnalyzerContext.addSubsystemFromArgument(argument: KotlinCallArgument?): Boolean {
    return when (argument) {
        is SubKotlinCallArgument -> {
            addOtherSystem(argument.callResult.constraintSystem.getBuilder().currentStorage())
            true
        }

        is CallableReferenceKotlinCallArgument -> {
            addSubsystemFromArgument(argument.lhsResult.safeAs<LHSResult.Expression>()?.lshCallArgument)
        }

        else -> false
    }
}
