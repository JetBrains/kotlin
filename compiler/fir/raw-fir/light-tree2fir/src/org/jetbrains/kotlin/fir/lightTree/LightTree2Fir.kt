/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.converter.DeclarationsConverter
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import java.io.File
import java.nio.file.Path

class LightTree2Fir(
    val session: FirSession,
    private val scopeProvider: FirScopeProvider
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

        fun buildLightTree(code: String): FlyweightCapableTreeStructure<LighterASTNode> {
            val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, makeLexer(), code)
            KotlinLightParser.parse(builder)
            return builder.lightTree
        }
    }

    fun buildFirFile(path: Path): FirFile {
        return buildFirFile(path.toFile())
    }

    fun buildFirFile(file: File): FirFile {
        val code = FileUtil.loadFile(file, CharsetToolkit.UTF8, true)
        return buildFirFile(code, file.name, file.path)
    }

    fun buildFirFile(lightTreeFile: LightTreeFile): FirFile = with(lightTreeFile) {
        DeclarationsConverter(
            session, scopeProvider, lightTree, diagnosticsReporter = diagnosticsReporter,
            diagnosticContext = makeDiagnosticContext(path)
        ).convertFile(lightTree.root, fileName, path)
    }

    fun buildFirFile(code: String, fileName: String, path: String?): FirFile {
        val lightTree = buildLightTree(code)

        return DeclarationsConverter(session, scopeProvider, lightTree)
            .convertFile(lightTree.root, fileName, path)
    }
}

data class LightTreeFile(
    val lightTree: FlyweightCapableTreeStructure<LighterASTNode>,
    val fileName: String,
    val path: String?
)

