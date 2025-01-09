/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.scopes.scopeForTypeAlias
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol

fun FirClassLikeSymbol<*>.expandedClassWithConstructorsScope(
    session: FirSession,
    scopeSession: ScopeSession,
    memberRequiredPhaseForRegularClasses: FirResolvePhase?,
): Pair<FirRegularClassSymbol, FirScope>? {
    return when (this) {
        is FirRegularClassSymbol -> this to unsubstitutedScope(
            session, scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = memberRequiredPhaseForRegularClasses,
        )
        is FirTypeAliasSymbol -> {
            val expandedType = resolvedExpandedTypeRef.coneType as? ConeClassLikeType ?: return null
            val expandedClass = expandedType.toRegularClassSymbol(session) ?: return null
            expandedClass to this.fir.scopeForTypeAlias(session, scopeSession)
        }
        else -> null
    }
}


fun FirClassLikeSymbol<*>.getPrimaryConstructorSymbol(
    session: FirSession,
    scopeSession: ScopeSession,
): FirConstructorSymbol? {
    var constructorSymbol: FirConstructorSymbol? = null
    val (_, constructorsScope) = expandedClassWithConstructorsScope(
        session, scopeSession,
        memberRequiredPhaseForRegularClasses = null,
    ) ?: return null

    constructorsScope.processDeclaredConstructors {
        // Typealias constructors & SO override constructors of primary constructors are not marked as primary
        val unwrappedConstructor = it.fir.originalConstructorIfTypeAlias?.unwrapSubstitutionOverrides() ?: it.fir
        if (unwrappedConstructor.isPrimary && constructorSymbol == null) {
            constructorSymbol = it
        }
    }
    return constructorSymbol
}