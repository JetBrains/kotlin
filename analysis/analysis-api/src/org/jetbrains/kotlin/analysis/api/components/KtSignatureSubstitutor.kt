/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

public abstract class KaSignatureSubstitutor : KaSessionComponent() {
    public open fun <S : KaCallableSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaCallableSignature<S> = when (symbol) {
        is KaFunctionLikeSymbol -> substitute(symbol, substitutor)
        is KaVariableLikeSymbol -> substitute(symbol, substitutor)
        else -> unexpectedElementError("symbol", symbol)
    }

    public abstract fun <S : KaFunctionLikeSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaFunctionLikeSignature<S>

    public abstract fun <S : KaVariableLikeSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaVariableLikeSignature<S>

    public abstract fun <S : KaCallableSymbol> asSignature(symbol: S): KaCallableSignature<S>

    public abstract fun <S : KaFunctionLikeSymbol> asSignature(symbol: S): KaFunctionLikeSignature<S>

    public abstract fun <S : KaVariableLikeSymbol> asSignature(symbol: S): KaVariableLikeSignature<S>
}

public typealias KtSignatureSubstitutor = KaSignatureSubstitutor

public interface KaSignatureSubstitutorMixIn : KaSessionMixIn {
    /**
     * Applies a [substitutor] to the given symbol and return a signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    public fun <S : KaCallableSymbol> S.substitute(substitutor: KaSubstitutor): KaCallableSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given symbol and return a signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    public fun <S : KaFunctionLikeSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given symbols and return a signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    public fun <S : KaVariableLikeSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.asSignature(this) }

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KaFunctionLikeSymbol> S.asSignature(): KaFunctionLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.asSignature(this) }

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KaVariableLikeSymbol> S.asSignature(): KaVariableLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.asSignature(this) }
}

public typealias KtSignatureSubstitutorMixIn = KaSignatureSubstitutorMixIn