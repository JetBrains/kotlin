/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.KtMissingSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithoutSource

/**
 * Standard implementation of [BaseDiagnosticsCollector]
 */
class DiagnosticsCollectorImpl : BaseDiagnosticsCollector() {
    override val diagnostics: List<KtDiagnostic>
        get() = diagnosticsByFile.flatMap { it.value }
    override val diagnosticsByFile: Map<KtSourceFile?, List<KtDiagnostic>>
        field = mutableMapOf<KtSourceFile?, MutableList<KtDiagnostic>>()

    override var hasErrors = false
        private set

    override var hasWarningsForWError = false
        private set

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && !context.isDiagnosticSuppressed(diagnostic)) {

            val containingFile = when (diagnostic) {
                is KtDiagnosticWithoutSource -> null
                is KtDiagnosticWithSource -> when (diagnostic.element) {
                    is KtMissingSourceElement -> null
                    else -> context.containingFile
                }
            }

            diagnosticsByFile.getOrPut(containingFile) { mutableListOf() }.run {
                add(diagnostic)
                hasErrors = hasErrors || diagnostic.severity.isError
                hasWarningsForWError = hasWarningsForWError || diagnostic.severity.isErrorWhenWError
            }
        }
    }
}
