/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionError
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

@KaAnalysisApiInternals
class KaSymbolResolutionErrorImpl(
    diagnostic: KaDiagnostic,
    candidateSymbols: List<KaSymbol>,
) : KaSymbolResolutionError {
    private val backingDiagnostic: KaDiagnostic = diagnostic

    override val token: KaLifetimeToken get() = backingDiagnostic.token
    override val diagnostic: KaDiagnostic get() = withValidityAssertion { backingDiagnostic }
    override val candidateSymbols: List<KaSymbol> by validityAsserted(candidateSymbols)
}
