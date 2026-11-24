/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.asSignature as asSignatureEndpoint
import org.jetbrains.kotlin.analysis.api.signatures.substitute as substituteEndpoint

internal class KaSignatureSubstitutorBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaFirSessionComponent(), KaSignatureSubstitutor {
    override fun <S : KaCallableSymbol> S.substitute(substitutor: KaSubstitutor): KaCallableSignature<S> =
        substituteEndpoint(substitutor, session = analysisSession)

    override fun <S : KaFunctionSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionSignature<S> =
        substituteEndpoint(substitutor, session = analysisSession)

    override fun <S : KaVariableSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableSignature<S> =
        substituteEndpoint(substitutor, session = analysisSession)

    override fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S> =
        asSignatureEndpoint(session = analysisSession)

    override fun <S : KaFunctionSymbol> S.asSignature(): KaFunctionSignature<S> =
        asSignatureEndpoint(session = analysisSession)

    override fun <S : KaVariableSymbol> S.asSignature(): KaVariableSignature<S> =
        asSignatureEndpoint(session = analysisSession)
}
