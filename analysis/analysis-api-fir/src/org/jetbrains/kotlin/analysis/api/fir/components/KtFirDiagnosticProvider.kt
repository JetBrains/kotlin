/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getDiagnostics
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KaFirDiagnosticProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaDiagnosticProvider(), KaFirSessionComponent {

    override fun getDiagnosticsForElement(
        element: KtElement,
        filter: KaDiagnosticCheckerFilter
    ): Collection<KaDiagnosticWithPsi<*>> {
        return element.getDiagnostics(firResolveSession, filter.asLLFilter()).map { it.asKtDiagnostic() }
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        ktFile.collectDiagnosticsForFile(firResolveSession, filter.asLLFilter()).map { it.asKtDiagnostic() }


    private fun KaDiagnosticCheckerFilter.asLLFilter() = when (this) {
        KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS
        KaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS
        KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS -> DiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS
    }
}