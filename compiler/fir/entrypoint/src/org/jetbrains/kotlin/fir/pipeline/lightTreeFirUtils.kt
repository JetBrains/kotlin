/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * A facade for the light tree, which is absent in the Analysis API.
 * All code that directly depends on the light tree should be localized in this file.
 */

@file:JvmName("LightTreeFirUtils")

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.builder.MultiplatformParsing2Fir
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.fir.syntax.FirKmpSyntaxComponent
import org.jetbrains.kotlin.fir.syntax.FirLightTreeSyntaxComponent
import org.jetbrains.kotlin.fir.syntax.FirPsiSyntaxComponent
import org.jetbrains.kotlin.fir.syntax.FirSyntaxComponent
import org.jetbrains.kotlin.readSourceFileWithMapping

/**
 * Registers the [org.jetbrains.kotlin.fir.syntax.FirSyntaxComponent] matching the raw FIR builder selected by [parserMode].
 *
 * The Analysis API doesn't use this function; it always registers [FirPsiSyntaxComponent] in `registerIdeComponents`.
 *
 * The set of implementations is expected to shrink:
 * - [ParserMode.Psi] will be removed from the CLI (KT-86283); [FirPsiSyntaxComponent] stays for the Analysis API only.
 * - [ParserMode.LightTree] and [FirLightTreeSyntaxComponent] will be dropped once [ParserMode.KmpTree] becomes the default;
 */
@OptIn(SessionConfiguration::class)
fun FirSession.registerSyntaxComponent(parserMode: ParserMode) {
    val syntaxComponent = when (parserMode) {
        ParserMode.Psi -> FirPsiSyntaxComponent
        ParserMode.LightTree -> FirLightTreeSyntaxComponent
        ParserMode.KmpTree -> FirKmpSyntaxComponent
    }
    register(FirSyntaxComponent::class, syntaxComponent)
}

fun FirSession.buildFirViaLightTree(
    files: Collection<KtSourceFile>,
    diagnosticsReporter: DiagnosticReporter?,
    useMultiplatformParsing: Boolean,
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
            sourcesToPathsMapper.registerFileSource(firFile.source!!, file.path ?: file.name, linesMapping)
        }
    }
    reportFilesAndLines?.invoke(files.count(), linesCount)
    return firFiles
}
