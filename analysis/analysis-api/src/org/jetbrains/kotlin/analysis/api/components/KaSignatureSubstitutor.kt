/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
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
    public fun <S : KaFunctionSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionSignature<S>

    /**
     * Applies a [substitutor] to the given symbols and return a signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    @KaExperimentalApi
    public fun <S : KaVariableSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableSignature<S>

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    @KaExperimentalApi
    public fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S>

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    @KaExperimentalApi
    public fun <S : KaFunctionSymbol> S.asSignature(): KaFunctionSignature<S>

    /**
     * Creates a new [KaCallableSignature] by given symbol and leave all types intact
     */
    @KaExperimentalApi
    public fun <S : KaVariableSymbol> S.asSignature(): KaVariableSignature<S>
}