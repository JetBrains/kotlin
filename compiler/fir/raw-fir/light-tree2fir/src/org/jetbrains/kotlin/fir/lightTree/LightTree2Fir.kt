/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
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
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.readSourceFileWithMapping
import java.io.File
import java.nio.file.Path

class LightTree2Fir(
    val session: FirSession,
    private val scopeProvider: FirScopeProvider,
    private val diagnosticsReporter: DiagnosticReporter? = null,
) {
    companion object {
        private val parserDefinition = KotlinParserDefinition()
        private fun makeLexer() = KotlinLexer()

        fun buildLightTree(
            code: CharSequence,
            errorListener: LightTreeParsingErrorListener?,
        ): FlyweightCapableTreeStructure<LighterASTNode> {
            val builder = PsiBuilderFactory.getInstance().createBuilder(parserDefinition, makeLexer(), code)
            return KotlinLightParser.parse(builder, /* isScript = */ false).also {
                if (errorListener != null) reportErrors(it.root, it, errorListener)
            }
        }

        private fun reportErrors(
            node: LighterASTNode,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
            errorListener: LightTreeParsingErrorListener,
            ref: Ref<Array<LighterASTNode?>> = Ref<Array<LighterASTNode?>>(),
        ) {
            tree.getChildren(node, ref)
            val kidsArray = ref.get() ?: return

            for (kid in kidsArray) {
                if (kid == null) break
                val tokenType = kid.tokenType
                if (tokenType == TokenType.ERROR_ELEMENT) {
                    val message = PsiBuilderImpl.getErrorMessage(kid)
                    errorListener.onError(kid.startOffset, kid.endOffset, message)
                }

                ref.set(null)
                reportErrors(kid, tree, errorListener, ref)
            }
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
        linesMapping: KtSourceFileLinesMapping,
    ): FirFile {
        return LightTreeRawFirDeclarationBuilder(session, scopeProvider, lightTree)
            .convertFile(lightTree.root, sourceFile, linesMapping)
    }

    fun buildFirFile(code: CharSequence, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping): FirFile {
        val errorListener = makeErrorListener(sourceFile)
        val lightTree = buildLightTree(code, errorListener)
        return buildFirFile(lightTree, sourceFile, linesMapping)
    }

    private fun makeErrorListener(sourceFile: KtSourceFile): LightTreeParsingErrorListener? {
        val diagnosticsReporter = diagnosticsReporter ?: return null
        return diagnosticsReporter.toKotlinParsingErrorListener(sourceFile, session.languageVersionSettings)
    }
}
