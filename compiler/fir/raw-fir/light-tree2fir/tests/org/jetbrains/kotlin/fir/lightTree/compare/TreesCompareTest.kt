/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.compare

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.StubFirScopeProvider
import org.jetbrains.kotlin.fir.builder.test.COMPILER_DIAGNOSTICS_TEST_DATA_DIRECTORY
import org.jetbrains.kotlin.fir.builder.test.splitTestDataIntoFiles
import org.jetbrains.kotlin.fir.builder.test.toStrippedCompilerDiagnosticsTestDataFiles
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.test.util.walkRepositoryKotlinFilesWithoutTestData
import org.jetbrains.kotlin.test.util.walkRepositoryKotlinFilesWithTestData
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.utils.isCustomTestData
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class TreesCompareTest : AbstractRawFirBuilderTestCase() {
    companion object {
        private val DIAGNOSTIC_IN_TESTDATA_PATTERN = Regex("<!>|<!(.*?(\\(\".*?\"\\)|\\(\\))??)+(?<!<)!>")
    }

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
            path.walkRepositoryKotlinFilesWithoutTestData(onEachFile)
        } else {
            path.walkRepositoryKotlinFilesWithTestData(onEachFile)
        }
        println("All scanned files: $counter")
        println("Files that aren't equal to FIR: $errorCounter")
        if (errorCounter > 0) {
            println(differentFiles)
        }
        TestCase.assertEquals(0, errorCounter)
    }

    private fun compareAll() {
        @OptIn(ObsoleteTestInfrastructure::class)
        val session = FirSessionFactoryHelper.createEmptySession()

        val lightTreeConverter = LightTree2Fir(
            session = session,
            scopeProvider = StubFirScopeProvider,
            diagnosticsReporter = null
        )
        compareBase(System.getProperty("user.dir"), withTestData = false) { file ->
            val (text, linesMapping) = file.inputStream().reader(Charsets.UTF_8).use {
                it.readSourceFileWithMapping()
            }
            splitTestDataIntoFiles(file.path, text.toString().trim()).forEach { pair ->
                val (filePath, fileText) = pair

                //psi
                val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileText) as KtFile
                val firFileFromPsi = ktFile.toFirFile(session)
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
        @OptIn(ObsoleteTestInfrastructure::class)
        val session = FirSessionFactoryHelper.createEmptySession()

        val lightTreeConverter = LightTree2Fir(
            session = session,
            scopeProvider = StubFirScopeProvider,
            diagnosticsReporter = null
        )
        compareBase(COMPILER_DIAGNOSTICS_TEST_DATA_DIRECTORY, withTestData = true) { file ->
            if (file.isCustomTestData) return@compareBase true

            file.toStrippedCompilerDiagnosticsTestDataFiles()?.forEach { (filePath, fileText) ->
                //psi
                val fileName = PathUtil.getFileName(filePath)
                val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(fileName), fileText) as KtFile
                val firFileFromPsi = ktFile.toFirFile(session)
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
}
