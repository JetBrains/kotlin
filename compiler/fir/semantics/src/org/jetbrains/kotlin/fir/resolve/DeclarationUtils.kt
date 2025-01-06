/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*

fun FirClassLikeDeclaration.getContainingDeclaration(session: FirSession): FirClassLikeDeclaration? {
    return symbol.getContainingDeclaration(session)?.fir
}

fun FirClassLikeSymbol<FirClassLikeDeclaration>.getContainingDeclaration(session: FirSession): FirClassLikeSymbol<FirClassLikeDeclaration>? {
    return session.firProvider.getContainingClass(this)
}

// TODO(KT-66349) Investigate and fix the contract.
//  - Why aren't we supporting nested `inner` classes?
//  - Why are we traversing super types?
fun isValidTypeParameterFromOuterDeclaration(
    typeParameterSymbol: FirTypeParameterSymbol,
    declaration: FirDeclaration?,
    session: FirSession
): Boolean {
    if (declaration == null) {
        return true  // Extra check is required because of classDeclaration will be resolved later
    }

    val visited = mutableSetOf<FirDeclaration>()

    fun containsTypeParameter(currentDeclaration: FirDeclaration?): Boolean {
        if (currentDeclaration == null || !visited.add(currentDeclaration)) {
            return false
        }

        if (currentDeclaration is FirTypeParameterRefsOwner) {
            if (currentDeclaration.typeParameters.any { it.symbol == typeParameterSymbol }) {
                return true
            }

            if (currentDeclaration is FirCallableDeclaration) {
                val containingClassLookupTag = currentDeclaration.containingClassLookupTag() ?: return true
                return containsTypeParameter(containingClassLookupTag.toSymbol(session)?.fir)
            } else if (currentDeclaration is FirClass) {
                for (superTypeRef in currentDeclaration.superTypeRefs) {
                    val superClassFir = superTypeRef.firClassLike(session) ?: return true
                    if (superClassFir is FirRegularClass && containsTypeParameter(superClassFir)) return true
                    if (superClassFir is FirTypeAlias && containsTypeParameter(superClassFir.fullyExpandedClass(session))) return true
                }
            }
        }

        return false
    }

    return containsTypeParameter(declaration)
}

fun FirTypeRef.firClassLike(session: FirSession): FirClassLikeDeclaration? {
    val type = coneTypeSafe<ConeClassLikeType>() ?: return null
    return type.lookupTag.toSymbol(session)?.fir
}

fun List<FirQualifierPart>.toTypeProjections(): Array<ConeTypeProjection> =
    asReversed().flatMap { it.typeArgumentList.typeArguments.map { typeArgument -> typeArgument.toConeTypeProjection() } }.toTypedArray()

interface FirCodeFragmentContext {
    val towerDataContext: FirTowerDataContext
    val smartCasts: Map<RealVariable, Set<ConeKotlinType>>
}

private object CodeFragmentContextDataKey : FirDeclarationDataKey()

var FirCodeFragment.codeFragmentContext: FirCodeFragmentContext? by FirDeclarationDataRegistry.data(CodeFragmentContextDataKey)

fun FirBasedSymbol<*>.isContextParameter(): Boolean {
    return this is FirValueParameterSymbol && this.fir.valueParameterKind == FirValueParameterKind.ContextParameter
}