/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.registerAllComponents
import org.jetbrains.kotlin.fir.analysis.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtElement

class FirIdeDiagnosticsCollector(private val resolveState: FirModuleResolveState) : AbstractDiagnosticCollector() {

    init {
        registerAllComponents()
    }

    private inner class Reporter : DiagnosticReporter() {
        override fun report(diagnostic: ConeDiagnostic?) {
            if (diagnostic == null) return
            val psi = diagnostic.source.psi as? KtElement ?: return
            resolveState.record(psi, diagnostic.diagnostic)
        }

    }

    private lateinit var reporter: Reporter

    override fun initializeCollector() {
        reporter = Reporter()
    }

    override fun getCollectedDiagnostics(): Iterable<ConeDiagnostic> {
        // Not necessary in IDE
        return emptyList()
    }

    override fun runCheck(block: (DiagnosticReporter) -> Unit) {
        block(reporter)
    }
}