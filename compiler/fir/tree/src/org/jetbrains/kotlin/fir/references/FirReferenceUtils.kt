/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

val FirReference.resolved: FirResolvedNamedReference? get() = this as? FirResolvedNamedReference

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE",)
inline fun <reified T : FirBasedSymbol<*>> FirReference.toResolvedSymbol(
    discardErrorReference: Boolean = false
): @kotlin.internal.NoInfer T? {
    val resolvedReference = resolved ?: return null
    if (discardErrorReference && resolvedReference is FirResolvedErrorReference) {
        return null
    }
    return resolvedReference.resolvedSymbol as? T
}

fun FirReference.toResolvedBaseSymbol(discardErrorReference: Boolean = false): FirBasedSymbol<*>? {
    return this.toResolvedSymbol<FirBasedSymbol<*>>(discardErrorReference)
}

fun FirReference.toResolvedCallableSymbol(discardErrorReference: Boolean = false): FirCallableSymbol<*>? {
    return this.toResolvedSymbol<FirCallableSymbol<*>>(discardErrorReference)
}

fun FirReference.toResolvedTypeParameterSymbol(discardErrorReference: Boolean = false): FirTypeParameterSymbol? {
    return this.toResolvedSymbol<FirTypeParameterSymbol>(discardErrorReference)
}

fun FirReference.toResolvedVariableSymbol(discardErrorReference: Boolean = false): FirVariableSymbol<*>? {
    return this.toResolvedSymbol<FirVariableSymbol<*>>(discardErrorReference)
}

fun FirReference.toResolvedPropertySymbol(discardErrorReference: Boolean = false): FirPropertySymbol? {
    return this.toResolvedSymbol<FirPropertySymbol>(discardErrorReference)
}

fun FirReference.toResolvedValueParameterSymbol(discardErrorReference: Boolean = false): FirValueParameterSymbol? {
    return this.toResolvedSymbol<FirValueParameterSymbol>(discardErrorReference)
}

fun FirReference.toResolvedFunctionSymbol(discardErrorReference: Boolean = false): FirFunctionSymbol<*>? {
    return this.toResolvedSymbol<FirFunctionSymbol<*>>(discardErrorReference)
}

fun FirReference.toResolvedNamedFunctionSymbol(discardErrorReference: Boolean = false): FirNamedFunctionSymbol? {
    return this.toResolvedSymbol<FirNamedFunctionSymbol>(discardErrorReference)
}

fun FirReference.toResolvedConstructorSymbol(discardErrorReference: Boolean = false): FirConstructorSymbol? {
    return this.toResolvedSymbol<FirConstructorSymbol>(discardErrorReference)
}

fun FirReference.toResolvedEnumEntrySymbol(discardErrorReference: Boolean = false): FirEnumEntrySymbol? {
    return this.toResolvedSymbol<FirEnumEntrySymbol>(discardErrorReference)
}

@OptIn(ExperimentalContracts::class)
fun FirReference.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is FirDiagnosticHolder)
    }
    return when (this) {
        is FirResolvedErrorReference, is FirErrorNamedReference -> true
        else -> false
    }
}

