/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.components.KtSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

abstract class AbstractKtSignatureSubstitutor : KtSignatureSubstitutor() {
    override fun <S : KtFunctionLikeSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtFunctionLikeSignature<S> {
        if (substitutor is KtSubstitutor.Empty) return asSignature(symbol)
        return asSignature(symbol).substitute(substitutor)
    }

    override fun <S : KtVariableLikeSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtVariableLikeSignature<S> {
        if (substitutor is KtSubstitutor.Empty) return asSignature(symbol)
        return asSignature(symbol).substitute(substitutor)
    }

    override fun <S : KtCallableSymbol> asSignature(symbol: S): KtCallableSignature<S> {
        return when (symbol) {
            is KtFunctionLikeSymbol -> asSignature(symbol)
            is KtVariableLikeSymbol -> asSignature(symbol)
            else -> unexpectedElementError("symbol", symbol)
        }
    }
}