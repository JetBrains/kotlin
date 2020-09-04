/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.registerAllComponents
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.util.addValueFor
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.psi.KtElement

internal class FirIdeDiagnosticsCollector private constructor(
    session: FirSession,
) : AbstractDiagnosticCollector(
    session,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(session, ScopeSession())
) {
    private val result = mutableMapOf<KtElement, MutableList<Diagnostic>>()

    init {
        registerAllComponents()
    }

    private inner class Reporter : DiagnosticReporter() {
        override fun report(diagnostic: FirDiagnostic<*>?) {
            checkCanceled()
            try {
                if (diagnostic !is FirPsiDiagnostic<*>) return
                val psi = diagnostic.element.psi as? KtElement ?: return
                result.addValueFor(psi, diagnostic.asPsiBasedDiagnostic())
            } catch (e: Throwable) {
                LOG.error(e)
            }
        }
    }

    override var reporter: DiagnosticReporter = Reporter()
        private set

    override fun initializeCollector() {
        reporter = Reporter()
    }

    override fun getCollectedDiagnostics(): Iterable<FirDiagnostic<*>> {
        // Not necessary in IDE
        return emptyList()
    }

    companion object {
        private val LOG = Logger.getInstance(FirIdeDiagnosticsCollector::class.java)

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
