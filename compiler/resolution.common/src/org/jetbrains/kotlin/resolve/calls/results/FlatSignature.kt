/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.*

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeNotLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean
}

class FlatSignature<out T> constructor(
    val origin: T,
    val typeParameters: Collection<TypeParameterMarker>,
    val valueParameterTypes: List<KotlinTypeMarker?>,
    val hasExtensionReceiver: Boolean,
    val contextReceiverCount: Int,
    val hasVarargs: Boolean,
    val numDefaults: Int,
    val isExpect: Boolean,
    val isSyntheticMember: Boolean
) {
    val isGeneric = typeParameters.isNotEmpty()

    companion object
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
    if (specific.contextReceiverCount != general.contextReceiverCount) return false
    if (specific.valueParameterTypes.size != general.valueParameterTypes.size && specific.contextReceiverCount == general.contextReceiverCount) return false

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
            val specificCapturedType = AbstractTypeChecker.prepareType(context, specificType)
                .let { if (captureFromArgument) context.captureFromExpression(it) ?: it else it }
            addSubtypeConstraint(specificCapturedType, substitutedGeneralType)
        }
    }

    return !hasContradiction()
}

