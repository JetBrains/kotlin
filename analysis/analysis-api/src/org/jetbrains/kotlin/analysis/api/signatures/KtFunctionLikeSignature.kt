/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType

/**
 * A signature of a function-like symbol. This includes functions, getters, setters, lambdas, etc.
 */
public abstract class KtFunctionLikeSignature<out S : KtFunctionLikeSymbol> : KtCallableSignature<S>() {
    /**
     * The use-site-substituted value parameters.
     */
    public abstract val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>

    abstract override fun substitute(substitutor: KtSubstitutor): KtFunctionLikeSignature<S>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtFunctionLikeSignature<*>

        if (symbol != other.symbol) return false
        if (returnType != other.returnType) return false
        if (receiverType != other.receiverType) return false
        if (valueParameters != other.valueParameters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = symbol.hashCode()
        result = 31 * result + returnType.hashCode()
        result = 31 * result + (receiverType?.hashCode() ?: 0)
        result = 31 * result + valueParameters.hashCode()
        return result
    }
}

