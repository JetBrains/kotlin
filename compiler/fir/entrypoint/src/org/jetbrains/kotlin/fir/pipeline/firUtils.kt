/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.builder.MultiplatformParsing2Fir
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.readSourceFileWithMapping
import java.io.File

fun FirSession.buildFirViaLightTree(
    files: Collection<KtSourceFile>,
    diagnosticsReporter: DiagnosticReporter?,
    useMultiplatformParsing: Boolean,
    sourceSetByFile: Map<File, File> = emptyMap(),
    reportFilesAndLines: ((Int, Int) -> Unit)?,
): List<FirFile> {
    val firProvider = (firProvider as FirProviderImpl)
    val sourcesToPathsMapper = sourcesToPathsMapper
    val builder = if (useMultiplatformParsing) {
        MultiplatformParsing2Fir(this, firProvider.kotlinScopeProvider, diagnosticsReporter)
    } else {
        LightTree2Fir(this, firProvider.kotlinScopeProvider, diagnosticsReporter)
    }
    val shouldCountLines = (reportFilesAndLines != null)
    var linesCount = 0
    val firFiles = files.map { file ->
        val [code, linesMapping] = file.getContentsAsStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        if (shouldCountLines) {
            linesCount += linesMapping.linesCount
        }
        builder.buildFirFile(code, file, linesMapping).also { firFile ->
            firProvider.recordFile(firFile)
            sourcesToPathsMapper.registerFileSource(
                sourceElement = firFile.source!!,
                path = file.path ?: file.name,
                owningSourceRootPath = file.path?.let { sourceSetByFile[File(it)] }?.canonicalPath,
            )
        }
    }
    reportFilesAndLines?.invoke(files.count(), linesCount)
    return firFiles
}

fun FirSession.buildFirFromKtFiles(
    ktFiles: Collection<KtFile>,
    sourceSetByFile: Map<File, File> = emptyMap(),
): List<FirFile> {
    val firProvider = (firProvider as FirProviderImpl)
    val builder = PsiRawFirBuilder(this, firProvider.kotlinScopeProvider)
    return ktFiles.map {
        builder.buildFirFile(it).also { firFile ->
            firProvider.recordFile(firFile)
            firFile.source?.psi?.containingFile?.virtualFile?.path?.let { path ->
                sourceSetByFile[File(path)]?.let { owningSourceRoot ->
                    sourcesToPathsMapper.registerOwningSourceRoot(path, owningSourceRoot.canonicalPath)
                }
            }
        }
    }
}

fun buildResolveAndCheckFirFromKtFiles(
    session: FirSession,
    ktFiles: List<KtFile>,
    diagnosticsReporter: BaseDiagnosticsCollector,
    sourceSetByFile: Map<File, File> = emptyMap(),
): SingleModuleFrontendOutput {
    return resolveAndCheckFir(session, session.buildFirFromKtFiles(ktFiles, sourceSetByFile), diagnosticsReporter)
}

/**
 * This function runs only common checkers
 * Platform checkers should be run separately, after all parts of MPP structure will be resolved
 */
fun resolveAndCheckFir(
    session: FirSession,
    firFiles: List<FirFile>,
    diagnosticsReporter: BaseDiagnosticsCollector
): SingleModuleFrontendOutput {
    val [scopeSession, fir] = session.runResolution(firFiles)
    // Skip checkers in header mode.
    if (!session.languageVersionSettings.getFlag(AnalysisFlags.headerMode)) {
        session.runCheckers(scopeSession, fir, diagnosticsReporter, MppCheckerKind.Common)
    }
    return SingleModuleFrontendOutput(session, scopeSession, fir)
}

fun buildResolveAndCheckFirViaLightTree(
    session: FirSession,
    ktFiles: Collection<KtSourceFile>,
    diagnosticsReporter: BaseDiagnosticsCollector,
    sourceSetByFile: Map<File, File> = emptyMap(),
    countFilesAndLines: ((Int, Int) -> Unit)?
): SingleModuleFrontendOutput {
    val firFiles = session.buildFirViaLightTree(
        files = ktFiles,
        diagnosticsReporter = diagnosticsReporter,
        useMultiplatformParsing = false,
        sourceSetByFile = sourceSetByFile,
        reportFilesAndLines = countFilesAndLines,
    )
    return resolveAndCheckFir(session, firFiles, diagnosticsReporter)
}
