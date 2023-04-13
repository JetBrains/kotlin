/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor

/**
 * A signature of a function-like symbol. This includes functions, getters, setters, lambdas, etc.
 */
public abstract class KtFunctionLikeSignature<out S : KtFunctionLikeSymbol> : KtCallableSignature<S>() {
    /**
     * The use-site-substituted value parameters.
     */
    public abstract val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>

    abstract override fun substitute(substitutor: KtSubstitutor): KtFunctionLikeSignature<S>
}

