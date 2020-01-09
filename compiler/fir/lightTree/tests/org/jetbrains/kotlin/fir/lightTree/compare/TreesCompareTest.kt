/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.compare

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.StubFirScopeProvider
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.walkTopDown
import org.jetbrains.kotlin.fir.lightTree.walkTopDownWithTestData
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class TreesCompareTest : AbstractRawFirBuilderTestCase() {
    private fun compareBase(path: String, withTestData: Boolean, compareFir: (File) -> Boolean) {
        var counter = 0
        var errorCounter = 0
        val differentFiles = mutableListOf<File>()

        val onEachFile: (File) -> Unit = { file ->
            if (!compareFir(file)) {
                errorCounter++
                differentFiles += file
            }
            if (!file.name.endsWith(".fir.kt")) {
                counter++
            }
        }
        println("BASE PATH: $path")
        if (!withTestData) {
            path.walkTopDown(onEachFile)
        } else {
            path.walkTopDownWithTestData(onEachFile)
        }
        println("All scanned files: $counter")
        println("Files that aren't equal to FIR: $errorCounter")
        if (errorCounter > 0) {
            println(differentFiles)
        }
        TestCase.assertEquals(0, errorCounter)
    }

    private fun compareAll(stubMode: Boolean) {
        val lightTreeConverter = LightTree2Fir(scopeProvider = StubFirScopeProvider, stubMode = stubMode)
        compareBase(System.getProperty("user.dir"), withTestData = false) { file ->
            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()

            //psi
            val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
            val firFileFromPsi = ktFile.toFirFile(stubMode)
            val treeFromPsi = StringBuilder().also { FirRenderer(it).visitFile(firFileFromPsi) }.toString()

            //light tree
            val firFileFromLightTree = lightTreeConverter.buildFirFile(text, file.name)
            val treeFromLightTree = StringBuilder().also { FirRenderer(it).visitFile(firFileFromLightTree) }.toString()

            return@compareBase treeFromLightTree == treeFromPsi
        }
    }

    fun testCompareDiagnostics() {
        val lightTreeConverter = LightTree2Fir(scopeProvider = StubFirScopeProvider, stubMode = false)
        compareBase("compiler/testData/diagnostics/tests", withTestData = true) { file ->
            if (file.name.endsWith(".fir.kt")) {
                return@compareBase true
            }
            val notEditedText = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            val text = notEditedText.replace("(<!>)|(<!.*?!>)".toRegex(), "").replaceAfter(".java", "")

            //psi
            val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
            val firFileFromPsi = ktFile.toFirFile(stubMode = false)
            val treeFromPsi = StringBuilder().also { FirRenderer(it).visitFile(firFileFromPsi) }.toString()
                .replace("<Unsupported LValue.*?>".toRegex(), "<Unsupported LValue>")

            //light tree
            val firFileFromLightTree = lightTreeConverter.buildFirFile(text, file.name)
            val treeFromLightTree = StringBuilder().also { FirRenderer(it).visitFile(firFileFromLightTree) }.toString()
                .replace("<Unsupported LValue.*?>".toRegex(), "<Unsupported LValue>")

            return@compareBase treeFromLightTree == treeFromPsi
        }
    }

    fun testStubCompareAll() {
        compareAll(stubMode = true)
    }

    fun testCompareAll() {
        compareAll(stubMode = false)
    }
}
