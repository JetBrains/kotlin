/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.readSourceFileWithMapping
import kotlin.reflect.KFunction2

fun FirSession.buildFirViaLightTree(
    files: Collection<KtSourceFile>,
    diagnosticsReporter: DiagnosticReporter? = null,
    reportFilesAndLines: ((Int, Int) -> Unit)? = null
): List<FirFile> {
    val firProvider = (firProvider as FirProviderImpl)
    val sourcesToPathsMapper = sourcesToPathsMapper
    val builder = LightTree2Fir(this, firProvider.kotlinScopeProvider, diagnosticsReporter)
    val shouldCountLines = (reportFilesAndLines != null)
    var linesCount = 0
    val firFiles = files.map { file ->
        val (code, linesMapping) = file.getContentsAsStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        if (shouldCountLines) {
            linesCount += linesMapping.linesCount
        }
        builder.buildFirFile(code, file, linesMapping).also { firFile ->
            firProvider.recordFile(firFile)
            sourcesToPathsMapper.registerFileSource(firFile.source!!, file.path ?: file.name)
        }
    }
    reportFilesAndLines?.invoke(files.count(), linesCount)
    return firFiles
}

fun FirSession.buildFirFromKtFiles(ktFiles: Collection<KtFile>): List<FirFile> {
    val firProvider = (firProvider as FirProviderImpl)
    val builder = PsiRawFirBuilder(this, firProvider.kotlinScopeProvider)
    return ktFiles.map {
        builder.buildFirFile(it).also { firFile ->
            firProvider.recordFile(firFile)
        }
    }
}

fun buildResolveAndCheckFirFromKtFiles(
    session: FirSession,
    ktFiles: List<KtFile>,
    diagnosticsReporter: BaseDiagnosticsCollector
): ModuleCompilerAnalyzedOutput {
    return resolveAndCheckFir(session, session.buildFirFromKtFiles(ktFiles), diagnosticsReporter)
}

/**
 * This function runs only common checkers
 * Platform checkers should be run separately, after all parts of MPP structure will be resolved
 */
fun resolveAndCheckFir(
    session: FirSession,
    firFiles: List<FirFile>,
    diagnosticsReporter: BaseDiagnosticsCollector
): ModuleCompilerAnalyzedOutput {
    val (scopeSession, fir) = session.runResolution(firFiles)
    session.runCheckers(scopeSession, fir, diagnosticsReporter, MppCheckerKind.Common)
    return ModuleCompilerAnalyzedOutput(session, scopeSession, fir)
}

fun buildResolveAndCheckFirViaLightTree(
    session: FirSession,
    ktFiles: Collection<KtSourceFile>,
    diagnosticsReporter: BaseDiagnosticsCollector,
    countFilesAndLines: KFunction2<Int, Int, Unit>?
): ModuleCompilerAnalyzedOutput {
    val firFiles = session.buildFirViaLightTree(ktFiles, diagnosticsReporter, countFilesAndLines)
    return resolveAndCheckFir(session, firFiles, diagnosticsReporter)
}
