/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor

/**
 * A signature of a function-like symbol. This includes functions, getters, setters, lambdas, etc.
 */
public abstract class KaFunctionLikeSignature<out S : KaFunctionLikeSymbol> : KaCallableSignature<S>() {
    /**
     * The use-site-substituted value parameters.
     */
    public abstract val valueParameters: List<KaVariableLikeSignature<KaValueParameterSymbol>>

    @KaExperimentalApi
    abstract override fun substitute(substitutor: KaSubstitutor): KaFunctionLikeSignature<S>
}

@Deprecated("Use 'KaFunctionLikeSignature' instead", ReplaceWith("KaFunctionLikeSignature"))
public typealias KtFunctionLikeSignature<S> = KaFunctionLikeSignature<S>