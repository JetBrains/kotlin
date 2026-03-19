/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity

class SimpleDiagnosticsCollectorWithSuppress : BaseDiagnosticsCollector() {
    private val _diagnosticsByFilePath: MutableMap<String?, MutableList<KtDiagnostic>> = mutableMapOf()
    override val diagnostics: List<KtDiagnostic>
        get() = diagnosticsByFile.flatMap { it.value }
    private val _diagnosticsByFile = mutableMapOf<KtSourceFile?, MutableList<KtDiagnostic>>()
    override val diagnosticsByFile: Map<KtSourceFile?, List<KtDiagnostic>>
        get() = _diagnosticsByFile

    override var hasErrors = false
        private set

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && !context.isDiagnosticSuppressed(diagnostic)) {
            _diagnosticsByFile.getOrPut(context.containingFile) { mutableListOf() }.run {
                add(diagnostic)
                if (!hasErrors && diagnostic.severity == Severity.ERROR) {
                    hasErrors = true
                }
            }
        }
    }
}
