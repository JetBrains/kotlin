/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.signatures.KtFirFunctionLikeSymbolBasedSignature
import org.jetbrains.kotlin.analysis.api.fir.signatures.KtFirVariableLikeSymbolBasedSignature
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol

internal class KtFirSignatureSubstitutor(
    override val analysisSession: KtFirAnalysisSession
) : AbstractKtSignatureSubstitutor(), KtFirAnalysisSessionComponent {
    override fun <S : KtFunctionLikeSymbol> asSignature(symbol: S): KtFunctionLikeSignature<S> =
        KtFirFunctionLikeSymbolBasedSignature<S>(symbol)

    override fun <S : KtVariableLikeSymbol> asSignature(symbol: S): KtVariableLikeSignature<S> =
        KtFirVariableLikeSymbolBasedSignature<S>(symbol)
}