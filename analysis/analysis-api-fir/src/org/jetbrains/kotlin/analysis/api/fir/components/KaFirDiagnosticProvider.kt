/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticCheckerKind
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.diagnostics.KaFirDiagnostics
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsDiagnosticProvider
import org.jetbrains.kotlin.psi.KtElement

internal class KaFirDiagnosticProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaFirSessionComponent(), KaInternalsDiagnosticProvider {
    override fun diagnostics(element: KtElement): KaDiagnostics = entryPointImplementation(element) {
        KaFirDiagnostics(
            element = element,
            directOnly = false,
            checkerKinds = DEFAULT_CHECKER_KINDS,
            includingSuppressed = false,
            analysisSession = analysisSession,
        )
    }

    private companion object {
        private val DEFAULT_CHECKER_KINDS = setOf(KaDiagnosticCheckerKind.COMMON)
    }
}
