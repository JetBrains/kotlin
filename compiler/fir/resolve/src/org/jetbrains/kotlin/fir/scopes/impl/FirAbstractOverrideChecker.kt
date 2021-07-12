/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

abstract class FirAbstractOverrideChecker : FirOverrideChecker {

    protected abstract fun buildTypeParametersSubstitutorIfCompatible(
        overrideCandidate: FirCallableDeclaration,
        baseDeclaration: FirCallableDeclaration
    ): ConeSubstitutor?
}

fun buildSubstitutorForOverridesCheck(
    overrideCandidate: FirCallableDeclaration,
    baseDeclaration: FirCallableDeclaration,
    useSiteSession: FirSession
): ConeSubstitutor? {
    if (overrideCandidate.typeParameters.size != baseDeclaration.typeParameters.size) return null

    if (baseDeclaration.typeParameters.isEmpty()) return ConeSubstitutor.Empty
    val types = baseDeclaration.typeParameters.map {
        ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
    }
    return substitutorByMap(overrideCandidate.typeParameters.map { it.symbol }.zip(types).toMap(), useSiteSession)
}
