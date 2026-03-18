/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic

/**
 * Standard implementation of [BaseDiagnosticsCollector]
 */
class DiagnosticsCollectorImpl : BaseDiagnosticsCollector() {
    override val diagnostics: List<KtDiagnostic>
        get() = diagnosticsByFilePath.flatMap { it.value }
    override val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>
        field = mutableMapOf<String?, MutableList<KtDiagnostic>>()

    override var hasErrors = false
        private set

    override var hasWarningsForWError = false
        private set

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && !context.isDiagnosticSuppressed(diagnostic)) {
            diagnosticsByFilePath.getOrPut(context.containingFilePath) { mutableListOf() }.run {
                add(diagnostic)
                hasErrors = hasErrors || diagnostic.severity.isError
                hasWarningsForWError = hasWarningsForWError || diagnostic.severity.isErrorWhenWError
            }
        }
    }
}
