/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
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
    val mapping = overrideCandidate.typeParameters.zip(baseDeclaration.typeParameters).associate { (from, to) -> from.symbol to to.symbol }
    return ConeSubstitutorForOverridesCheck(useSiteSession, mapping)
}

class ConeSubstitutorForOverridesCheck(
    session: FirSession,
    private val substitution: Map<FirTypeParameterSymbol, FirTypeParameterSymbol>,
) : AbstractConeSubstitutor(session.typeContext) {
    override fun substituteArgument(projection: ConeTypeProjection, lookupTag: ConeClassLikeLookupTag, index: Int): ConeTypeProjection? {
        // Erase captured types to the underlying type - the instantiations of these variables will be
        // the same in both declarations, so the subtyping relationships are valid.
        val type = projection.type ?: return null
        if (type is ConeCapturedType) return type.constructor.projection
        return super.substituteArgument(projection, lookupTag, index)
    }

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        return when (type) {
            is ConeTypeParameterType -> {
                // Replace override's type parameters with the base declaration's type parameters.
                val replaced = substitution[type.lookupTag.symbol] ?: return null
                ConeTypeParameterTypeImpl(replaced.toLookupTag(), type.isNullable)
            }
            is ConeCapturedType ->
                when (val projection = type.constructor.projection) {
                    is ConeKotlinTypeProjectionOut, is ConeKotlinType -> projection.type.updateNullabilityIfNeeded(type)
                    else -> null
                }
            else -> null
        }?.withCombinedAttributesFrom(type)
    }
}
