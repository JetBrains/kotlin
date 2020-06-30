/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerLiteralTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScope
import org.jetbrains.kotlin.fir.scopes.scope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

fun ConeKotlinType.scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope? {
    return when (this) {
        is ConeKotlinErrorType -> null
        is ConeClassLikeType -> {
            val fullyExpandedType = fullyExpandedType(useSiteSession)
            val fir = fullyExpandedType.lookupTag.toSymbol(useSiteSession)?.fir as? FirClass<*> ?: return null

            val substitution = createSubstitution(fir.typeParameters, fullyExpandedType.typeArguments, useSiteSession)

            fir.scope(substitutorByMap(substitution), useSiteSession, scopeSession, skipPrivateMembers = false)
        }
        is ConeTypeParameterType -> {
            // TODO: support LibraryTypeParameterSymbol or get rid of it
            val fir = lookupTag.toSymbol().fir
            FirTypeIntersectionScope.prepareIntersectionScope(
                useSiteSession,
                FirStandardOverrideChecker(useSiteSession),
                fir.bounds.mapNotNullTo(mutableListOf()) {
                    it.coneType.scope(useSiteSession, scopeSession)
                }
            )
        }
        is ConeRawType -> lowerBound.scope(useSiteSession, scopeSession)
        is ConeFlexibleType -> lowerBound.scope(useSiteSession, scopeSession)
        is ConeIntersectionType -> FirTypeIntersectionScope.prepareIntersectionScope(
            useSiteSession,
            FirStandardOverrideChecker(useSiteSession),
            intersectedTypes.mapNotNullTo(mutableListOf()) {
                it.scope(useSiteSession, scopeSession)
            }
        )
        is ConeDefinitelyNotNullType -> original.scope(useSiteSession, scopeSession)
        is ConeIntegerLiteralType -> {
            @Suppress("USELESS_CAST") // TODO: remove once fixed: https://youtrack.jetbrains.com/issue/KT-35635
            scopeSession.getOrBuild(
                when {
                    isUnsigned -> FirIntegerLiteralTypeScope.ILTKey.Unsigned
                    else -> FirIntegerLiteralTypeScope.ILTKey.Signed
                },
                FirIntegerLiteralTypeScope.SCOPE_SESSION_KEY
            ) {
                FirIntegerLiteralTypeScope(useSiteSession, isUnsigned)
            } as FirTypeScope
        }
        else -> null
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

fun FirAnonymousObject.defaultType(): ConeClassLikeType {
    return this.typeRef.coneTypeSafe() ?: ConeClassLikeTypeImpl(
        symbol.toLookupTag(),
        emptyArray(),
        isNullable = false
    )
}
