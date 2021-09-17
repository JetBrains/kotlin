/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.impl

import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.KtDiagnostic

class DiagnosticReporterWithSuppress : BaseDiagnosticReporter() {
    private val _diagnostics: MutableList<KtDiagnostic> = mutableListOf()
    override val diagnostics: List<KtDiagnostic>
        get() = _diagnostics

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        if (!context.isDiagnosticSuppressed(diagnostic)) {
            _diagnostics += diagnostic
        }
    }
}
