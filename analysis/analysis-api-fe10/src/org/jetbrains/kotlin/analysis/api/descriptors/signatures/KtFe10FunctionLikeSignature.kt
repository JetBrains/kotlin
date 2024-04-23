/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    private val backingSymbol: S,
    private val backingReturnType: KtType,
    private val backingReceiverType: KtType?,
    private val backingValueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtFunctionLikeSignature<S>() {
    override val token: KtLifetimeToken get() = backingSymbol.token
    override val symbol: S get() = withValidityAssertion { backingSymbol }
    override val returnType: KtType get() = withValidityAssertion { backingReturnType }
    override val receiverType: KtType? get() = withValidityAssertion { backingReceiverType }
    override val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>> get() = withValidityAssertion { backingValueParameters }

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

        other as KtFe10FunctionLikeSignature<*>

        if (backingSymbol != other.backingSymbol) return false
        if (backingReturnType != other.backingReturnType) return false
        if (backingReceiverType != other.backingReceiverType) return false
        if (backingValueParameters != other.backingValueParameters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backingSymbol.hashCode()
        result = 31 * result + backingReturnType.hashCode()
        result = 31 * result + (backingReceiverType?.hashCode() ?: 0)
        result = 31 * result + backingValueParameters.hashCode()
        return result
    }
}
