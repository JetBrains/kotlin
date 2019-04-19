/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.ParserDefinition
import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.measureNanoTime

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class SimpleTestCase : AbstractRawFirBuilderTestCase() {
    fun test() {
        val file = KtFile(
            (PsiManager.getInstance(myProject) as PsiManagerEx).fileManager.createFileViewProvider(
                LightVirtualFile(
                    "foo",
                    KotlinFileType.INSTANCE,
                    ""
                ), true
            ), false
        )
        val parserDefinition = KotlinParserDefinition()
        val lexer = parserDefinition.createLexer(myProject)
        val code = """
            class SimpleClass {
                val x: Int
            }
        """.trimIndent()

        val builder = PsiBuilderFactoryImpl().createBuilder(
            parserDefinition,
            lexer,
            code
        )

        val ktParsing = parserDefinition.createParser(myProject) as KotlinParser
        ktParsing.parse(null, builder, file)

        println("LightTree")
        println(DebugUtil.lightTreeToString(builder.lightTree, false))
        println("AST Tree")
        println(DebugUtil.nodeTreeToString(builder.treeBuilt, false))

        val firFromLightTreeFile = Converter(object : FirSessionBase() {}, true, builder.lightTree).convertFile(builder.lightTree.root)
        println("Fir from LightTree")
        println(StringBuilder().also { FirRenderer(it).visitFile(firFromLightTreeFile) }.toString())

        val psiFile = createPsiFile("foo", code) as KtFile
        val firFromPsiFile = psiFile.toFirFile(stubMode = true)
        println("Fir from PSI")
        println(StringBuilder().also { FirRenderer(it).visitFile(firFromPsiFile) }.toString())

    }
}

