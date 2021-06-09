/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.impl

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic

class DiagnosticReporterWithSuppress : BaseDiagnosticReporter() {
    private val _diagnostics: MutableList<FirDiagnostic> = mutableListOf()
    override val diagnostics: List<FirDiagnostic>
        get() = _diagnostics

    override fun report(diagnostic: FirDiagnostic?, context: CheckerContext) {
        if (diagnostic == null) return
        if (!context.isDiagnosticSuppressed(diagnostic)) {
            _diagnostics += diagnostic
        }
    }
}
