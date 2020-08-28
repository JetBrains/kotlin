/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.registerAllComponents
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.util.addValueFor
import org.jetbrains.kotlin.psi.KtElement

internal class FirIdeDiagnosticsCollector private constructor(
    session: FirSession,
) : AbstractDiagnosticCollector(session) {
    private val result = mutableMapOf<KtElement, MutableList<Diagnostic>>()

    init {
        registerAllComponents()
    }

    private inner class Reporter : DiagnosticReporter() {
        override fun report(diagnostic: FirDiagnostic<*>?) {
            if (diagnostic !is FirPsiDiagnostic<*>) return
            val psi = diagnostic.element.psi as? KtElement ?: return
            result.addValueFor(psi, diagnostic.asPsiBasedDiagnostic())
        }
    }

    private lateinit var reporter: Reporter

    override fun initializeCollector() {
        reporter = Reporter()
    }

    override fun getCollectedDiagnostics(): Iterable<FirDiagnostic<*>> {
        // Not necessary in IDE
        return emptyList()
    }

    override fun runCheck(block: (DiagnosticReporter) -> Unit) {
        block(reporter)
    }

    companion object {
        /**
         * Collects diagnostics for given [firFile]
         * Should be called under [firFile]-based lock
         */
        fun collect(firFile: FirFile): Map<KtElement, List<Diagnostic>> =
            FirIdeDiagnosticsCollector(firFile.session).let { collector ->
                collector.collectDiagnostics(firFile)
                collector.result
            }
    }
}