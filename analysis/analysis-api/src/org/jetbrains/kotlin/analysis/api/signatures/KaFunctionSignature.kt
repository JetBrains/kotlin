/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor

/**
 * A signature of a function-like symbol. This includes functions, getters, setters, lambdas, etc.
 */
public abstract class KaFunctionSignature<out S : KaFunctionSymbol> : KaCallableSignature<S>() {
    /**
     * The use-site-substituted value parameters.
     */
    public abstract val valueParameters: List<KaVariableSignature<KaValueParameterSymbol>>

    @KaExperimentalApi
    abstract override fun substitute(substitutor: KaSubstitutor): KaFunctionSignature<S>
}

@Deprecated("Use 'KaFunctionSignature' instead", ReplaceWith("KaFunctionSignature"))
public typealias KaFunctionLikeSignature<S> = KaFunctionSignature<S>

@Deprecated("Use 'KaFunctionSignature' instead", ReplaceWith("KaFunctionSignature"))
public typealias KtFunctionLikeSignature<S> = KaFunctionSignature<S>