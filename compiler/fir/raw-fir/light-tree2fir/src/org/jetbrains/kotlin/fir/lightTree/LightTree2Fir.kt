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
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.converter.DeclarationsConverter
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import java.io.File
import java.nio.file.Path

class LightTree2Fir(
    val session: FirSession = @OptIn(PrivateSessionConstructor::class) object : FirSession(null) {},
    private val scopeProvider: FirScopeProvider,
    private val stubMode: Boolean = false
) {
    //private val ktDummyFile = KtFile(SingleRootFileViewProvider(PsiManager.getInstance(project), LightVirtualFile()), false)

    companion object {
        private val parserDefinition = KotlinParserDefinition()
        private val lexer = KotlinLexer()

        fun buildLightTreeBlockExpression(code: String): FlyweightCapableTreeStructure<LighterASTNode> {
            val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, lexer, code)
            //KotlinParser.parseBlockExpression(builder)
            KotlinLightParser.parseBlockExpression(builder)
            return builder.lightTree
        }

        fun buildLightTreeLambdaExpression(code: String): FlyweightCapableTreeStructure<LighterASTNode> {
            val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, lexer, code)
            //KotlinParser.parseLambdaExpression(builder)
            KotlinLightParser.parseLambdaExpression(builder)
            return builder.lightTree
        }
    }

    fun buildFirFile(path: Path): FirFile {
        return buildFirFile(path.toFile())
    }

    fun buildFirFile(file: File): FirFile {
        val code = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
        return buildFirFile(code, file.name)
    }

    fun buildLightTree(code: String): FlyweightCapableTreeStructure<LighterASTNode> {
        val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, lexer, code)
        //KotlinParser(project).parse(null, builder, ktDummyFile)
        KotlinLightParser.parse(builder)
        return builder.lightTree
    }

    fun buildFirFile(code: String, fileName: String): FirFile {
        val lightTree = buildLightTree(code)

        return DeclarationsConverter(session, scopeProvider, stubMode, lightTree)
            .convertFile(lightTree.root, fileName)
    }
}
