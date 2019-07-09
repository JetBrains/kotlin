/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.compare

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.walkTopDown
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class TreesCompareTest : AbstractRawFirBuilderTestCase() {
    private fun compare(
        stubMode: Boolean,
        visitAnnotation: Boolean = true
    ) {
        val path = System.getProperty("user.dir")
        var counter = 0
        var errorCounter = 0

        val parserDefinition = KotlinParserDefinition()
        val lexer = parserDefinition.createLexer(myProject)
        val lightTreeConverter = LightTree2Fir(stubMode, parserDefinition, lexer)
        val firVisitor = FirPartialTransformer(visitAnnotation = visitAnnotation)

        println("BASE PATH: $path")
        path.walkTopDown { file ->
            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()

            //light tree
            val firFileFromLightTree = lightTreeConverter.buildFirFile(text, file.name)
            val treeFromLightTree =
                StringBuilder().also { FirRenderer(it).visitFile(firVisitor.transformFile(firFileFromLightTree, null).single) }.toString()

            //psi
            val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
            val firFileFromPsi = ktFile.toFirFile(stubMode)
            val treeFromPsi =
                StringBuilder().also { FirRenderer(it).visitFile(firVisitor.transformFile(firFileFromPsi, null).single) }.toString()

            if (treeFromLightTree != treeFromPsi) {
                errorCounter++
                //TestCase.assertEquals(treeFromPsi, treeFromLightTree)
            }
            counter++
        }
        println("All scanned files: $counter")
        println("Files that aren't equal to FIR: $errorCounter")
    }

    fun testStubCompareAll() {
        compare(stubMode = true)
    }

    fun testCompareAll() {
        compare(stubMode = false)
    }

    fun testStubCompareWithoutAnnotations() {
        compare(stubMode = true, visitAnnotation = false)
    }
}
