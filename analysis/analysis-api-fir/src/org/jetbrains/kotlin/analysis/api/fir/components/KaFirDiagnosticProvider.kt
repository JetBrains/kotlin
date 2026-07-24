/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.diagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getDiagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.plus
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KaFirDiagnosticProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaInternalsDiagnosticProvider, KaFirSessionComponent {
    override fun directDiagnostics(
        element: KtElement,
        filter: KaDiagnosticCheckerFilter,
    ): Collection<KaDiagnosticWithPsi<*>> = element.withPsiValidityAssertion {
        element.getDiagnostics(resolutionFacade, filter.asLLFilter()).map { it.asKaDiagnostic() }
    }

    override fun collectDiagnostics(
        file: KtFile,
        filter: KaDiagnosticCheckerFilter,
    ): Collection<KaDiagnosticWithPsi<*>> = file.withPsiValidityAssertion {
        diagnostics(file, filter).toList()
    }

    override fun diagnostics(
        file: KtFile,
        filter: KaDiagnosticCheckerFilter,
    ): Sequence<KaDiagnosticWithPsi<*>> = file.withPsiValidityAssertion {
        file.diagnostics(resolutionFacade, filter.asLLFilter()).map { it.asKaDiagnostic() }
    }

    private fun KaDiagnosticCheckerFilter.asLLFilter() = when (this) {
        KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS
        KaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
        KaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS
        KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
    }
}
