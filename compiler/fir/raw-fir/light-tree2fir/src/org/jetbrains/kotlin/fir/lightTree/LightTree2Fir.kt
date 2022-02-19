/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lightTree.converter.DeclarationsConverter
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.readSourceFileWithMapping
import java.io.File
import java.nio.file.Path

class LightTree2Fir(
    val session: FirSession,
    private val scopeProvider: FirScopeProvider,
    private val diagnosticsReporter: DiagnosticReporter? = null
) {
    companion object {
        private val parserDefinition = KotlinParserDefinition()
        private fun makeLexer() = KotlinLexer()

        fun buildLightTreeBlockExpression(code: String): FlyweightCapableTreeStructure<LighterASTNode> {
            val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, makeLexer(), code)
            KotlinLightParser.parseBlockExpression(builder)
            return builder.lightTree
        }

        fun buildLightTreeLambdaExpression(code: String): FlyweightCapableTreeStructure<LighterASTNode> {
            val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, makeLexer(), code)
            KotlinLightParser.parseLambdaExpression(builder)
            return builder.lightTree
        }

        fun buildLightTree(code: CharSequence): FlyweightCapableTreeStructure<LighterASTNode> {
            val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, makeLexer(), code)
            KotlinLightParser.parse(builder)
            return builder.lightTree
        }
    }

    fun buildFirFile(path: Path): FirFile {
        return buildFirFile(path.toFile())
    }

    fun buildFirFile(file: File): FirFile {
        val sourceFile = KtIoFileSourceFile(file)
        val (code, linesMapping) = with(file.inputStream().reader(Charsets.UTF_8)) {
            this.readSourceFileWithMapping()
        }
        return buildFirFile(code, sourceFile, linesMapping)
    }

    fun buildFirFile(
        lightTree: FlyweightCapableTreeStructure<LighterASTNode>,
        sourceFile: KtSourceFile,
        linesMapping: KtSourceFileLinesMapping
    ): FirFile =
        DeclarationsConverter(
            session, scopeProvider, lightTree, diagnosticsReporter = diagnosticsReporter,
            diagnosticContext = makeDiagnosticContext(sourceFile.path)
        ).convertFile(lightTree.root, sourceFile, linesMapping)

    fun buildFirFile(code: CharSequence, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping): FirFile =
        buildFirFile(buildLightTree(code), sourceFile, linesMapping)

    private fun makeDiagnosticContext(path: String?) =
        if (diagnosticsReporter == null) null else object : DiagnosticContext {
            override val containingFilePath = path
            override val languageVersionSettings: LanguageVersionSettings get() = session.languageVersionSettings
            override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean = false
        }
}

