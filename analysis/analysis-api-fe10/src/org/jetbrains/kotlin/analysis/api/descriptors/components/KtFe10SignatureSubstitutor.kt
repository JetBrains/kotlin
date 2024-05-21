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
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType

internal class KaFe10SignatureSubstitutor(
    override val analysisSession: KaFe10Session
) : KaAbstractSignatureSubstitutor(), KaFe10SessionComponent {
    override fun <S : KaFunctionLikeSymbol> asSignature(symbol: S): KaFunctionLikeSignature<S> {
        return KaFe10FunctionLikeSignature(symbol, symbol.returnType, symbol.receiverType, symbol.valueParameters.map { asSignature(it) })
    }

    override fun <S : KaVariableLikeSymbol> asSignature(symbol: S): KaVariableLikeSignature<S> {
        return KaFe10VariableLikeSignature(symbol, symbol.returnType, symbol.receiverType)
    }
}