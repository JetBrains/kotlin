/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.resolve.ArrayFqNames

private fun FirNamedFunctionSymbol.isArrayOfFunction(): Boolean {
    return callableId in ArrayFqNames.ARRAY_OF_CALLABLE_IDS
}

fun FirNamedFunctionSymbol.isArrayOfOrArrayDotOfFunction(): Boolean {
    return callableId in ArrayFqNames.ARRAY_OF_CALLABLE_IDS || callableId in ArrayFqNames.ARRAY_DOT_OF_CALLABLE_IDS
}

fun FirFunctionCall.isArrayOfCall(): Boolean {
    val symbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol ?: return false
    return symbol.isArrayOfFunction()
}

fun FirFunctionCall.isArrayOfOrArrayDotOfCall(): Boolean {
    val symbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol ?: return false
    return symbol.isArrayOfOrArrayDotOfFunction()
}

/**
 * Flattens arguments of `*arrayOf` call (they are wrapped into [FirVarargArgumentsExpression]).
 */
fun FirFunctionCall.unwrapArgumentsOfArrayOfCall(): List<FirExpression> {
    return arguments.flatMap { (it as? FirVarargArgumentsExpression)?.arguments ?: [it] }
}
