/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

abstract class KtDiagnosticProvider : KtAnalysisSessionComponent() {
    abstract fun getDiagnosticsForElement(element: KtElement, filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>>
    abstract fun collectDiagnosticsForFile(ktFile: KtFile, filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>>
}

interface KtDiagnosticProviderMixIn : KtAnalysisSessionMixIn {
    fun KtElement.getDiagnostics(filter: KtDiagnosticCheckerFilter): Collection<KtDiagnostic> =
        analysisSession.diagnosticProvider.getDiagnosticsForElement(this, filter)

    fun KtFile.collectDiagnosticsForFile(filter: KtDiagnosticCheckerFilter): Collection<KtDiagnosticWithPsi<*>> =
        analysisSession.diagnosticProvider.collectDiagnosticsForFile(this, filter)
}

enum class KtDiagnosticCheckerFilter {
    ONLY_COMMON_CHECKERS,
    ONLY_EXTENDED_CHECKERS,
    EXTENDED_AND_COMMON_CHECKERS,
}