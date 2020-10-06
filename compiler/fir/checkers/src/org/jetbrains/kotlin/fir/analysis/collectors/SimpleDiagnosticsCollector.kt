/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.SimpleDiagnosticReporter

class SimpleDiagnosticsCollector(session: FirSession) : AbstractDiagnosticCollector(session) {
    override var reporter = SimpleDiagnosticReporter()
        private set

    override fun initializeCollector() {
        reporter = SimpleDiagnosticReporter()
    }

    override fun getCollectedDiagnostics(): Iterable<FirDiagnostic<*>> {
        return reporter.diagnostics
    }
}

