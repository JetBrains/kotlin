/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.platform.syntax.lexer.Lexer
import com.intellij.platform.syntax.lexer.performLexing
import com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.AbstractTree2Fir
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.parser.AbstractParser
import org.jetbrains.kotlin.kmp.parser.KotlinParser
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree
import org.jetbrains.kotlin.kmp.tree.buildLanguageSpecificLightTree

class MultiplatformParsing2Fir(
    override val session: FirSession,
    private val scopeProvider: FirScopeProvider,
    private val diagnosticsReporter: DiagnosticReporter? = null,
) : AbstractTree2Fir() {
    override fun buildFirFile(code: CharSequence, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping): FirFile {
        val parser = KotlinParser(sourceFile.extension == "kts", isLazy = false)
        val lightTree = buildLightTree(code, KotlinLexer(), parser)
        return buildFirFile(lightTree, sourceFile, linesMapping)
    }

    private fun buildFirFile(
        lightTree: LightSyntaxTree,
        sourceFile: KtSourceFile,
        linesMapping: KtSourceFileLinesMapping,
    ): FirFile {
        return MultiplatformParsingRawFirDeclarationBuilder(
            session,
            scopeProvider,
            lightTree.lightSourceTreeStructure as KotlinLightTreeStructure
        ).convertFile(lightTree.getRoot(), sourceFile, linesMapping)
    }

    private fun buildLightTree(
        charSequence: CharSequence,
        lexer: Lexer,
        parser: AbstractParser,
    ): LightSyntaxTree {
        val syntaxTreeBuilder = SyntaxTreeBuilderFactory.builder(
            charSequence,
            performLexing(charSequence, lexer, cancellationProvider = null, logger = null),
            whitespaces = parser.whitespaces,
            comments = parser.comments,
        ).withStartOffset(startOffset = 0)
            .withWhitespaceOrCommentBindingPolicy(parser.whitespaceOrCommentBindingPolicy)
            .build()

        parser.parse(syntaxTreeBuilder)
        return buildLanguageSpecificLightTree(syntaxTreeBuilder, charSequence) {
            KotlinLightTreeStructure(it)
        }
    }
}
