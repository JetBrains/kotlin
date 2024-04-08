/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.scopes.scopeForTypeAlias
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
        val unwrappedConstructor = it.fir.typeAliasConstructorInfo?.originalConstructor?.unwrapSubstitutionOverrides() ?: it.fir
        if (unwrappedConstructor.isPrimary && constructorSymbol == null) {
            constructorSymbol = it
        }
    }
    return constructorSymbol
}

fun FirBasedSymbol<*>.collectAllSubclasses(session: FirSession): Set<FirBasedSymbol<*>> {
    return mutableSetOf<FirBasedSymbol<*>>().apply { collectAllSubclassesTo(this, session) }
}

private fun FirBasedSymbol<*>.collectAllSubclassesTo(destination: MutableSet<FirBasedSymbol<*>>, session: FirSession) {
    if (this !is FirRegularClassSymbol) {
        destination.add(this)
        return
    }
    when {
        fir.modality == Modality.SEALED -> fir.getSealedClassInheritors(session).forEach {
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol
            symbol?.collectAllSubclassesTo(destination, session)
        }
        fir.classKind == ClassKind.ENUM_CLASS -> fir.collectEnumEntries().mapTo(destination) { it.symbol }
        else -> destination.add(this)
    }
}