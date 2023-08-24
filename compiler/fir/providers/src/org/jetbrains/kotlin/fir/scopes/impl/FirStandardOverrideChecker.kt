/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class FirStandardOverrideChecker(private val session: FirSession) : FirAbstractOverrideChecker() {
    private val context = session.typeContext

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

    fun isEqualTypes(candidateTypeRef: FirTypeRef, baseTypeRef: FirTypeRef, substitutor: ConeSubstitutor): Boolean {
        candidateTypeRef.ensureResolvedTypeDeclaration(session, requiredPhase = FirResolvePhase.TYPES)
        baseTypeRef.ensureResolvedTypeDeclaration(session, requiredPhase = FirResolvePhase.TYPES)
        if (candidateTypeRef is FirErrorTypeRef && baseTypeRef is FirErrorTypeRef) {
            return maybeEqualErrorTypes(candidateTypeRef, baseTypeRef)
        }
        return isEqualTypes(candidateTypeRef.coneType, baseTypeRef.coneType, substitutor)
    }

    private fun maybeEqualErrorTypes(ref1: FirErrorTypeRef, ref2: FirErrorTypeRef): Boolean {
        val delegated1 = ref1.delegatedTypeRef as? FirUserTypeRef ?: return false
        val delegated2 = ref2.delegatedTypeRef as? FirUserTypeRef ?: return false
        if (delegated1.qualifier.size != delegated2.qualifier.size) return false
        return delegated1.qualifier.zip(delegated2.qualifier).all { (l, r) -> l.name == r.name }
    }


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

        return overrideTypeParameter.symbol.resolvedBounds.any { bound -> isEqualTypes(bound.coneType, substitutedBaseType, substitutor) } &&
                baseTypeParameter.symbol.resolvedBounds.any { bound -> isEqualTypes(bound.coneType, substitutedOverrideType, substitutor) }
    }

    private fun isCompatibleTypeParameters(
        overrideCandidate: FirTypeParameterRef,
        baseDeclaration: FirTypeParameterRef,
        substitutor: ConeSubstitutor
    ): Boolean {
        if (overrideCandidate.symbol == baseDeclaration.symbol) return true
        if (overrideCandidate !is FirTypeParameter || baseDeclaration !is FirTypeParameter) return false
        if (overrideCandidate.bounds.size != baseDeclaration.bounds.size) return false
        return overrideCandidate.symbol.resolvedBounds.zip(baseDeclaration.symbol.resolvedBounds)
            .all { (aBound, bBound) -> isEqualBound(aBound, bBound, overrideCandidate, baseDeclaration, substitutor) }
    }

    override fun buildTypeParametersSubstitutorIfCompatible(
        overrideCandidate: FirCallableDeclaration,
        baseDeclaration: FirCallableDeclaration
    ): ConeSubstitutor? {
        overrideCandidate.lazyResolveToPhase(FirResolvePhase.TYPES)
        baseDeclaration.lazyResolveToPhase(FirResolvePhase.TYPES)
        val substitutor = buildSubstitutorForOverridesCheck(overrideCandidate, baseDeclaration, session) ?: return null
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
        return isOverriddenFunction(overrideCandidate, baseDeclaration, ignoreVisibility = false)
    }

    fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction, ignoreVisibility: Boolean): Boolean {
        if (overrideCandidate.valueParameters.size != baseDeclaration.valueParameters.size) return false

        val substitutor = buildTypeParametersSubstitutorIfCompatible(overrideCandidate, baseDeclaration) ?: return false

        if (!commonCallableChecks(overrideCandidate, baseDeclaration, substitutor, ignoreVisibility)) return false

        return overrideCandidate.valueParameters.zip(baseDeclaration.valueParameters).all { (memberParam, selfParam) ->
            isEqualTypes(memberParam.returnTypeRef, selfParam.returnTypeRef, substitutor)
        }
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean {
        return isOverriddenProperty(overrideCandidate, baseDeclaration, ignoreVisibility = false)
    }

    fun isOverriddenProperty(
        overrideCandidate: FirCallableDeclaration,
        baseDeclaration: FirProperty,
        ignoreVisibility: Boolean,
    ): Boolean {
        if (overrideCandidate !is FirProperty) return false
        val substitutor = buildTypeParametersSubstitutorIfCompatible(overrideCandidate, baseDeclaration) ?: return false
        return commonCallableChecks(overrideCandidate, baseDeclaration, substitutor, ignoreVisibility)
    }

    private fun FirStandardOverrideChecker.commonCallableChecks(
        overrideCandidate: FirCallableDeclaration,
        baseDeclaration: FirCallableDeclaration,
        substitutor: ConeSubstitutor,
        // Overload-ability is used to filter out equivalent calls (see ConeEquivalentCallConflictResolver) in which case visibility
        // must be ignored.
        ignoreVisibility: Boolean,
    ): Boolean {
        if (!ignoreVisibility && Visibilities.isPrivate(baseDeclaration.visibility)) return false
        if (overrideCandidate.contextReceivers.size != baseDeclaration.contextReceivers.size) return false

        overrideCandidate.lazyResolveToPhase(FirResolvePhase.TYPES)
        baseDeclaration.lazyResolveToPhase(FirResolvePhase.TYPES)

        return isEqualReceiverTypes(
            overrideCandidate.receiverParameter?.typeRef,
            baseDeclaration.receiverParameter?.typeRef,
            substitutor
        ) && overrideCandidate.contextReceivers.zip(baseDeclaration.contextReceivers).all { (memberParam, selfParam) ->
            isEqualTypes(memberParam.typeRef, selfParam.typeRef, substitutor)
        }
    }
}
