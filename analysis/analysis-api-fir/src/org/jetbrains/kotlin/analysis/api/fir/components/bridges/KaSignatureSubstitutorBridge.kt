/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.asSignature as asSignatureEndpoint
import org.jetbrains.kotlin.analysis.api.signatures.substitute as substituteEndpoint

@OptIn(KaExperimentalApi::class)
internal class KaSignatureSubstitutorBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaSignatureSubstitutor {
    override fun <S : KaCallableSymbol> S.substitute(substitutor: KaSubstitutor): KaCallableSignature<S> =
        context(analysisSession) { substituteEndpoint(substitutor) }

    override fun <S : KaFunctionSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionSignature<S> =
        context(analysisSession) { substituteEndpoint(substitutor) }

    override fun <S : KaVariableSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableSignature<S> =
        context(analysisSession) { substituteEndpoint(substitutor) }

    override fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S> =
        context(analysisSession) { asSignatureEndpoint() }

    override fun <S : KaFunctionSymbol> S.asSignature(): KaFunctionSignature<S> =
        context(analysisSession) { asSignatureEndpoint() }

    override fun <S : KaVariableSymbol> S.asSignature(): KaVariableSignature<S> =
        context(analysisSession) { asSignatureEndpoint() }
}
