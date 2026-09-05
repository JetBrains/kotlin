/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.fir.references

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

val FirReference.symbol: FirBasedSymbol<*>?
    get() = when (this) {
        is FirThisReference -> boundSymbol as FirBasedSymbol<*>?
        is FirResolvedNamedReference -> resolvedSymbol
        is FirNamedReferenceWithCandidateBase -> candidateSymbol
        else -> null
    }

val FirReference.resolved: FirResolvedNamedReference? get() = this as? FirResolvedNamedReference

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE",)
inline fun <reified T : FirBasedSymbol<*>> FirReference.toResolvedSymbol(
    discardErrorReference: Boolean = false
): @kotlin.internal.NoInfer T? {
    contract { returnsNotNull() implies (this@toResolvedSymbol is FirResolvedNamedReference) }
    if (discardErrorReference && this is FirResolvedErrorReference) {
        return null
    }
    return resolved?.resolvedSymbol as? T
}

fun FirReference.toResolvedBaseSymbol(discardErrorReference: Boolean = false): FirBasedSymbol<*>? {
    contract { returnsNotNull() implies (this@toResolvedBaseSymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirBasedSymbol<*>>(discardErrorReference)
}

fun FirReference.toResolvedCallableSymbol(discardErrorReference: Boolean = false): FirCallableSymbol<*>? {
    contract { returnsNotNull() implies (this@toResolvedCallableSymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirCallableSymbol<*>>(discardErrorReference)
}

fun FirReference.toResolvedTypeParameterSymbol(discardErrorReference: Boolean = false): FirTypeParameterSymbol? {
    contract { returnsNotNull() implies (this@toResolvedTypeParameterSymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirTypeParameterSymbol>(discardErrorReference)
}

fun FirReference.toResolvedVariableSymbol(discardErrorReference: Boolean = false): FirVariableSymbol<*>? {
    contract { returnsNotNull() implies (this@toResolvedVariableSymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirVariableSymbol<*>>(discardErrorReference)
}

fun FirReference.toResolvedPropertySymbol(discardErrorReference: Boolean = false): FirPropertySymbol? {
    contract { returnsNotNull() implies (this@toResolvedPropertySymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirPropertySymbol>(discardErrorReference)
}

fun FirReference.toResolvedValueParameterSymbol(discardErrorReference: Boolean = false): FirValueParameterSymbol? {
    contract { returnsNotNull() implies (this@toResolvedValueParameterSymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirValueParameterSymbol>(discardErrorReference)
}

fun FirReference.toResolvedFunctionSymbol(discardErrorReference: Boolean = false): FirFunctionSymbol<*>? {
    contract { returnsNotNull() implies (this@toResolvedFunctionSymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirFunctionSymbol<*>>(discardErrorReference)
}

fun FirReference.toResolvedNamedFunctionSymbol(discardErrorReference: Boolean = false): FirNamedFunctionSymbol? {
    contract { returnsNotNull() implies (this@toResolvedNamedFunctionSymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirNamedFunctionSymbol>(discardErrorReference)
}

fun FirReference.toResolvedConstructorSymbol(discardErrorReference: Boolean = false): FirConstructorSymbol? {
    contract { returnsNotNull() implies (this@toResolvedConstructorSymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirConstructorSymbol>(discardErrorReference)
}

fun FirReference.toResolvedEnumEntrySymbol(discardErrorReference: Boolean = false): FirEnumEntrySymbol? {
    contract { returnsNotNull() implies (this@toResolvedEnumEntrySymbol is FirResolvedNamedReference) }
    return this.toResolvedSymbol<FirEnumEntrySymbol>(discardErrorReference)
}

fun FirReference.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is FirDiagnosticHolder)
    }
    return when (this) {
        is FirResolvedErrorReference, is FirErrorNamedReference -> true
        else -> false
    }
}

fun buildErrorNamedReferenceWithNoName(diagnostic: ConeDiagnostic, source: KtSourceElement? = null): FirErrorNamedReference =
    buildErrorNamedReference {
        this.diagnostic = diagnostic
        this.source = source
        this.name = Name.special("<${diagnostic.reason}>")
    }
