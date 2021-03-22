/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolved
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class FirStandardOverrideChecker(private val session: FirSession) : FirAbstractOverrideChecker() {
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
        val substitutedCandidateType = substitutor.substituteOrSelf(candidateType).unwrapDefinitelyNotNullType()
        val substitutedBaseType = substitutor.substituteOrSelf(baseType).unwrapDefinitelyNotNullType()
        return isEqualTypes(substitutedCandidateType, substitutedBaseType)
    }

    private fun ConeKotlinType.unwrapDefinitelyNotNullType(): ConeKotlinType = when (this) {
        is ConeDefinitelyNotNullType -> original
        else -> this
    }

    fun isEqualTypes(candidateTypeRef: FirTypeRef, baseTypeRef: FirTypeRef, substitutor: ConeSubstitutor) =
        isEqualTypes(candidateTypeRef.coneType, baseTypeRef.coneType, substitutor)


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
        val substitutedOverrideType = substitutor.substituteOrSelf(overrideBound.coneType)
        val substitutedBaseType = substitutor.substituteOrSelf(baseBound.coneType)

        if (isEqualTypes(substitutedOverrideType, substitutedBaseType)) return true

        return overrideTypeParameter.bounds.any { bound -> isEqualTypes(bound.coneType, substitutedBaseType, substitutor) } &&
                baseTypeParameter.bounds.any { bound -> isEqualTypes(bound.coneType, substitutedOverrideType, substitutor) }
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
        if (Visibilities.isPrivate(baseDeclaration.visibility)) return false

        if (overrideCandidate.valueParameters.size != baseDeclaration.valueParameters.size) return false

        val substitutor = buildTypeParametersSubstitutorIfCompatible(overrideCandidate, baseDeclaration) ?: return false

        overrideCandidate.ensureResolved(FirResolvePhase.TYPES, useSiteSession = session)
        baseDeclaration.ensureResolved(FirResolvePhase.TYPES, useSiteSession = session)

        if (!isEqualReceiverTypes(overrideCandidate.receiverTypeRef, baseDeclaration.receiverTypeRef, substitutor)) return false

        return overrideCandidate.valueParameters.zip(baseDeclaration.valueParameters).all { (memberParam, selfParam) ->
            isEqualTypes(memberParam.returnTypeRef, selfParam.returnTypeRef, substitutor)
        }
    }

    override fun isOverriddenProperty(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirProperty
    ): Boolean {
        if (Visibilities.isPrivate(baseDeclaration.visibility)) return false

        if (overrideCandidate !is FirProperty) return false
        val substitutor = buildTypeParametersSubstitutorIfCompatible(overrideCandidate, baseDeclaration) ?: return false
        return isEqualReceiverTypes(overrideCandidate.receiverTypeRef, baseDeclaration.receiverTypeRef, substitutor)
    }
}
