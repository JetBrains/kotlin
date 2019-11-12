/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

fun ConeKotlinType.scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
    return when (this) {
        is ConeKotlinErrorType -> null
        is ConeAbbreviatedType -> directExpansionType(useSiteSession)?.scope(useSiteSession, scopeSession)
        is ConeClassLikeType -> {
            // TODO: for ConeClassLikeType they might be a type alias instead of a class
            val fir = this.lookupTag.toSymbol(useSiteSession)?.fir as? FirClass<*> ?: return null
            wrapSubstitutionScopeIfNeed(useSiteSession, fir.buildUseSiteMemberScope(useSiteSession, scopeSession)!!, fir, scopeSession)
        }
        is ConeTypeParameterType -> {
            // TODO: support LibraryTypeParameterSymbol or get rid of it
            val fir = lookupTag.toSymbol().fir
            FirCompositeScope(
                fir.bounds.mapNotNullTo(mutableListOf()) {
                    it.coneTypeUnsafe<ConeKotlinType>().scope(useSiteSession, scopeSession)
                }
            )
        }
        is ConeRawType -> lowerBound.scope(useSiteSession, scopeSession)
        is ConeFlexibleType -> lowerBound.scope(useSiteSession, scopeSession)
        is ConeIntersectionType -> FirCompositeScope(
            intersectedTypes.mapNotNullTo(mutableListOf()) {
                it.scope(useSiteSession, scopeSession)
            }
        )
        is ConeDefinitelyNotNullType -> original.scope(useSiteSession, scopeSession)
        else -> error("Failed type $this")
    }
}

fun FirRegularClass.defaultType(): ConeClassTypeImpl {
    return ConeClassTypeImpl(
        symbol.toLookupTag(),
        typeParameters.map {
            ConeTypeParameterTypeImpl(
                it.symbol.toLookupTag(),
                isNullable = false
            )
        }.toTypedArray(),
        isNullable = false
    )
}

fun FirAnonymousObject.defaultType(): ConeClassTypeImpl {
    return ConeClassTypeImpl(
        symbol.toLookupTag(),
        emptyArray(),
        isNullable = false
    )
}
