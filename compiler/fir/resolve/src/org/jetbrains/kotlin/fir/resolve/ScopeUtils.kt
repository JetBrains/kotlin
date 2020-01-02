/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.scope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

fun ConeKotlinType.scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
    return when (this) {
        is ConeKotlinErrorType -> null
        is ConeClassLikeType -> {
            val fullyExpandedType = fullyExpandedType(useSiteSession)
            val fir = fullyExpandedType.lookupTag.toSymbol(useSiteSession)?.fir as? FirClass<*> ?: return null

            val substitution = when (fir) {
                is FirTypeParametersOwner -> createSubstitution(fir.typeParameters, fullyExpandedType.typeArguments, useSiteSession)
                else -> emptyMap()
            }

            fir.scope(substitutorByMap(substitution), useSiteSession, scopeSession)
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
        is ConeIntegerLiteralType -> {

            @Suppress("USELESS_CAST") // TODO: remove once fixed: https://youtrack.jetbrains.com/issue/KT-35635
            scopeSession.getOrBuild(
                FirIntegerLiteralTypeScope.ILT_SYMBOL,
                FirIntegerLiteralTypeScope.SCOPE_SESSION_KEY
            ) {
                FirIntegerLiteralTypeScope(useSiteSession)
            } as FirScope
        }
        else -> error("Failed type $this")
    }
}

fun FirRegularClass.defaultType(): ConeClassLikeTypeImpl {
    return ConeClassLikeTypeImpl(
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

fun FirAnonymousObject.defaultType(): ConeClassLikeTypeImpl {
    return ConeClassLikeTypeImpl(
        symbol.toLookupTag(),
        emptyArray(),
        isNullable = false
    )
}
