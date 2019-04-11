/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.resolve.transformers.firSafeNullable
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

fun ConeKotlinType.scope(useSiteSession: FirSession): FirScope? {
    return when (this) {
        is ConeKotlinErrorType -> null
        is ConeClassErrorType -> null
        is ConeAbbreviatedType -> directExpansionType(useSiteSession)?.scope(useSiteSession)
        is ConeClassLikeType -> {
            // For ConeClassLikeType they might be a type alias instead of a regular class
            // TODO: support that case and switch back to `firUnsafe` instead of `firSafeNullable`
            val fir = this.lookupTag.toSymbol(useSiteSession)?.firSafeNullable<FirRegularClass>() ?: return null
            fir.buildUseSiteScope(useSiteSession)
        }
        is ConeTypeParameterType -> {
            // TODO: support LibraryTypeParameterSymbol or get rid of it
            val toSymbol = this.lookupTag.toSymbol(useSiteSession)?.takeIf { it is FirBasedSymbol<*> } ?: return null
            val fir = toSymbol.firUnsafe<FirTypeParameter>()
            FirCompositeScope(fir.bounds.mapNotNullTo(mutableListOf()) { it.coneTypeUnsafe().scope(useSiteSession) })
        }
        is ConeFlexibleType -> lowerBound.scope(useSiteSession)
        else -> error("Failed type ${this}")
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
