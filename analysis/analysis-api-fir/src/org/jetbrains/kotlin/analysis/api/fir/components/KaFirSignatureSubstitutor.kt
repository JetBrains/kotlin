/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.signatures.KaFirFunctionDummySignature
import org.jetbrains.kotlin.analysis.api.fir.signatures.KaFirVariableDummySignature
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAbstractSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

internal class KaFirSignatureSubstitutor(
    override val analysisSessionProvider: () -> KaFirSession
) : KaAbstractSignatureSubstitutor<KaFirSession>(), KaFirSessionComponent {
    override fun <S : KaFunctionSymbol> S.asSignature(): KaFunctionSignature<S> = withValidityAssertion {
        val firSymbol = (this as KaFirSymbol<*>).firSymbol as FirFunctionSymbol<*>
        return KaFirFunctionDummySignature<S>(analysisSession.token, firSymbol, analysisSession.firSymbolBuilder)
    }

    override fun <S : KaVariableSymbol> S.asSignature(): KaVariableSignature<S> = withValidityAssertion {
        val firSymbol = (this as KaFirSymbol<*>).firSymbol as FirVariableSymbol<*>
        return KaFirVariableDummySignature<S>(analysisSession.token, firSymbol, analysisSession.firSymbolBuilder)
    }
}