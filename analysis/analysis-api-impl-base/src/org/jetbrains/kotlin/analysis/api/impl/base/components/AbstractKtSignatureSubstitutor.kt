/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.components.KaSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

abstract class KaAbstractSignatureSubstitutor : KaSignatureSubstitutor() {
    override fun <S : KaFunctionLikeSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaFunctionLikeSignature<S> {
        if (substitutor is KaSubstitutor.Empty) return asSignature(symbol)
        return asSignature(symbol).substitute(substitutor)
    }

    override fun <S : KaVariableLikeSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaVariableLikeSignature<S> {
        if (substitutor is KaSubstitutor.Empty) return asSignature(symbol)
        return asSignature(symbol).substitute(substitutor)
    }

    override fun <S : KaCallableSymbol> asSignature(symbol: S): KaCallableSignature<S> {
        return when (symbol) {
            is KaFunctionLikeSymbol -> asSignature(symbol)
            is KaVariableLikeSymbol -> asSignature(symbol)
            else -> unexpectedElementError("symbol", symbol)
        }
    }
}