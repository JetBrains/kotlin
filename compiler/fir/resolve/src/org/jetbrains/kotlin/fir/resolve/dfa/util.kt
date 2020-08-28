/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.PersistentMap
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference
import org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
internal inline fun <K, V> MutableMap<K, V>.put(key: K, value: V, remappingFunction: (existing: V) -> V) {
    contract {
        callsInPlace(remappingFunction, InvocationKind.AT_MOST_ONCE)
    }
    val existing = this[key]
    if (existing == null) {
        put(key, value)
    } else {
        put(key, remappingFunction(existing))
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun <K, V> PersistentMap<K, V>.put(
    key: K,
    valueProducer: () -> V,
    remappingFunction: (existing: V) -> V
): PersistentMap<K, V> {
    contract {
        callsInPlace(remappingFunction, InvocationKind.AT_MOST_ONCE)
        callsInPlace(valueProducer, InvocationKind.AT_MOST_ONCE)
    }
    val existing = this[key]
    return if (existing == null) {
        put(key, valueProducer())
    } else {
        put(key, remappingFunction(existing))
    }
}

@DfaInternals
internal fun FirOperation.invert(): FirOperation = when (this) {
    FirOperation.EQ -> FirOperation.NOT_EQ
    FirOperation.NOT_EQ -> FirOperation.EQ
    FirOperation.IDENTITY -> FirOperation.NOT_IDENTITY
    FirOperation.NOT_IDENTITY -> FirOperation.IDENTITY
    else -> throw IllegalArgumentException("$this can not be inverted")
}

@DfaInternals
internal fun FirOperation.isEq(): Boolean {
    return when (this) {
        FirOperation.EQ, FirOperation.IDENTITY -> true
        FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> false
        else -> throw IllegalArgumentException("$this should not be there")
    }
}

internal fun FirFunctionCall.isBooleanNot(): Boolean {
    val symbol = calleeReference.safeAs<FirResolvedNamedReference>()?.resolvedSymbol as? FirNamedFunctionSymbol ?: return false
    return symbol.callableId == FirDataFlowAnalyzer.KOTLIN_BOOLEAN_NOT
}

fun ConeConstantReference.toOperation(): Operation = when (this) {
    ConeConstantReference.NULL -> Operation.EqNull
    ConeConstantReference.NOT_NULL -> Operation.NotEqNull
    ConeBooleanConstantReference.TRUE -> Operation.EqTrue
    ConeBooleanConstantReference.FALSE -> Operation.EqFalse
    else -> throw IllegalArgumentException("$this can not be transformed to Operation")
}

@DfaInternals
internal val FirExpression.coneType: ConeKotlinType
    get() = typeRef.coneType

@DfaInternals
internal val FirElement.symbol: AbstractFirBasedSymbol<*>?
    get() = when (this) {
        is FirResolvable -> symbol
        is FirSymbolOwner<*> -> symbol
        is FirWhenSubjectExpression -> whenRef.value.subject?.symbol
        is FirSafeCallExpression -> regularQualifiedAccess.symbol
        else -> null
    }?.takeIf { this is FirThisReceiverExpression || (it !is FirFunctionSymbol<*> && it !is FirAccessorSymbol) }

@DfaInternals
internal val FirResolvable.symbol: AbstractFirBasedSymbol<*>?
    get() = when (val reference = calleeReference) {
        is FirThisReference -> reference.boundSymbol
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirNamedReferenceWithCandidate -> reference.candidateSymbol
        else -> null
    }
