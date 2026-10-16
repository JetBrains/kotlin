/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun ConeKotlinType.hasDiagnosticKind(kind: DiagnosticKind): Boolean {
    contract { returns(true) implies (this@hasDiagnosticKind is ConeErrorType) }
    return this is ConeErrorType && (diagnostic as? ConeSimpleDiagnostic)?.kind == kind
}

fun ConeKotlinType.fullyExpandedClassId(session: FirSession): ClassId? {
    return fullyExpandedType(session).classId
}

/**
 * Returns the closest to the end of context.containingDeclarations
 * item like FirRegularClass or FirAnonymousObject
 * or null if no such item could be found.
 */
fun CheckerContext.findClosestClassOrObject(): FirClassSymbol<*>? {
    for (it in containingDeclarations.asReversed()) {
        if (
            it is FirRegularClassSymbol ||
            it is FirAnonymousObjectSymbol
        ) {
            return it
        }
    }

    return null
}

data class FirTypeRefSource(val typeRef: FirTypeRef?, val source: KtSourceElement?) {
    override fun toString(): String {
        return "FirTypeRefSource(typeRef=${typeRef?.render()}, source=${source?.kind?.javaClass?.simpleName})"
    }
}

val FirClassLikeSymbol<*>.classKind: ClassKind?
    get() = (this as? FirClassSymbol<*>)?.classKind

val FirBasedSymbol<*>.typeParameterSymbols: List<FirTypeParameterSymbol>?
    get() = when (this) {
        is FirCallableSymbol<*> -> typeParameterSymbols
        is FirClassLikeSymbol<*> -> typeParameterSymbols
        else -> null
    }

fun FirTypeRef.toClassLikeSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return coneType.toClassLikeSymbol(session)
}

context(context: CheckerContext)
fun FirClassSymbol<*>.unsubstitutedScope(): FirTypeScope = unsubstitutedScope(context)

fun FirClassSymbol<*>.unsubstitutedScope(context: CheckerContext): FirTypeScope =
    this.unsubstitutedScope(
        context.sessionHolder.session,
        context.sessionHolder.scopeSession,
        withForcedTypeCalculator = true,
        memberRequiredPhase = FirResolvePhase.STATUS,
    )
