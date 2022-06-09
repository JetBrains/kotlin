/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures


import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType

/**
 * A signature of a function-like symbol. This includes functions, getters, setters, lambdas, etc.
 */
public class KtFunctionLikeSignature<out S : KtFunctionLikeSymbol>(
    private val _symbol: S,
    private val _returnType: KtType,
    private val _receiverType: KtType?,
    private val _valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtCallableSignature<S>() {
    override val token: KtLifetimeToken
        get() = _symbol.token
    override val symbol: S
        get() = withValidityAssertion { _symbol }
    override val returnType: KtType
        get() = withValidityAssertion { _returnType }
    override val receiverType: KtType?
        get() = withValidityAssertion { _receiverType }

    /**
     * The use-site-substituted value parameters.
     */
    public val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>
        get() = withValidityAssertion { _valueParameters }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtFunctionLikeSignature<*>

        if (_symbol != other._symbol) return false
        if (_returnType != other._returnType) return false
        if (_receiverType != other._receiverType) return false
        if (_valueParameters != other._valueParameters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _symbol.hashCode()
        result = 31 * result + _returnType.hashCode()
        result = 31 * result + (_receiverType?.hashCode() ?: 0)
        result = 31 * result + _valueParameters.hashCode()
        return result
    }
}

