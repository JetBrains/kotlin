/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.signatures

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType

internal class KtFe10VariableLikeSignature<out S : KtVariableLikeSymbol>(
    symbol: S,
    private val _returnType: KtType,
    private val _receiverType: KtType?,
) : KtVariableLikeSignature<S>(symbol) {
    override val returnType: KtType
        get() = withValidityAssertion { _returnType }
    override val receiverType: KtType?
        get() = withValidityAssertion { _receiverType }

    override fun substitute(substitutor: KtSubstitutor): KtVariableLikeSignature<S> = KtFe10VariableLikeSignature(
        symbol,
        substitutor.substitute(returnType),
        receiverType?.let { substitutor.substitute(it) },
    )
}