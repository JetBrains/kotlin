/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.resolve.transformers.firSafeNullable
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

fun ConeKotlinType.scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
    return when (this) {
        is ConeKotlinErrorType -> null
        is ConeClassErrorType -> null
        is ConeAbbreviatedType -> directExpansionType(useSiteSession)?.scope(useSiteSession, scopeSession)
        is ConeClassLikeType -> {
            // For ConeClassLikeType they might be a type alias instead of a regular class
            // TODO: support that case and switch back to `firUnsafe` instead of `firSafeNullable`
            val fir = this.lookupTag.toSymbol(useSiteSession)?.firSafeNullable<FirRegularClass>() ?: return null
            val companionScope = fir.companionObject?.buildUseSiteScope(useSiteSession, scopeSession)
            val ownScope = wrapSubstitutionScopeIfNeed(useSiteSession, fir.buildUseSiteScope(useSiteSession, scopeSession)!!, scopeSession)
            if (companionScope != null) FirCompositeScope(mutableListOf(ownScope, companionScope)) else ownScope

        }
        is ConeTypeParameterType -> {
            // TODO: support LibraryTypeParameterSymbol or get rid of it
            val toSymbol = this.lookupTag.toSymbol(useSiteSession)?.takeIf { it is FirBasedSymbol<*> } ?: return null
            val fir = toSymbol.firUnsafe<FirTypeParameter>()
            FirCompositeScope(
                fir.bounds.mapNotNullTo(mutableListOf()) {
                    it.coneTypeUnsafe<ConeKotlinType>().scope(useSiteSession, scopeSession)
                }
            )
        }
        is ConeFlexibleType -> lowerBound.scope(useSiteSession, scopeSession)
        else -> error("Failed type ${this}")
    }
}

fun ConeClassLikeType.wrapSubstitutionScopeIfNeed(
    session: FirSession,
    useSiteScope: FirScope,
    builder: ScopeSession
): FirScope {
    if (this.typeArguments.isEmpty()) return useSiteScope
    val symbol = this.lookupTag.toSymbol(session) as? FirClassSymbol ?: return useSiteScope
    val regularClass = symbol.fir
    return builder.getOrBuild(symbol, SubstitutionScopeKey(this)) {
        @Suppress("UNCHECKED_CAST")
        val substitution = regularClass.typeParameters.zip(this.typeArguments) { typeParameter, typeArgument ->
            typeParameter.symbol to (typeArgument as? ConeTypedProjection)?.type
        }.filter { (_, type) -> type != null }.toMap() as Map<ConeTypeParameterSymbol, ConeKotlinType>

        FirClassSubstitutionScope(session, useSiteScope, substitution)
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
