/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector


interface DiagnosticsCollectorHolder {
    val diagnosticReporter: BaseDiagnosticsCollector
}

class DiagnosticsCollectorStub : BaseDiagnosticsCollector() {
    override val diagnostics: List<KtDiagnostic>
        get() = error("Should not reach here")
    override val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>
        get() = error("Should not reach here")
    override val hasErrors: Boolean
        get() = error("Should not reach here")
    override val rawReporter: RawReporter
        get() = error("Should not reach here")

    override fun report(
        diagnostic: KtDiagnostic?,
        context: DiagnosticContext,
    ) {
        error("Should not reach here")
    }
}
