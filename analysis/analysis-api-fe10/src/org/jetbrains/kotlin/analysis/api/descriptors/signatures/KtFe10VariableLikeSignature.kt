/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.signatures

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType

internal class KtFe10VariableLikeSignature<out S : KtVariableLikeSymbol>(
    private val backingSymbol: S,
    private val backingReturnType: KtType,
    private val backingReceiverType: KtType?,
) : KtVariableLikeSignature<S>() {
    override val token: KtLifetimeToken get() = backingSymbol.token
    override val symbol: S get() = withValidityAssertion { backingSymbol }
    override val returnType: KtType get() = withValidityAssertion { backingReturnType }
    override val receiverType: KtType? get() = withValidityAssertion { backingReceiverType }

    override fun substitute(substitutor: KtSubstitutor): KtVariableLikeSignature<S> = withValidityAssertion {
        KtFe10VariableLikeSignature(
            symbol,
            substitutor.substitute(returnType),
            receiverType?.let { substitutor.substitute(it) },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtFe10VariableLikeSignature<*>

        if (backingSymbol != other.backingSymbol) return false
        if (backingReturnType != other.backingReturnType) return false
        if (backingReceiverType != other.backingReceiverType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backingSymbol.hashCode()
        result = 31 * result + backingReturnType.hashCode()
        result = 31 * result + (backingReceiverType?.hashCode() ?: 0)
        return result
    }
}