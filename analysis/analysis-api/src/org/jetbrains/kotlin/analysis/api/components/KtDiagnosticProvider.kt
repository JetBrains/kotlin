/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtDiagnosticProvider : KtAnalysisSessionComponent() {
    public abstract fun getDiagnosticsForElement(element: KtElement, filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>>
    public abstract fun collectDiagnosticsForFile(ktFile: KtFile, filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>>
}

public interface KtDiagnosticProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtElement.getDiagnostics(filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>> =
        withValidityAssertion { analysisSession.diagnosticProvider.getDiagnosticsForElement(this, filter) }

    public fun KtFile.collectDiagnosticsForFile(filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>> =
        withValidityAssertion { analysisSession.diagnosticProvider.collectDiagnosticsForFile(this, filter) }
}

public enum class KtDiagnosticCheckerFilter {
    ONLY_COMMON_CHECKERS,
    ONLY_EXTENDED_CHECKERS,
    EXTENDED_AND_COMMON_CHECKERS,
}