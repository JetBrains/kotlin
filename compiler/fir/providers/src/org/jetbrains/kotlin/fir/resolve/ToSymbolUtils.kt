/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
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

/**
 * @see toSymbol
 */
context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toSymbol(): FirClassifierSymbol<*>? {
    return toSymbol(useSiteSession = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toSymbol(@Suppress("unused") s: FirSession): FirClassifierSymbol<*>? {
    return toSymbol()
}

fun ConeClassifierLookupTag.toClassLikeSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
    return toSymbol(useSiteSession) as? FirClassLikeSymbol<*>
}

context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toClassLikeSymbol(): FirClassLikeSymbol<*>? {
    return toClassLikeSymbol(useSiteSession = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toClassLikeSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toClassLikeSymbol(@Suppress("unused") s: FirSession): FirClassLikeSymbol<*>? {
    return toClassLikeSymbol()
}

fun ConeClassifierLookupTag.toRegularClassSymbol(useSiteSession: FirSession): FirRegularClassSymbol? {
    return toSymbol(useSiteSession) as? FirRegularClassSymbol
}

context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toRegularClassSymbol(): FirRegularClassSymbol? {
    return toRegularClassSymbol(useSiteSession = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toRegularClassSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toRegularClassSymbol(@Suppress("unused") s: FirSession): FirRegularClassSymbol? {
    return toRegularClassSymbol()
}

/**
 * @see toSymbol
 */
@OptIn(LookupTagInternals::class)
fun ConeClassLikeLookupTag.toSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
    if (this is ConeClassLikeLookupTagWithFixedSymbol) {
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
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toSymbol(): FirClassLikeSymbol<*>? {
    return toSymbol(useSiteSession = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toSymbol(@Suppress("unused") s: FirSession): FirClassLikeSymbol<*>? {
    return toSymbol()
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
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toClassSymbol(): FirClassSymbol<*>? {
    return toClassSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toClassSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toClassSymbol(@Suppress("unused") s: FirSession): FirClassSymbol<*>? {
    return toClassSymbol()
}

/**
 * @see toSymbol
 */
fun ConeClassLikeLookupTag.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return toSymbol(session) as? FirRegularClassSymbol
}

/**
 * @see toSymbol
 */
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toRegularClassSymbol(): FirRegularClassSymbol? {
    return toRegularClassSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toRegularClassSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toRegularClassSymbol(@Suppress("unused") s: FirSession): FirRegularClassSymbol? {
    return toRegularClassSymbol()
}

fun ConeClassLikeLookupTag.toTypeAliasSymbol(useSiteSession: FirSession): FirTypeAliasSymbol? {
    return toSymbol(useSiteSession) as? FirTypeAliasSymbol
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toTypeAliasSymbol(): FirTypeAliasSymbol? {
    return toTypeAliasSymbol(useSiteSession = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toTypeAliasSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toTypeAliasSymbol(@Suppress("unused") s: FirSession): FirTypeAliasSymbol? {
    return toTypeAliasSymbol()
}

// ----------------------------------------------- cone type (without expansion) -----------------------------------------------

fun ConeClassLikeType.toSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return lookupTag.toSymbol(session)
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toSymbol(): FirClassLikeSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toSymbol(@Suppress("unused") s: FirSession): FirClassLikeSymbol<*>? {
    return toSymbol()
}

fun ConeKotlinType.toSymbol(session: FirSession): FirClassifierSymbol<*>? {
    return (this.lowerBoundIfFlexible() as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session)
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.toSymbol(): FirClassifierSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeKotlinType.toSymbol(@Suppress("unused") s: FirSession): FirClassifierSymbol<*>? {
    return toSymbol()
}

fun ConeKotlinType.toClassLikeSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return toSymbol(session) as? FirClassLikeSymbol
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.toClassLikeSymbol(): FirClassLikeSymbol<*>? {
    return toClassLikeSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toClassLikeSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeKotlinType.toClassLikeSymbol(@Suppress("unused") s: FirSession): FirClassLikeSymbol<*>? {
    return toClassLikeSymbol()
}

fun ConeKotlinType.toTypeAliasSymbol(session: FirSession): FirTypeAliasSymbol? {
    return toSymbol(session) as? FirTypeAliasSymbol
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.toTypeAliasSymbol(): FirTypeAliasSymbol? {
    return toTypeAliasSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toTypeAliasSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeKotlinType.toTypeAliasSymbol(@Suppress("unused") s: FirSession): FirTypeAliasSymbol? {
    return toTypeAliasSymbol()
}

fun ConeKotlinType.toTypeParameterSymbol(session: FirSession): FirTypeParameterSymbol? {
    return toSymbol(session) as? FirTypeParameterSymbol
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.toTypeParameterSymbol(): FirTypeParameterSymbol? {
    return toTypeParameterSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toTypeParameterSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeKotlinType.toTypeParameterSymbol(@Suppress("unused") s: FirSession): FirTypeParameterSymbol? {
    return toTypeParameterSymbol()
}

// ----------------------------------------------- cone type (with expansion) -----------------------------------------------

fun ConeKotlinType.toClassSymbol(session: FirSession): FirClassSymbol<*>? {
    return (this.lowerBoundIfFlexible() as? ConeClassLikeType)?.toClassSymbol(session)
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.toClassSymbol(): FirClassSymbol<*>? {
    return toClassSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toClassSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeKotlinType.toClassSymbol(@Suppress("unused") s: FirSession): FirClassSymbol<*>? {
    return toClassSymbol()
}

fun ConeClassLikeType.toClassSymbol(session: FirSession): FirClassSymbol<*>? {
    return fullyExpandedType(session).toSymbol(session) as? FirClassSymbol<*>
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toClassSymbol(): FirClassSymbol<*>? {
    return toClassSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toClassSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toClassSymbol(@Suppress("unused") s: FirSession): FirClassSymbol<*>? {
    return toClassSymbol()
}

fun ConeKotlinType.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return (this.lowerBoundIfFlexible() as? ConeClassLikeType)?.toRegularClassSymbol(session)
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.toRegularClassSymbol(): FirRegularClassSymbol? {
    return toRegularClassSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toRegularClassSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeKotlinType.toRegularClassSymbol(@Suppress("unused") s: FirSession): FirRegularClassSymbol? {
    return toRegularClassSymbol()
}

/**
 * Returns the FirRegularClassSymbol associated with this
 * or null of something goes wrong.
 */
fun ConeClassLikeType.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toRegularClassSymbol(): FirRegularClassSymbol? {
    return toRegularClassSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toRegularClassSymbol()"))
context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toRegularClassSymbol(@Suppress("unused") s: FirSession): FirRegularClassSymbol? {
    return toRegularClassSymbol()
}

// ----------------------------------------------- ClassId -----------------------------------------------

fun ClassId.toSymbol(session: FirSession): FirClassifierSymbol<*>? {
    return session.symbolProvider.getClassLikeSymbolByClassId(this)
}

context(sessionHolder: SessionHolder)
fun ClassId.toSymbol(): FirClassifierSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}

@Deprecated("Use parameterless overload", replaceWith = ReplaceWith("toSymbol()"))
context(sessionHolder: SessionHolder)
fun ClassId.toSymbol(@Suppress("unused") s: FirSession): FirClassifierSymbol<*>? {
    return toSymbol()
}
