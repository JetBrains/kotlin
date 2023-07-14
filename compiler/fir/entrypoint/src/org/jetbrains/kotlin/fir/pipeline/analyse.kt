/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping

fun FirSession.runResolution(firFiles: List<FirFile>): Pair<ScopeSession, List<FirFile>> {
    val resolveProcessor = FirTotalResolveProcessor(this)
    resolveProcessor.process(firFiles)
    return resolveProcessor.scopeSession to firFiles
}

fun FirSession.runCheckers(
    scopeSession: ScopeSession,
    firFiles: List<FirFile>,
    reporter: BaseDiagnosticsCollector
): Map<FirFile, List<KtDiagnostic>> {
    val collector = FirDiagnosticsCollector.create(this, scopeSession)
    collector.collectDiagnosticsInSettings(reporter)
    for (file in firFiles) {
        withFileAnalysisExceptionWrapping(file) {
            collector.collectDiagnostics(file, reporter)
        }
    }
    return firFiles.associateWith {
        val path = it.sourceFile?.path ?: return@associateWith emptyList()
        reporter.diagnosticsByFilePath[path] ?: emptyList()
    }
}
