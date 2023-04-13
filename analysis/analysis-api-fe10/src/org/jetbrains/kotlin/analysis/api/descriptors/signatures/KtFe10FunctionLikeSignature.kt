/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.signatures

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType

internal class KtFe10FunctionLikeSignature<out S : KtFunctionLikeSymbol>(
    private val _symbol: S,
    private val _returnType: KtType,
    private val _receiverType: KtType?,
    private val _valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtFunctionLikeSignature<S>() {
    override val token: KtLifetimeToken
        get() = _symbol.token
    override val symbol: S
        get() = withValidityAssertion { _symbol }
    override val returnType: KtType
        get() = withValidityAssertion { _returnType }
    override val receiverType: KtType?
        get() = withValidityAssertion { _receiverType }
    override val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>
        get() = withValidityAssertion { _valueParameters }

    override fun substitute(substitutor: KtSubstitutor): KtFunctionLikeSignature<S> = withValidityAssertion {
        KtFe10FunctionLikeSignature(
            symbol,
            substitutor.substitute(returnType),
            receiverType?.let { substitutor.substitute(it) },
            valueParameters.map { valueParameter ->
                KtFe10VariableLikeSignature<KtValueParameterSymbol>(
                    valueParameter.symbol,
                    substitutor.substitute(valueParameter.returnType),
                    valueParameter.receiverType?.let { substitutor.substitute(it) }
                )
            }
        )
    }

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
