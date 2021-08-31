/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor

fun FirSession.runResolution(firFiles: List<FirFile>): Pair<ScopeSession, List<FirFile>> {
    val resolveProcessor = FirTotalResolveProcessor(this)
    resolveProcessor.process(firFiles)
    return resolveProcessor.scopeSession to firFiles
}

@OptIn(ExperimentalStdlibApi::class)
fun FirSession.runCheckers(scopeSession: ScopeSession, firFiles: List<FirFile>): Map<FirFile, List<FirDiagnostic>> {
    val collector = FirDiagnosticsCollector.create(this, scopeSession)
    return buildMap {
        for (file in firFiles) {
            val reporter = DiagnosticReporterFactory.createReporter()
            collector.collectDiagnostics(file, reporter)
            put(file, reporter.diagnostics)
        }
    }
}