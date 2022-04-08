/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun FirClass.constructors(session: FirSession): List<FirConstructorSymbol> {
    val result = mutableListOf<FirConstructorSymbol>()
    session.declaredMemberScope(this).processDeclaredConstructors { result += it }
    return result
}

fun FirClass.constructorsSortedByDelegation(session: FirSession): List<FirConstructorSymbol> {
    return constructors(session).sortedWith(ConstructorDelegationComparator)
}

fun FirClass.primaryConstructorIfAny(session: FirSession): FirConstructorSymbol? {
    return constructors(session).find(FirConstructorSymbol::isPrimary)
}

fun FirRegularClass.collectEnumEntries(): Collection<FirEnumEntry> {
    assert(classKind == ClassKind.ENUM_CLASS)
    return declarations.filterIsInstance<FirEnumEntry>()
}

fun FirRegularClassSymbol.collectEnumEntries(): Collection<FirEnumEntrySymbol> {
    return fir.collectEnumEntries().map { it.symbol }
}

val FirConstructorSymbol.delegatedThisConstructor: FirConstructorSymbol?
    get() = runIf(delegatedConstructorCallIsThis) { this.resolvedDelegatedConstructor }


private object ConstructorDelegationComparator : Comparator<FirConstructorSymbol> {
    override fun compare(p0: FirConstructorSymbol?, p1: FirConstructorSymbol?): Int {
        if (p0 == null && p1 == null) return 0
        if (p0 == null) return -1
        if (p1 == null) return 1
        if (p0.delegatedThisConstructor == p1) return 1
        if (p1.delegatedThisConstructor == p0) return -1
        // If neither is a delegation to each other, the order doesn't matter.
        // Here we return 0 to preserve the original order.
        return 0
    }
}

/**
 * Returns the FirClassLikeDeclaration that the
 * sequence of FirTypeAlias'es points to starting
 * with `this`. Or null if something goes wrong or we have anonymous object symbol.
 */
tailrec fun FirClassLikeSymbol<*>.fullyExpandedClass(useSiteSession: FirSession): FirRegularClassSymbol? {
    return when (this) {
        is FirRegularClassSymbol -> this
        is FirAnonymousObjectSymbol -> null
        is FirTypeAliasSymbol -> resolvedExpandedTypeRef.coneTypeSafe<ConeClassLikeType>()
            ?.toSymbol(useSiteSession)?.fullyExpandedClass(useSiteSession)
    }
}

fun FirBasedSymbol<*>.isAnnotationConstructor(session: FirSession): Boolean {
    if (this !is FirConstructorSymbol) return false
    return getConstructedClass(session)?.classKind == ClassKind.ANNOTATION_CLASS
}

fun FirBasedSymbol<*>.isEnumConstructor(session: FirSession): Boolean {
    if (this !is FirConstructorSymbol) return false
    return getConstructedClass(session)?.classKind == ClassKind.ENUM_CLASS
}

fun FirBasedSymbol<*>.isPrimaryConstructorOfInlineClass(session: FirSession): Boolean {
    if (this !is FirConstructorSymbol) return false
    return getConstructedClass(session)?.isInlineOrValueClass() == true && this.isPrimary
}

fun FirConstructorSymbol.getConstructedClass(session: FirSession): FirRegularClassSymbol? {
    return resolvedReturnTypeRef.coneType
        .fullyExpandedType(session)
        .toSymbol(session) as? FirRegularClassSymbol?
}

fun FirRegularClassSymbol.isInlineOrValueClass(): Boolean {
    if (this.classKind != ClassKind.CLASS) return false

    return isInline
}
