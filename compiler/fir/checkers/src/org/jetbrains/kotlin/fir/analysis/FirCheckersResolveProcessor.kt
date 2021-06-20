/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirCheckersResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    val diagnosticCollector: AbstractDiagnosticCollector = FirDiagnosticsCollector.create(session, scopeSession)

    override val transformer: FirTransformer<Nothing?> = FirCheckersRunnerTransformer(diagnosticCollector)
}

class FirCheckersRunnerTransformer(private val diagnosticCollector: AbstractDiagnosticCollector) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        val reporter = DiagnosticReporterFactory.createReporter()
        diagnosticCollector.collectDiagnostics(file, reporter)
        return file
    }
}
