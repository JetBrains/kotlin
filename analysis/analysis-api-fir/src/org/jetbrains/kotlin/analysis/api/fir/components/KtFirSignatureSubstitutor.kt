/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.signatures.KtFirFunctionLikeDummySignature
import org.jetbrains.kotlin.analysis.api.fir.signatures.KtFirVariableLikeDummySignature
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

internal class KtFirSignatureSubstitutor(
    override val analysisSession: KtFirAnalysisSession
) : AbstractKtSignatureSubstitutor(), KtFirAnalysisSessionComponent {
    override fun <S : KtFunctionLikeSymbol> asSignature(symbol: S): KtFunctionLikeSignature<S> {
        val firSymbol = (symbol as KtFirSymbol<*>).firSymbol as FirFunctionSymbol<*>
        return KtFirFunctionLikeDummySignature<S>(analysisSession.token, firSymbol, analysisSession.firSymbolBuilder)
    }

    override fun <S : KtVariableLikeSymbol> asSignature(symbol: S): KtVariableLikeSignature<S> {
        val firSymbol = (symbol as KtFirSymbol<*>).firSymbol as FirVariableSymbol<*>
        return KtFirVariableLikeDummySignature<S>(analysisSession.token, firSymbol, analysisSession.firSymbolBuilder)
    }
}