/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class TreesCompareTest: AbstractRawFirBuilderTestCase() {
    fun testCompare() {
        val path = System.getProperty("user.dir")
        val root = File(path)
        var counter = 0
        var errorCounter = 0

        val parserDefinition = KotlinParserDefinition()
        val lexer = parserDefinition.createLexer(myProject)
        val lightTreeConverter = LightTree2Fir(true, parserDefinition, lexer)

        println("BASE PATH: $path")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue

            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()

            //light tree
            val firFileFromLightTree = lightTreeConverter.buildFirFile(text, file.name)
            val treeFromLightTree = StringBuilder().also { FirRenderer(it).visitFile(firFileFromLightTree) }.toString()

            //psi
            val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
            val firFileFromPsi = ktFile.toFirFile(stubMode = true)
            val treeFromPsi = StringBuilder().also { FirRenderer(it).visitFile(firFileFromPsi) }.toString()

            if (treeFromLightTree != treeFromPsi){
                errorCounter++;
                //TestCase.assertEquals(treeFromPsi, treeFromLightTree)
            }
            counter++
        }
        println("All scanned files: $counter")
        println("Files that aren't equal to FIR: $errorCounter")
    }
}