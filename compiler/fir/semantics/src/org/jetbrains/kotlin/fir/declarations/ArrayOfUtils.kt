/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isArrayType

fun FirNamedFunctionSymbol.isArrayOfFunction(
    session: FirSession,
    argumentList: FirArgumentList,
): Boolean {
    return fir.returnTypeRef.coneTypeOrNull?.fullyExpandedType(session)?.isArrayType == true &&
            isArrayOf(this, argumentList.arguments) &&
            this.receiverParameterSymbol == null
}

private val arrayOfNames = hashSetOf("kotlin/arrayOf") +
        hashSetOf(
            "boolean", "byte", "char", "double", "float", "int", "long", "short",
            "ubyte", "uint", "ulong", "ushort"
        ).map { "kotlin/" + it + "ArrayOf" }

private fun isArrayOf(function: FirNamedFunctionSymbol, arguments: List<FirExpression>): Boolean =
    when (function.callableId.toString()) {
        "kotlin/emptyArray" -> function.valueParameterSymbols.isEmpty() && arguments.isEmpty()
        in arrayOfNames -> function.valueParameterSymbols.size == 1 && function.valueParameterSymbols[0].isVararg && arguments.size <= 1
        else -> false
    }