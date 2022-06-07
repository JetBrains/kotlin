/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor

public abstract class KtSignatureSubsitutor : KtAnalysisSessionComponent() {
    public abstract fun <S : KtCallableSymbol> substitute(
        signature: KtCallableSignature<S>,
        substitutor: KtSubstitutor
    ): KtCallableSignature<S>

    public abstract fun <S : KtFunctionLikeSymbol> substitute(
        signature: KtFunctionLikeSignature<S>,
        substitutor: KtSubstitutor
    ): KtFunctionLikeSignature<S>

    public abstract fun <S : KtVariableLikeSymbol> substitute(
        signature: KtVariableLikeSignature<S>,
        substitutor: KtSubstitutor
    ): KtVariableLikeSignature<S>

    public abstract fun <S : KtCallableSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtCallableSignature<S>

    public abstract fun <S : KtFunctionLikeSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtFunctionLikeSignature<S>

    public abstract fun <S : KtVariableLikeSymbol> substitute(symbol: S, substitutor: KtSubstitutor): KtVariableLikeSignature<S>

    public abstract fun <S : KtCallableSymbol> asSignature(symbol: S): KtCallableSignature<S>

    public abstract fun <S : KtFunctionLikeSymbol> asSignature(symbol: S): KtFunctionLikeSignature<S>

    public abstract fun <S : KtVariableLikeSymbol> asSignature(symbol: S): KtVariableLikeSignature<S>
}

public interface KtSignatureSubsitutorMixIn : KtAnalysisSessionMixIn {
    /**
     * Applies a [substitutor] to the given signature and return a new signature with substituted types.
     *
     * @see KtSubstitutor.substituteOrSelf
     */
    public fun <S : KtCallableSymbol> KtCallableSignature<S>.substitute(substitutor: KtSubstitutor): KtCallableSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.substitute(this, substitutor) }

    /**
     * Applies a [substitutor]  to the given signature and return a new signature with substituted types.
     *
     * @see KtSubstitutor.substituteOrSelf
     */
    public fun <S : KtFunctionLikeSymbol> KtFunctionLikeSignature<S>.substitute(substitutor: KtSubstitutor): KtFunctionLikeSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.substitute(this, substitutor) }

    /**
     * Applies a [substitutor]  to the given signature and return a new signature with substituted types.
     *
     * @see KtSubstitutor.substituteOrSelf
     */
    public fun <S : KtVariableLikeSymbol> KtVariableLikeSignature<S>.substitute(substitutor: KtSubstitutor): KtVariableLikeSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given symbols and return a signature with substituted types.
     *
     * @see KtSubstitutor.substituteOrSelf
     */
    public fun <S : KtCallableSymbol> S.substitute(substitutor: KtSubstitutor): KtCallableSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given symbols and return a signature with substituted types.
     *
     * @see KtSubstitutor.substituteOrSelf
     */
    public fun <S : KtFunctionSymbol> S.substitute(substitutor: KtSubstitutor): KtFunctionLikeSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.substitute(this, substitutor) }

    /**
     * Applies a [substitutor] to the given symbols and return a signature with substituted types.
     *
     * @see KtSubstitutor.substituteOrSelf
     */
    public fun <S : KtVariableLikeSymbol> S.substitute(substitutor: KtSubstitutor): KtVariableLikeSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.substitute(this, substitutor) }

    /**
     * Creates a new [KtCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KtCallableSymbol> S.asSignature(): KtCallableSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.asSignature(this) }

    /**
     * Creates a new [KtCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KtFunctionLikeSymbol> S.asSignature(): KtFunctionLikeSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.asSignature(this) }

    /**
     * Creates a new [KtCallableSignature] by given symbol and leave all types intact
     */
    public fun <S : KtVariableLikeSymbol> S.asSignature(): KtVariableLikeSignature<S> =
        withValidityAssertion { analysisSession.substitutionProvider.asSignature(this) }
}
