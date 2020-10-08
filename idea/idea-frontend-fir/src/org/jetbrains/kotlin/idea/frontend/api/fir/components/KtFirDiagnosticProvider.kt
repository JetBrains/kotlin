/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtDiagnosticProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirDiagnosticProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtDiagnosticProvider(), KtFirAnalysisSessionComponent {
    override fun getDiagnosticsForElement(element: KtElement): Collection<Diagnostic> = withValidityAssertion {
        LowLevelFirApiFacade.getDiagnosticsFor(element, firResolveState)
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile): Collection<Diagnostic> =
        LowLevelFirApiFacade.collectDiagnosticsForFile(ktFile, firResolveState)
}