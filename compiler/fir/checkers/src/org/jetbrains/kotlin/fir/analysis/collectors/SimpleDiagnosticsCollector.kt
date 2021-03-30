/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.impl.BaseDiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.impl.DiagnosticReporterWithSuppress
import org.jetbrains.kotlin.fir.analysis.diagnostics.impl.SimpleDiagnosticReporter
import org.jetbrains.kotlin.fir.resolve.ScopeSession

class SimpleDiagnosticsCollector(
    session: FirSession,
    scopeSession: ScopeSession,
    private val disableSuppress: Boolean = false
) : AbstractDiagnosticCollector(session, scopeSession) {
    override var reporter = createDiagnosticReporter()
        private set

    private fun createDiagnosticReporter(): BaseDiagnosticReporter {
        return if (disableSuppress) {
            SimpleDiagnosticReporter()
        } else {
            DiagnosticReporterWithSuppress()
        }
    }

    override fun initializeCollector() {
        reporter = createDiagnosticReporter()
    }

    override fun getCollectedDiagnostics(): List<FirDiagnostic<*>> {
        return reporter.diagnostics
    }
}

