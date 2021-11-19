/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol

public fun KtCallTarget.getSuccessCallSymbolOrNull(): KtFunctionLikeSymbol? = when (this) {
    is KtSuccessCallTarget -> symbol
    is KtErrorCallTarget -> null
}

public fun KtCallTarget.getSingleCandidateSymbolOrNull(): KtFunctionLikeSymbol? = when (this) {
    is KtSuccessCallTarget -> symbol
    is KtErrorCallTarget -> candidates.singleOrNull()
}

public inline fun <reified S : KtFunctionLikeSymbol> KtCall.isSuccessCallOf(predicate: (S) -> Boolean): Boolean {
    if (this !is KtSimpleFunctionCall) return false
    val symbol = targetFunction.getSuccessCallSymbolOrNull() ?: return false
    if (symbol !is S) return false
    return predicate(symbol)
}