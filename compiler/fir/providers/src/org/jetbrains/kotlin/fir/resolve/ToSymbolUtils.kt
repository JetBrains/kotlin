/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.name.ClassId

// ----------------------------------------------- lookup tag -----------------------------------------------

/**
 * Main operation on the [ConeClassifierLookupTag]
 *
 * Lookups the tag into its target within the given [useSiteSession]
 *
 * The second step of type refinement, see `/docs/fir/k2_kmp.md`
 *
 * @see ConeClassifierLookupTag
 */
fun ConeClassifierLookupTag.toSymbol(useSiteSession: FirSession): FirClassifierSymbol<*>? {
    return when (this) {
        is ConeClassLikeLookupTag -> toSymbol(useSiteSession)
        is ConeClassifierLookupTagWithFixedSymbol -> this.symbol
        else -> error("missing branch for ${javaClass.name}")
    }
}

fun ConeClassifierLookupTag.toClassLikeSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
    return toSymbol(useSiteSession) as? FirClassLikeSymbol<*>
}

fun ConeClassifierLookupTag.toRegularClassSymbol(useSiteSession: FirSession): FirRegularClassSymbol? {
    return toSymbol(useSiteSession) as? FirRegularClassSymbol
}

/**
 * @see toSymbol
 */
@OptIn(LookupTagInternals::class)
fun ConeClassLikeLookupTag.toSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
    if (this is ConeClassLookupTagWithFixedSymbol) {
        return this.symbol
    }
    (this as? ConeClassLikeLookupTagImpl)?.boundSymbol?.takeIf { it.first === useSiteSession }?.let { return it.second }

    return useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId).also {
        (this as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(useSiteSession, it)
    }
}

/**
 * @see toSymbol
 */
fun ConeClassLikeLookupTag.toClassSymbol(session: FirSession): FirClassSymbol<*>? {
    return toSymbol(session) as? FirClassSymbol<*>
}

/**
 * @see toSymbol
 */
fun ConeClassLikeLookupTag.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return toSymbol(session) as? FirRegularClassSymbol
}

fun ConeClassLikeLookupTag.toTypeAliasSymbol(useSiteSession: FirSession): FirTypeAliasSymbol? {
    return toSymbol(useSiteSession) as? FirTypeAliasSymbol
}

// ----------------------------------------------- cone type (without expansion) -----------------------------------------------

fun ConeClassLikeType.toSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return lookupTag.toSymbol(session)
}

fun ConeKotlinType.toSymbol(session: FirSession): FirClassifierSymbol<*>? {
    return (this as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session)
}

fun ConeKotlinType.toClassLikeSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return toSymbol(session) as? FirClassLikeSymbol
}

fun ConeKotlinType.toTypeAliasSymbol(session: FirSession): FirTypeAliasSymbol? {
    return toSymbol(session) as? FirTypeAliasSymbol
}

fun ConeKotlinType.toTypeParameterSymbol(session: FirSession): FirTypeParameterSymbol? {
    return toSymbol(session) as? FirTypeParameterSymbol
}

// ----------------------------------------------- cone type (with expansion) -----------------------------------------------

fun ConeKotlinType.toClassSymbol(session: FirSession): FirClassSymbol<*>? {
    return (this as? ConeClassLikeType)?.toClassSymbol(session)
}

fun ConeClassLikeType.toClassSymbol(session: FirSession): FirClassSymbol<*>? {
    return fullyExpandedType(session).toSymbol(session) as? FirClassSymbol<*>
}

fun ConeKotlinType.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return (this as? ConeClassLikeType)?.toRegularClassSymbol(session)
}

/**
 * Returns the FirRegularClassSymbol associated with this
 * or null of something goes wrong.
 */
fun ConeClassLikeType.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol
}

// ----------------------------------------------- ClassId -----------------------------------------------

fun ClassId.toSymbol(session: FirSession): FirClassifierSymbol<*>? {
    return session.symbolProvider.getClassLikeSymbolByClassId(this)
}
