/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.readSourceFileWithMapping

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
            linesCount += linesMapping.lastOffset
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
    val builder = RawFirBuilder(this, firProvider.kotlinScopeProvider, PsiHandlingMode.COMPILER)
    return ktFiles.map {
        builder.buildFirFile(it).also { firFile ->
            firProvider.recordFile(firFile)
        }
    }
}
