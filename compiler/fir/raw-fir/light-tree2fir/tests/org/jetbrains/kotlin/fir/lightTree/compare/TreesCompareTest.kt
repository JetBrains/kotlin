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
import org.jetbrains.kotlin.KtInMemoryTextSourceFile
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.Companion.DIAGNOSTIC_IN_TESTDATA_PATTERN
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.StubFirScopeProvider
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.walkTopDown
import org.jetbrains.kotlin.fir.lightTree.walkTopDownWithTestData
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.readSourceFileWithMapping
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.utils.isCustomTestData
import org.jetbrains.kotlin.toSourceLinesMapping
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
            if (!file.isCustomTestData) {
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

    private fun compareAll() {
        val lightTreeConverter = LightTree2Fir(
            session = FirSessionFactoryHelper.createEmptySession(),
            scopeProvider = StubFirScopeProvider,
            diagnosticsReporter = null
        )
        compareBase(System.getProperty("user.dir"), withTestData = false) { file ->
            val (text, linesMapping) = with(file.inputStream().reader(Charsets.UTF_8)) {
                this.readSourceFileWithMapping()
            }
            splitText(file.path, text.toString().trim()).forEach { pair ->
                val (filePath, fileText) = pair

                //psi
                val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileText) as KtFile
                val firFileFromPsi = ktFile.toFirFile()
                val treeFromPsi = FirRenderer().renderElementAsString(firFileFromPsi)
                    .replace("<ERROR TYPE REF:.*?>".toRegex(), "<ERROR TYPE REF>")

                //light tree
                val firFileFromLightTree = lightTreeConverter.buildFirFile(text, KtIoFileSourceFile(file), linesMapping)
                val treeFromLightTree = FirRenderer().renderElementAsString(firFileFromLightTree)
                    .replace("<ERROR TYPE REF:.*?>".toRegex(), "<ERROR TYPE REF>")

                if (treeFromLightTree != treeFromPsi) {
                    return@compareBase false
                }
            }
            return@compareBase true
        }
    }

    fun testCompareDiagnostics() {
        val lightTreeConverter = LightTree2Fir(
            session = FirSessionFactoryHelper.createEmptySession(),
            scopeProvider = StubFirScopeProvider,
            diagnosticsReporter = null
        )
        compareBase("compiler/testData/diagnostics/tests", withTestData = true) { file ->
            if (file.isCustomTestData) {
                return@compareBase true
            }
            if (file.path.replace("\\", "/") == "compiler/testData/diagnostics/tests/constantEvaluator/constant/strings.kt") {
                // `DIAGNOSTIC_IN_TESTDATA_PATTERN` fails to correctly strip diagnostics from this file
                return@compareBase true
            }
            val notEditedText = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            val text = notEditedText.replace(DIAGNOSTIC_IN_TESTDATA_PATTERN, "").replaceAfter(".java", "")

            splitText(file.path, text).forEach { pair ->
                val (filePath, fileText) = pair
                //psi
                val fileName = PathUtil.getFileName(filePath)
                val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(fileName), fileText) as KtFile
                val firFileFromPsi = ktFile.toFirFile()
                val treeFromPsi = FirRenderer().renderElementAsString(firFileFromPsi)
                    .replace("<Unsupported LValue.*?>".toRegex(), "<Unsupported LValue>")
                    .replace("<ERROR TYPE REF:.*?>".toRegex(), "<ERROR TYPE REF>")

                //light tree
                val firFileFromLightTree =
                    lightTreeConverter.buildFirFile(
                        fileText,
                        KtInMemoryTextSourceFile(fileName, filePath, fileText),
                        fileText.toSourceLinesMapping()
                    )
                val treeFromLightTree = FirRenderer().renderElementAsString(firFileFromLightTree)
                    .replace("<Unsupported LValue.*?>".toRegex(), "<Unsupported LValue>")
                    .replace("<ERROR TYPE REF:.*?>".toRegex(), "<ERROR TYPE REF>")

                if (treeFromLightTree != treeFromPsi) {
                    return@compareBase false
                }
            }
            return@compareBase true
        }
    }

    fun testCompareAll() {
        compareAll()
    }

    private fun splitText(filePath: String, text: String): List<Pair<String, String>> {
        val fileDirective = "// FILE:"
        val idx = text.indexOf(fileDirective)
        if (idx > 0 && text[idx - 1] != '\n') {
            //try to avoid splitting of sources
            return emptyList()
        }
        if (idx >= 0) {
            val result = mutableListOf<Pair<String, String>>()
            val strings = text.drop(idx).drop(fileDirective.length).split(fileDirective)
            for (string in strings) {
                val newLineIdx = string.indexOf("\n")
                if (newLineIdx < 0) return emptyList()
                result.add(Pair(string.substring(0, newLineIdx).trim(), string.substring(newLineIdx)))
            }
            return result
        }
        return listOf(Pair(filePath, text))
    }
}
