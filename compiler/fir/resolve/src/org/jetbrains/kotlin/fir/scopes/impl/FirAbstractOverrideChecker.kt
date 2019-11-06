/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

abstract class FirAbstractOverrideChecker : FirOverrideChecker {

    protected abstract fun isEqualTypes(candidateTypeRef: FirTypeRef, baseTypeRef: FirTypeRef, substitutor: ConeSubstitutor): Boolean

    protected fun getSubstitutorIfTypeParametersAreCompatible(
        overrideCandidate: FirSimpleFunction,
        baseDeclaration: FirSimpleFunction
    ): ConeSubstitutor? {
        if (overrideCandidate.typeParameters.size != baseDeclaration.typeParameters.size) return null

        val types = baseDeclaration.typeParameters.map {
            ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
        }
        val substitutor = substitutorByMap(overrideCandidate.typeParameters.map { it.symbol }.zip(types).toMap())
        if (!overrideCandidate.typeParameters.zip(baseDeclaration.typeParameters).all { (a, b) ->
                a.bounds.size == b.bounds.size && a.bounds.zip(b.bounds).all { (aBound, bBound) ->
                    isEqualTypes(aBound, bBound, substitutor)
                }
            }
        ) return null
        return substitutor
    }
}