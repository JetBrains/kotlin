/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getDiagnostics
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtDiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.frontend.api.components.KtDiagnosticProvider
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirDiagnosticProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtDiagnosticProvider(), KtFirAnalysisSessionComponent {
    override fun getDiagnosticsForElement(
        element: KtElement,
        filter: KtDiagnosticCheckerFilter
    ): Collection<KtDiagnosticWithPsi<*>> = withValidityAssertion {
        element.getDiagnostics(firResolveState, filter.asLLFilter()).map { it.asKtDiagnostic() }
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>> =
        ktFile.collectDiagnosticsForFile(firResolveState, filter.asLLFilter()).map { it.asKtDiagnostic() }


    private fun KtDiagnosticCheckerFilter.asLLFilter() = when (this) {
        KtDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS
        KtDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS
        KtDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS -> DiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS
    }
}