/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.typeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance

/**
 * Computes type arguments of [castClass] that are statically known from [originalType] based on
 * the notion of *type parameter inheritance*:
 *
 * a type parameter `X` of [castClass] inherits a type parameter `A` of the class of [originalType] when
 * - `X` is used directly (without any projection) as the type argument corresponding to `A`
 *   in the supertype of [castClass] with the type constructor of [originalType];
 * - `X` has the same variance as `A`, or `X` is invariant while `A` is not (in the latter case
 *   the propagated argument is wrapped into the corresponding use-site projection);
 * - when [requireEqualBounds] is `true`: `X` has the same upper bounds as `A` after replacing
 *   the type parameters of [castClass] with the type parameters they inherit.
 *
 * The inheritance relation is a partial bijection; in ambiguous cases the first (leftmost) usage wins.
 *
 * For inherited type parameters, the resulting array contains the corresponding type argument of [originalType];
 */
fun FirSession.staticallyKnownTypeArgumentsByTypeParameterInheritance(
    castClass: FirRegularClassSymbol,
    originalType: ConeKotlinType,
    requireEqualBounds: Boolean,
): Array<ConeTypeProjection>? {
    val preparedType = originalType.lowerBoundIfFlexible().fullyExpandedType(this)

    if (preparedType is ConeIntersectionType) {
        val parameterCount = castClass.fir.typeParameters.size
        var result: Array<ConeTypeProjection>? = null
        for (intersectedType in preparedType.intersectedTypes) {
            val fromIntersected =
                staticallyKnownTypeArgumentsByTypeParameterInheritance(castClass, intersectedType, requireEqualBounds) ?: continue
            val current = result ?: Array<ConeTypeProjection>(parameterCount) { ConeStarProjection }.also { result = it }
            for (index in 0 until parameterCount) {
                // Prefer the first non-star argument available among the intersected types.
                if (current[index] == ConeStarProjection && fromIntersected[index] != ConeStarProjection) {
                    current[index] = fromIntersected[index]
                }
            }
        }
        return result
    }

    typeApproximator.approximateToSuperType(
        preparedType,
        TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
    )?.let {
        return staticallyKnownTypeArgumentsByTypeParameterInheritance(castClass, it, requireEqualBounds)
    }

    val originalLookupTag = preparedType.classLikeLookupTagIfAny ?: return null
    val originalClass = originalLookupTag.toRegularClassSymbol(this) ?: return null

    val supertypeWithParameters = with(typeContext) {
        AbstractTypeChecker.findCorrespondingSupertypes(
            newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false),
            castClass.fir.defaultType(), originalLookupTag,
        ).firstOrNull() as? ConeClassLikeType ?: return null
    }

    val castTypeParameters = castClass.fir.typeParameters.map { it.symbol }
    val originalTypeParameters = originalClass.fir.typeParameters.map { it.symbol }
    val originalArguments = preparedType.typeArguments

    // index of a cast class type parameter -> index of the original class type parameter it inherits
    val inheritanceMapping = IntArray(castTypeParameters.size) { -1 }

    for (position in supertypeWithParameters.typeArguments.indices) {
        if (position >= originalTypeParameters.size || position >= originalArguments.size) break
        val supertypeArgument = supertypeWithParameters.typeArguments[position]
        if (supertypeArgument.kind != ProjectionKind.INVARIANT) continue
        val supertypeArgumentType = supertypeArgument.type?.lowerBoundIfFlexible() as? ConeTypeParameterType ?: continue
        if (supertypeArgumentType.isMarkedNullable) continue
        val castTypeParameter = supertypeArgumentType.lookupTag.typeParameterSymbol
        val index = castTypeParameters.indexOf(castTypeParameter)
        if (index < 0) continue
        // The inheritance relation is a bijection with the leftmost bias.
        if (inheritanceMapping[index] != -1) continue
        if (inheritanceMapping.any { it == position }) continue

        inheritanceMapping[index] = position
    }

    if (requireEqualBounds) {
        // Check that inherited type parameters have the same upper bounds as the original ones
        // after replacing the cast class type parameters with the original class type parameters they inherit.
        // A failed check may invalidate other type parameters whose bounds refer to the failed one, hence the loop.
        var changed = true
        while (changed) {
            changed = false
            val substitutionMap = buildMap {
                for (index in inheritanceMapping.indices) {
                    val originalIndex = inheritanceMapping[index]
                    if (originalIndex == -1) continue
                    put(
                        castTypeParameters[index],
                        ConeTypeParameterType(originalTypeParameters[originalIndex].toLookupTag(), isMarkedNullable = false)
                    )
                }
            }
            val substitutor = substitutorByMap(substitutionMap, this)
            for (index in inheritanceMapping.indices) {
                val originalIndex = inheritanceMapping[index]
                if (originalIndex == -1) continue
                val castParameterBounds = castTypeParameters[index].resolvedBounds.map {
                    substitutor.substituteOrSelf(it.coneType)
                }
                val originalParameterBounds = originalTypeParameters[originalIndex].resolvedBounds.map { it.coneType }
                if (!areEqualBounds(castParameterBounds, originalParameterBounds)) {
                    inheritanceMapping[index] = -1
                    changed = true
                }
            }
        }
    }

    return Array(castTypeParameters.size) { index ->
        val originalIndex = inheritanceMapping[index]
        if (originalIndex == -1) return null
        val originalArgument = originalArguments[originalIndex]
        val castParameterVariance = castTypeParameters[index].variance
        val originalParameterVariance = originalTypeParameters[originalIndex].variance
        if (castParameterVariance == originalParameterVariance) {
            originalArgument
        } else {
            val argumentType = originalArgument.type ?: return@Array ConeStarProjection
            when (originalParameterVariance) {
                Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(argumentType)
                Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(argumentType)
                else -> ConeStarProjection
            }
        }
    }
}

private fun FirSession.areEqualBounds(first: List<ConeKotlinType>, second: List<ConeKotlinType>): Boolean {
    if (first.size != second.size) return false
    val state by lazy(LazyThreadSafetyMode.NONE) {
        typeContext.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false)
    }
    return first.indices.all { index ->
        first[index] == second[index] || AbstractTypeChecker.equalTypes(state, first[index], second[index])
    }
}
