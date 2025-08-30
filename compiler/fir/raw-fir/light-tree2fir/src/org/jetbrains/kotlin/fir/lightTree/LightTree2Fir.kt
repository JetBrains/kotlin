/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lightTree.converter.LightTreeRawFirDeclarationBuilder
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.readSourceFileWithMapping
import java.io.File
import java.nio.file.Path

class LightTree2Fir(
    val session: FirSession,
    val headerCompilationMode: Boolean,
    private val scopeProvider: FirScopeProvider,
    private val diagnosticsReporter: DiagnosticReporter? = null,
) {
    fun buildFirFile(path: Path): FirFile {
        return buildFirFile(path.toFile())
    }

    fun buildFirFile(file: File): FirFile {
        val sourceFile = KtIoFileSourceFile(file)
        val (code, linesMapping) = file.inputStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        return buildFirFile(code, sourceFile, linesMapping)
    }

    fun buildFirFile(
        lightTree: FlyweightCapableTreeStructure<LighterASTNode>,
        sourceFile: KtSourceFile,
        linesMapping: KtSourceFileLinesMapping,
    ): FirFile {
        return LightTreeRawFirDeclarationBuilder(session, scopeProvider, lightTree, headerCompilationMode)
            .convertFile(lightTree.root, sourceFile, linesMapping)
    }

    fun buildFirFile(code: CharSequence, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping): FirFile {
        val errorListener = makeErrorListener(sourceFile)
        val lightTree = KotlinLightParser.buildLightTree(code, sourceFile, errorListener)
        return buildFirFile(lightTree, sourceFile, linesMapping)
    }

    private fun makeErrorListener(sourceFile: KtSourceFile): KotlinLightParser.LightTreeParsingErrorListener? {
        val diagnosticsReporter = diagnosticsReporter ?: return null
        return diagnosticsReporter.toKotlinParsingErrorListener(sourceFile, session.languageVersionSettings)
    }
}
