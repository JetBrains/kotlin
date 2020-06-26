/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class FirStandardOverrideChecker(session: FirSession) : FirAbstractOverrideChecker() {

    private val context: ConeTypeContext = session.typeContext

    private fun isEqualTypes(substitutedCandidateType: ConeKotlinType, substitutedBaseType: ConeKotlinType): Boolean {
        return with(context) {
            val baseIsFlexible = substitutedBaseType.isFlexible()
            val candidateIsFlexible = substitutedCandidateType.isFlexible()
            if (baseIsFlexible == candidateIsFlexible) {
                return AbstractTypeChecker.equalTypes(context, substitutedCandidateType, substitutedBaseType)
            }
            val lowerBound: SimpleTypeMarker
            val upperBound: SimpleTypeMarker
            val type: KotlinTypeMarker
            if (baseIsFlexible) {
                lowerBound = substitutedBaseType.lowerBoundIfFlexible()
                upperBound = substitutedBaseType.upperBoundIfFlexible()
                type = substitutedCandidateType
            } else {
                lowerBound = substitutedCandidateType.lowerBoundIfFlexible()
                upperBound = substitutedCandidateType.upperBoundIfFlexible()
                type = substitutedBaseType
            }
            AbstractTypeChecker.isSubtypeOf(context, lowerBound, type) && AbstractTypeChecker.isSubtypeOf(context, type, upperBound)
        }
    }

    private fun isEqualTypes(candidateType: ConeKotlinType, baseType: ConeKotlinType, substitutor: ConeSubstitutor): Boolean {
        val substitutedCandidateType = substitutor.substituteOrSelf(candidateType)
        val substitutedBaseType = substitutor.substituteOrSelf(baseType)
        return isEqualTypes(substitutedCandidateType, substitutedBaseType)
    }

    override fun isEqualTypes(candidateTypeRef: FirTypeRef, baseTypeRef: FirTypeRef, substitutor: ConeSubstitutor) =
        isEqualTypes((candidateTypeRef as FirResolvedTypeRef).type, (baseTypeRef as FirResolvedTypeRef).type, substitutor)


    /**
     * Good case complexity is O(1)
     * Worst case complexity is O(N), where N is number of type-parameter bound's
     */
    private fun isEqualBound(
        overrideBound: FirTypeRef,
        baseBound: FirTypeRef,
        overrideTypeParameter: FirTypeParameter,
        baseTypeParameter: FirTypeParameter,
        substitutor: ConeSubstitutor
    ): Boolean {
        val substitutedOverrideType = substitutor.substituteOrSelf(overrideBound.coneTypeUnsafe())
        val substitutedBaseType = substitutor.substituteOrSelf(baseBound.coneTypeUnsafe())

        if (isEqualTypes(substitutedOverrideType, substitutedBaseType)) return true

        return overrideTypeParameter.bounds.any { bound -> isEqualTypes(bound.coneTypeUnsafe(), substitutedBaseType, substitutor) } &&
                baseTypeParameter.bounds.any { bound -> isEqualTypes(bound.coneTypeUnsafe(), substitutedOverrideType, substitutor) }
    }

    private fun isCompatibleTypeParameters(
        overrideCandidate: FirTypeParameterRef,
        baseDeclaration: FirTypeParameterRef,
        substitutor: ConeSubstitutor
    ): Boolean {
        if (overrideCandidate.symbol == baseDeclaration.symbol) return true
        if (overrideCandidate !is FirTypeParameter || baseDeclaration !is FirTypeParameter) return false
        if (overrideCandidate.bounds.size != baseDeclaration.bounds.size) return false
        return overrideCandidate.bounds.zip(baseDeclaration.bounds)
            .all { (aBound, bBound) -> isEqualBound(aBound, bBound, overrideCandidate, baseDeclaration, substitutor) }
    }

    override fun buildTypeParametersSubstitutorIfCompatible(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirCallableMemberDeclaration<*>
    ): ConeSubstitutor? {
        val substitutor = buildSubstitutorForOverridesCheck(overrideCandidate, baseDeclaration) ?: return null
        if (
            overrideCandidate.typeParameters.isNotEmpty() &&
            overrideCandidate.typeParameters.zip(baseDeclaration.typeParameters).any { (override, base) ->
                !isCompatibleTypeParameters(override, base, substitutor)
            }
        ) return null
        return substitutor
    }

    private fun isEqualReceiverTypes(candidateTypeRef: FirTypeRef?, baseTypeRef: FirTypeRef?, substitutor: ConeSubstitutor): Boolean {
        return when {
            candidateTypeRef != null && baseTypeRef != null -> isEqualTypes(candidateTypeRef, baseTypeRef, substitutor)
            else -> candidateTypeRef == null && baseTypeRef == null
        }
    }

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        if (overrideCandidate.valueParameters.size != baseDeclaration.valueParameters.size) return false

        val substitutor = buildTypeParametersSubstitutorIfCompatible(overrideCandidate, baseDeclaration) ?: return false

        if (!isEqualReceiverTypes(overrideCandidate.receiverTypeRef, baseDeclaration.receiverTypeRef, substitutor)) return false

        if (overrideCandidate.valueParameters.zip(baseDeclaration.valueParameters).any { (memberParam, selfParam) ->
                !isEqualTypes(memberParam.returnTypeRef, selfParam.returnTypeRef, substitutor)
            }) {
            return false
        }

        // Even though signatures are equivalent, consider multi platform.
        // That is, when one is `actual` and the other is `expect`, it's actually not overridden.
        // To be conservative, skip determining the overridden relation for actual/expect candidate.
        return !overrideCandidate.isActual && !overrideCandidate.isExpect
    }

    override fun isOverriddenProperty(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirProperty
    ): Boolean {
        if (overrideCandidate !is FirProperty) return false
        val substitutor = buildTypeParametersSubstitutorIfCompatible(overrideCandidate, baseDeclaration) ?: return false
        return isEqualReceiverTypes(overrideCandidate.receiverTypeRef, baseDeclaration.receiverTypeRef, substitutor)
    }
}