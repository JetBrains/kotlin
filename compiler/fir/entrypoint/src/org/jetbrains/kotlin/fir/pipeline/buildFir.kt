/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import com.intellij.openapi.util.text.StringUtilRt
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.FileNotFoundException

fun FirSession.buildFirViaLightTree(
    files: Collection<File>,
    projectEnvironment: AbstractProjectEnvironment,
    diagnosticsReporter: DiagnosticReporter? = null,
    reportFilesAndLines: ((Int, Int) -> Unit)? = null
): List<FirFile> {
    val firProvider = (firProvider as FirProviderImpl)
    val sourcesToPathsMapper = sourcesToPathsMapper
    val builder = LightTree2Fir(this, firProvider.kotlinScopeProvider, diagnosticsReporter)
    val shouldCountLines = (reportFilesAndLines != null)
    var linesCount = 0
    val firFiles = files.map { file ->
        val text = projectEnvironment.getFileText(file.absolutePath) ?: throw FileNotFoundException(file.path)
        val code = StringUtilRt.convertLineSeparators(text)
        if (shouldCountLines) {
            linesCount += code.count { it == '\n' } // assuming converted line separators
        }
        builder.buildFirFile(code, file.name, file.path).also { firFile ->
            firProvider.recordFile(firFile)
            sourcesToPathsMapper.registerFileSource(firFile.source!!, file.path)
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
