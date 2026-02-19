/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.signatures

import org.jetbrains.kotlin.analysis.api.impl.base.signatures.KaBaseVariableSignature
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import java.util.*

internal class KaFe10VariableSignature<out S : KaVariableSymbol>(
    private val backingSymbol: S,
    private val backingReturnType: KaType,
    private val backingReceiverType: KaType?,
    private val backingContextParameters: List<KaVariableSignature<KaContextParameterSymbol>>,
) : KaBaseVariableSignature<S>() {
    override val token: KaLifetimeToken get() = backingSymbol.token
    override val symbol: S get() = withValidityAssertion { backingSymbol }
    override val returnType: KaType get() = withValidityAssertion { backingReturnType }
    override val receiverType: KaType? get() = withValidityAssertion { backingReceiverType }
    override val contextParameters: List<KaVariableSignature<KaContextParameterSymbol>> get() = withValidityAssertion { backingContextParameters }

    override fun substitute(substitutor: KaSubstitutor): KaVariableSignature<S> = withValidityAssertion {
        KaFe10VariableSignature(
            backingSymbol = symbol,
            backingReturnType = substitutor.substitute(returnType),
            backingReceiverType = receiverType?.let(substitutor::substitute),
            backingContextParameters = contextParameters.map { contextParameter ->
                KaFe10VariableSignature(
                    backingSymbol = contextParameter.symbol,
                    backingReturnType = substitutor.substitute(contextParameter.returnType),
                    backingReceiverType = contextParameter.receiverType?.let(substitutor::substitute),
                    backingContextParameters = emptyList(),
                )
            }
        )
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is KaFe10VariableSignature<*> &&
            other.backingSymbol == backingSymbol &&
            other.backingReturnType == backingReturnType &&
            other.backingReceiverType == backingReceiverType &&
            other.backingContextParameters == backingContextParameters

    override fun hashCode(): Int = Objects.hash(
        backingSymbol,
        backingReturnType,
        backingReceiverType,
        backingContextParameters,
    )
}
