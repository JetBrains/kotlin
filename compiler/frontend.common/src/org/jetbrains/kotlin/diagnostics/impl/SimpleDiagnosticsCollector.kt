/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity

class SimpleDiagnosticsCollector(override val rawReport: (Boolean, String) -> Unit = { _, _ -> }) : BaseDiagnosticsCollector() {
    private val _diagnosticsByFilePath: MutableMap<String?, MutableList<KtDiagnostic>> = mutableMapOf()
    override val diagnostics: List<KtDiagnostic>
        get() = _diagnosticsByFilePath.flatMap { it.value }
    override val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>
        get() = _diagnosticsByFilePath

    override var hasErrors = false
        private set

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null) {
            _diagnosticsByFilePath.getOrPut(context.containingFilePath) { mutableListOf() }.run {
                add(diagnostic)
                if (!hasErrors && diagnostic.severity == Severity.ERROR) {
                    hasErrors = true
                }
            }
        }
    }
}
