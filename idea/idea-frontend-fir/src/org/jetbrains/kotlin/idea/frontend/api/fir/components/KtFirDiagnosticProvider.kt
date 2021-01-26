/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getDiagnostics
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtDiagnosticProvider
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtSimpleDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.idea.highlighter.IdeErrorMessages
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirDiagnosticProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtDiagnosticProvider(), KtFirAnalysisSessionComponent {
    override fun getDiagnosticsForElement(element: KtElement): Collection<KtDiagnostic> = withValidityAssertion {
        element.getDiagnostics(firResolveState).map { it.asKtDiagnostic() }
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile): Collection<KtDiagnostic> =
        ktFile.collectDiagnosticsForFile(firResolveState).map { it.asKtDiagnostic() }

    fun firDiagnosticAsKtDiagnostic(diagnostic: FirPsiDiagnostic<*>): KtDiagnostic {
        val message = IdeErrorMessages.render(diagnostic)
        return KtSimpleDiagnostic(diagnostic.factory.name,  message, diagnostic.textRanges)
    }

    fun coneDiagnosticAsKtDiagnostic(coneDiagnostic: ConeDiagnostic, source: FirSourceElement): KtDiagnostic? {
        val firDiagnostic = coneDiagnostic.toFirDiagnostic(source) ?: return null
        check(firDiagnostic is FirPsiDiagnostic<*>)
        return firDiagnosticAsKtDiagnostic(firDiagnostic)
    }
}