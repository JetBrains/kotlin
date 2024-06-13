/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor

@KaExperimentalApi
public interface KaSignatureSubstitutor {
    /**
     * Applies a [substitutor] to the given symbol and return a signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    @KaExperimentalApi
    public fun <S : KaCallableSymbol> S.substitute(substitutor: KaSubstitutor): KaCallableSignature<S>

    /**
     * Applies a [substitutor] to the given symbol and return a signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    @KaExperimentalApi
    public fun <S : KaFunctionLikeSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionLikeSignature<S>

    /**
     * Applies a [substitutor] to the given symbols and return a signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    @KaExperimentalApi
    public fun <S : KaVariableLikeSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableLikeSignature<S>

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    @KaExperimentalApi
    public fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S>

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    @KaExperimentalApi
    public fun <S : KaFunctionLikeSymbol> S.asSignature(): KaFunctionLikeSignature<S>

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    @KaExperimentalApi
    public fun <S : KaVariableLikeSymbol> S.asSignature(): KaVariableLikeSignature<S>
}