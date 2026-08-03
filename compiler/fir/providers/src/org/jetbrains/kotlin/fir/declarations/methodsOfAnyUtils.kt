/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.utils.equalityBoundTypeOfParameter
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.util.OperatorNameConventions

fun FirNamedFunctionSymbol.isEquals(session: FirSession): Boolean {
    if (name != OperatorNameConventions.EQUALS) return false
    if (contextParameterSymbols.isNotEmpty()) return false
    if (receiverParameterSymbol != null) return false
    if (dispatchReceiverType == null) return false
    val parameter = valueParameterSymbols.singleOrNull() ?: return false
    return parameter.resolvedReturnTypeRef.coneType.fullyExpandedType(session).isNullableAny
}

fun FirNamedFunction.isEquals(session: FirSession): Boolean = symbol.isEquals(session)

fun FirNamedFunctionSymbol.isHashCode(): Boolean {
    return when {
        name != OperatorNameConventions.HASH_CODE -> false
        contextParameterSymbols.isNotEmpty() -> false
        receiverParameterSymbol != null -> false
        dispatchReceiverType == null -> false
        valueParameterSymbols.isNotEmpty() -> false
        else -> true
    }
}

fun FirNamedFunction.isHashCode(): Boolean = symbol.isHashCode()

fun FirNamedFunctionSymbol.isToString(): Boolean {
    return when {
        name != OperatorNameConventions.TO_STRING -> false
        contextParameterSymbols.isNotEmpty() -> false
        receiverParameterSymbol != null -> false
        dispatchReceiverType == null -> false
        valueParameterSymbols.isNotEmpty() -> false
        else -> true
    }
}

fun FirNamedFunction.isToString(): Boolean = symbol.isToString()

context(holder: SessionHolder)
val FirNamedFunctionSymbol.isMethodOfAny: Boolean
    get() = isToString() || isHashCode() || isEquals(holder.session)

context(_: SessionHolder)
val FirNamedFunction.isMethodOfAny: Boolean
    get() = symbol.isMethodOfAny

@JvmName("setEqualityBoundTypeFromOverriddenSymbols")
fun FirNamedFunction.setEqualityBoundTypeFromOverridden(
    overridden: Collection<FirCallableSymbol<*>>,
    session: FirSession,
) {
    setEqualityBoundTypeFromOverridden(overridden.map { it.fir }, session)
}

fun FirNamedFunction.setEqualityBoundTypeFromOverridden(
    overridden: Collection<FirCallableDeclaration>,
    session: FirSession,
) {
    if (isEquals(session)) {
        val bounds = overridden.filterIsInstance<FirNamedFunction>().mapNotNull { it.equalityBoundTypeOfParameter }
        if (bounds.isNotEmpty()) {
            equalityBoundTypeOfParameter = session.typeContext.intersectTypes(bounds)
        }
    }
}

context(holder: SessionAndScopeSessionHolder)
fun ConeKotlinType.calculateEqualityBoundType(): ConeKotlinType? {
    var result: ConeKotlinType? = null
    var equalsFound = false
    this.scope(CallableCopyTypeCalculator.DoNothing, requiredMembersPhase = FirResolvePhase.STATUS)?.processFunctionsByName(
        OperatorNameConventions.EQUALS,
    ) { function ->
        // there should only be a single such function in green code
        // for red code let's always consider the first one
        if (!equalsFound && function.isEquals(holder.session)) {
            equalsFound = true
            function.equalityBoundTypeOfParameter?.let {
                result = it
            }
        }
    }
    // currently, flexible LHS results in nullable RHS
    return result?.withNullability(this.canBeNull(), holder.session.typeContext)
}
