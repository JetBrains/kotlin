/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KaFe10FunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KaFe10VariableLikeSignature
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAbstractSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType

internal class KaFe10SignatureSubstitutor(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaAbstractSignatureSubstitutor<KaFe10Session>(), KaFe10SessionComponent {
    override fun <S : KaFunctionLikeSymbol> S.asSignature(): KaFunctionLikeSignature<S> = withValidityAssertion {
        return KaFe10FunctionLikeSignature(this, returnType, receiverType, valueParameters.map { it.asSignature() })
    }

    override fun <S : KaVariableLikeSymbol> S.asSignature(): KaVariableLikeSignature<S> = withValidityAssertion {
        return KaFe10VariableLikeSignature(this, returnType, receiverType)
    }
}