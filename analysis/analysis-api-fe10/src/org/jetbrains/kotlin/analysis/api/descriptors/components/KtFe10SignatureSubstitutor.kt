/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KtFe10FunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KtFe10VariableLikeSignature
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType

internal class KtFe10SignatureSubstitutor(
    override val analysisSession: KtFe10AnalysisSession
) : AbstractKtSignatureSubstitutor(), Fe10KtAnalysisSessionComponent {
    override fun <S : KtFunctionLikeSymbol> asSignature(symbol: S): KtFunctionLikeSignature<S> {
        return KtFe10FunctionLikeSignature(symbol, symbol.returnType, symbol.receiverType, symbol.valueParameters.map { asSignature(it) })
    }

    override fun <S : KtVariableLikeSymbol> asSignature(symbol: S): KtVariableLikeSignature<S> {
        return KtFe10VariableLikeSignature(symbol, symbol.returnType, symbol.receiverType)
    }
}