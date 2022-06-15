/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.components.KtSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

abstract class AbstractKtSignatureSubstitutorImpl : KtSignatureSubstitutor() {

    override fun <S : KtVariableLikeSymbol> substitute(
        signature: KtVariableLikeSignature<S>,
        substitutor: KtSubstitutor
    ): KtVariableLikeSignature<S> {
        if (substitutor is KtSubstitutor.Empty) return signature
        return KtVariableLikeSignature(
            signature.symbol,
            substitutor.substitute(signature.returnType),
            signature.receiverType?.let { substitutor.substitute(it) },
        )
    }

    override fun <S : KtFunctionLikeSymbol> substitute(
        signature: KtFunctionLikeSignature<S>,
        substitutor: KtSubstitutor
    ): KtFunctionLikeSignature<S> {
        if (substitutor is KtSubstitutor.Empty) return signature
        return KtFunctionLikeSignature(
            signature.symbol,
            substitutor.substitute(signature.returnType),
            signature.receiverType?.let { substitutor.substitute(it) },
            signature.valueParameters.map { substitute(it, substitutor) }
        )
    }

    override fun <S : KtFunctionLikeSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtFunctionLikeSignature<S> {
        if (substitutor is KtSubstitutor.Empty) return asSignature(symbol)
        return KtFunctionLikeSignature(
            symbol,
            substitutor.substitute(symbol.returnType),
            symbol.receiverType?.let { substitutor.substitute(it) },
            symbol.valueParameters.map { substitute(it, substitutor) }
        )
    }

    override fun <S : KtVariableLikeSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtVariableLikeSignature<S> {
        if (substitutor is KtSubstitutor.Empty) return asSignature(symbol)
        return KtVariableLikeSignature(
            symbol,
            substitutor.substitute(symbol.returnType),
            symbol.receiverType?.let { substitutor.substitute(it) },
        )
    }

    override fun <S : KtCallableSymbol> asSignature(symbol: S): KtCallableSignature<S> {
        return when (symbol) {
            is KtFunctionLikeSymbol -> asSignature(symbol)
            is KtVariableLikeSymbol -> asSignature(symbol)
            else -> unexpectedElementError("symbol", symbol)
        }
    }

    override fun <S : KtFunctionLikeSymbol> asSignature(symbol: S): KtFunctionLikeSignature<S> {
        return KtFunctionLikeSignature(symbol, symbol.returnType, symbol.receiverType, symbol.valueParameters.map { asSignature(it) })
    }

    override fun <S : KtVariableLikeSymbol> asSignature(symbol: S): KtVariableLikeSignature<S> {
        return KtVariableLikeSignature(symbol, symbol.returnType, symbol.receiverType)
    }
}