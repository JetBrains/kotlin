/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemMarker
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.*

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeEquallyOrMoreSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean
}

object OverloadabilitySpecificityCallbacks : SpecificityComparisonCallbacks {
    override fun isNonSubtypeEquallyOrMoreSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean =
        false
}

class TypeWithConversion(val resultType: KotlinTypeMarker?, val originalTypeIfWasConverted: KotlinTypeMarker? = null)

class FlatSignature<out T> constructor(
    val origin: T,
    val typeParameters: Collection<TypeParameterMarker>,
    val hasExtensionReceiver: Boolean,
    val hasVarargs: Boolean,
    val numDefaults: Int,
    val isExpect: Boolean,
    val isSyntheticMember: Boolean,
    val valueParameterTypes: List<TypeWithConversion?>,
    val contextParameterTypes: Set<KotlinTypeMarker?>,
) {
    val isGeneric = typeParameters.isNotEmpty()
    val contextReceiverCount: Int get() = contextParameterTypes.size

    constructor(
        origin: T,
        typeParameters: Collection<TypeParameterMarker>,
        valueParameterTypes: List<KotlinTypeMarker?>,
        contextParameterTypes: Set<KotlinTypeMarker?>,
        hasExtensionReceiver: Boolean,
        hasVarargs: Boolean,
        numDefaults: Int,
        isExpect: Boolean,
        isSyntheticMember: Boolean,
    ) : this(
        origin, typeParameters, hasExtensionReceiver, hasVarargs, numDefaults, isExpect,
        isSyntheticMember, valueParameterTypes.map(::TypeWithConversion), contextParameterTypes
    )

    companion object
}


interface SimpleConstraintSystem {
    fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker
    fun addSubtypeConstraint(subType: KotlinTypeMarker, superType: KotlinTypeMarker)
    fun hasContradiction(): Boolean

    // todo hack for migration
    val captureFromArgument get() = false

    val context: TypeSystemInferenceExtensionContext

    val constraintSystemMarker: ConstraintSystemMarker
}

class FlatSignatureComparisonState(
    private val cs: SimpleConstraintSystem,
    private val typeParameters: Collection<TypeParameterMarker>,
    private val typeSubstitutor: TypeSubstitutorMarker,
    private val callbacks: SpecificityComparisonCallbacks,
    private val specificityComparator: TypeSpecificityComparator,
) {
    fun isLessSpecific(specificType: KotlinTypeMarker, generalType: KotlinTypeMarker): Boolean {
        if (specificityComparator.isDefinitelyLessSpecific(specificType, generalType)) {
            return true
        } else if (typeParameters.isEmpty() || !generalType.dependsOnTypeParameters(cs.context, typeParameters)) {
            if (!AbstractTypeChecker.isSubtypeOf(cs.context, specificType, generalType)) {
                if (!callbacks.isNonSubtypeEquallyOrMoreSpecific(specificType, generalType)) {
                    return true
                }
            }
        } else {
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(cs.context, generalType)

            /**
             * Example:
             * fun <X> Array<out X>.sort(): Unit {}
             * fun <Y: Comparable<Y>> Array<out Y>.sort(): Unit {}
             * Here, when we try solve this CS(Y is variables) then Array<out X> <: Array<out Y> and this system impossible to solve,
             * so we capture types from receiver and value parameters.
             */
            val specificCapturedType = AbstractTypeChecker.prepareType(cs.context, specificType)
                .let { if (cs.captureFromArgument) cs.context.captureFromExpression(it) ?: it else it }
            cs.addSubtypeConstraint(specificCapturedType, substitutedGeneralType)
            if (cs.hasContradiction()) {
                return true
            }
        }

        return false
    }
}

private fun <T> FlatSignatureComparisonState.isValueParameterTypeEquallyOrMoreSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    typeKindSelector: (TypeWithConversion?) -> KotlinTypeMarker?,
): Boolean {
    val specificContextReceiverCount = specific.contextReceiverCount
    val generalContextReceiverCount = general.contextReceiverCount

    var specificValueParameterTypes = specific.valueParameterTypes
    var generalValueParameterTypes = general.valueParameterTypes

    if (specificContextReceiverCount != generalContextReceiverCount) {
        specificValueParameterTypes = specificValueParameterTypes.drop(specificContextReceiverCount)
        generalValueParameterTypes = generalValueParameterTypes.drop(generalContextReceiverCount)
    }

    for (index in specificValueParameterTypes.indices) {
        val specificType = typeKindSelector(specificValueParameterTypes[index]) ?: continue
        val generalType = typeKindSelector(generalValueParameterTypes[index]) ?: continue

        if (isLessSpecific(specificType, generalType)) return false
    }

    return true
}

fun <T> SimpleConstraintSystem.signatureComparisonStateIfEquallyOrMoreSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    useOriginalSamTypes: Boolean = false,
): FlatSignatureComparisonState? {
    if (specific.hasExtensionReceiver != general.hasExtensionReceiver) return null
    if (specific.contextReceiverCount > general.contextReceiverCount) return null
    if (specific.valueParameterTypes.size - specific.contextReceiverCount != general.valueParameterTypes.size - general.contextReceiverCount)
        return null

    val typeSubstitutor = registerTypeVariables(general.typeParameters)
    val state = FlatSignatureComparisonState(this, general.typeParameters, typeSubstitutor, callbacks, specificityComparator)

    if (!state.isValueParameterTypeEquallyOrMoreSpecific(specific, general) { it?.resultType }) {
        return null
    }

    if (useOriginalSamTypes && !state.isValueParameterTypeEquallyOrMoreSpecific(specific, general) { it?.originalTypeIfWasConverted }) {
        return null
    }

    return state
}

fun <T> SimpleConstraintSystem.isSignatureEquallyOrMoreSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    useOriginalSamTypes: Boolean = false
): Boolean {
    return signatureComparisonStateIfEquallyOrMoreSpecific(specific, general, callbacks, specificityComparator, useOriginalSamTypes) != null
}
