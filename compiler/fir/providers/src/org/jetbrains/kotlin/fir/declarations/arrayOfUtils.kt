/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.resolve.ArrayFqNames

fun FirNamedFunctionSymbol.isArrayOfFunction(): Boolean {
    return callableId in ArrayFqNames.ARRAY_OF_CALLABLE_IDS
}

fun FirFunctionCall.isArrayOfCall(): Boolean {
    val symbol = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol ?: return false
    return symbol.isArrayOfFunction()
}
