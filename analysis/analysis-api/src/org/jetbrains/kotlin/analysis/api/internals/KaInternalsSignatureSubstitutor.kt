/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsSignatureSubstitutor {
    @KaExperimentalApi
    public fun <S : KaCallableSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaCallableSignature<S>

    @KaExperimentalApi
    public fun <S : KaFunctionSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaFunctionSignature<S>

    @KaExperimentalApi
    public fun <S : KaVariableSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaVariableSignature<S>

    @KaExperimentalApi
    public fun <S : KaCallableSymbol> asSignature(symbol: S): KaCallableSignature<S>

    @KaExperimentalApi
    public fun <S : KaFunctionSymbol> asSignature(symbol: S): KaFunctionSignature<S>

    @KaExperimentalApi
    public fun <S : KaVariableSymbol> asSignature(symbol: S): KaVariableSignature<S>
}
