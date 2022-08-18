/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

public abstract class KtSignatureSubstitutor : KtAnalysisSessionComponent() {
    @Suppress("UNCHECKED_CAST")
    public open fun <S : KtCallableSymbol> substitute(
        signature: KtCallableSignature<S>,
        substitutor: KtSubstitutor
    ): KtCallableSignature<S> {
        return when (signature) {
            is KtFunctionLikeSignature -> {
                substitute(signature as KtFunctionLikeSignature<KtFunctionLikeSymbol>, substitutor) as KtCallableSignature<S>
            }
            is KtVariableLikeSignature -> {
                substitute(signature as KtVariableLikeSignature<KtVariableLikeSymbol>, substitutor) as KtCallableSignature<S>
            }
        }
    }

    public abstract fun <S : KtFunctionLikeSymbol> substitute(
        signature: KtFunctionLikeSignature<S>,
        substitutor: KtSubstitutor
    ): KtFunctionLikeSignature<S>

    public abstract fun <S : KtVariableLikeSymbol> substitute(
        signature: KtVariableLikeSignature<S>,
        substitutor: KtSubstitutor
    ): KtVariableLikeSignature<S>

    public open fun <S : KtCallableSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtCallableSignature<S> = when (symbol) {
        is KtFunctionLikeSymbol -> substitute(symbol, substitutor)
        is KtVariableLikeSymbol -> substitute(symbol, substitutor)
        else -> unexpectedElementError("symbol", symbol)
    }

    public abstract fun <S : KtFunctionLikeSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtFunctionLikeSignature<S>

    public abstract fun <S : KtVariableLikeSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtVariableLikeSignature<S>

    public open fun <S : KtCallableSymbol> asSignature(symbol: S): KtCallableSignature<S> =
        substitute(symbol, KtSubstitutor.Empty(token))

    public open fun <S : KtFunctionLikeSymbol> asSignature(symbol: S): KtFunctionLikeSignature<S> =
        substitute(symbol, KtSubstitutor.Empty(token))

    public open fun <S : KtVariableLikeSymbol> asSignature(symbol: S): KtVariableLikeSignature<S> =
        substitute(symbol, KtSubstitutor.Empty(token))
}

public interface KtSignatureSubstitutorMixIn : KtAnalysisSessionMixIn {
    /**
     * Applies a [substitutor] to the given signature and return a new signature with substituted types.
     *
     * @see KtSubstitutor.substitute
     */
    public fun <S : KtCallableSymbol> KtCallableSignature<S>.substitute(substitutor: KtSubstitutor): KtCallableSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given signature and return a new signature with substituted types.
     *
     * @see KtSubstitutor.substitute
     */
    public fun <S : KtFunctionLikeSymbol> KtFunctionLikeSignature<S>.substitute(substitutor: KtSubstitutor): KtFunctionLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given signature and return a new signature with substituted types.
     *
     * @see KtSubstitutor.substitute
     */
    public fun <S : KtVariableLikeSymbol> KtVariableLikeSignature<S>.substitute(substitutor: KtSubstitutor): KtVariableLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given symbol and return a signature with substituted types.
     *
     * @see KtSubstitutor.substitute
     */
    public fun <S : KtCallableSymbol> S.substitute(substitutor: KtSubstitutor): KtCallableSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given symbol and return a signature with substituted types.
     *
     * @see KtSubstitutor.substitute
     */
    public fun <S : KtFunctionLikeSymbol> S.substitute(substitutor: KtSubstitutor): KtFunctionLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given symbols and return a signature with substituted types.
     *
     * @see KtSubstitutor.substitute
     */
    public fun <S : KtVariableLikeSymbol> S.substitute(substitutor: KtSubstitutor): KtVariableLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.substitute(this, substitutor) }

    /**
     * Creates a new [KtCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KtCallableSymbol> S.asSignature(): KtCallableSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.asSignature(this) }

    /**
     * Creates a new [KtCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KtFunctionLikeSymbol> S.asSignature(): KtFunctionLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.asSignature(this) }

    /**
     * Creates a new [KtCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KtVariableLikeSymbol> S.asSignature(): KtVariableLikeSignature<S> =
        withValidityAssertion { analysisSession.signatureSubstitutor.asSignature(this) }
}
