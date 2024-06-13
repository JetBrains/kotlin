/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public abstract class KaDiagnosticProvider : KaSessionComponent() {
    public abstract fun getDiagnosticsForElement(element: KtElement, filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>
    public abstract fun collectDiagnosticsForFile(ktFile: KtFile, filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>
}

public typealias KtDiagnosticProvider = KaDiagnosticProvider

public interface KaDiagnosticProviderMixIn : KaSessionMixIn {
    public fun KtElement.getDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        withValidityAssertion { analysisSession.diagnosticProvider.getDiagnosticsForElement(this, filter) }

    public fun KtFile.collectDiagnosticsForFile(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        withValidityAssertion { analysisSession.diagnosticProvider.collectDiagnosticsForFile(this, filter) }
}

public typealias KtDiagnosticProviderMixIn = KaDiagnosticProviderMixIn

public enum class KaDiagnosticCheckerFilter {
    ONLY_COMMON_CHECKERS,
    ONLY_EXTENDED_CHECKERS,
    EXTENDED_AND_COMMON_CHECKERS,
}

public typealias KtDiagnosticCheckerFilter = KaDiagnosticCheckerFilter