/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

abstract class FirAbstractOverrideChecker : FirOverrideChecker {

    protected abstract fun isEqualTypes(candidateTypeRef: FirTypeRef, baseTypeRef: FirTypeRef, substitutor: ConeSubstitutor): Boolean

    private fun isCompatibleTypeParameters(
        overrideCandidate: FirTypeParameterRef,
        baseDeclaration: FirTypeParameterRef,
        substitutor: ConeSubstitutor
    ): Boolean {
        if (overrideCandidate.symbol == baseDeclaration.symbol) return true
        if (overrideCandidate !is FirTypeParameter || baseDeclaration !is FirTypeParameter) return false
        return overrideCandidate.bounds.zip(baseDeclaration.bounds).all { (aBound, bBound) -> isEqualTypes(aBound, bBound, substitutor) }
    }

    protected fun getSubstitutorIfTypeParametersAreCompatible(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirCallableMemberDeclaration<*>
    ): ConeSubstitutor? {
        val substitutor = buildSubstitutorForOverridesCheck(overrideCandidate, baseDeclaration) ?: return null
        if (
            overrideCandidate.typeParameters.zip(baseDeclaration.typeParameters).any { (override, base) ->
                !isCompatibleTypeParameters(override, base, substitutor)
            }
        ) return null
        return substitutor
    }
}

fun buildSubstitutorForOverridesCheck(
    overrideCandidate: FirCallableMemberDeclaration<*>,
    baseDeclaration: FirCallableMemberDeclaration<*>
): ConeSubstitutor? {
    if (overrideCandidate.typeParameters.size != baseDeclaration.typeParameters.size) return null

    if (baseDeclaration.typeParameters.isEmpty()) return ConeSubstitutor.Empty
    val types = baseDeclaration.typeParameters.map {
        ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
    }
    return substitutorByMap(overrideCandidate.typeParameters.map { it.symbol }.zip(types).toMap())
}
