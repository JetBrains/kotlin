/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.fir.builder.test.COMPILER_DIAGNOSTICS_TEST_DATA_DIRECTORY
import org.jetbrains.kotlin.fir.builder.test.toStrippedCompilerDiagnosticsTestDataFiles
import org.jetbrains.kotlin.test.frontend.fir.checkDistinctSourceElements
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.util.walkRepositoryKotlinFilesWithTestData
import org.jetbrains.kotlin.test.util.walkRepositoryKotlinFilesWithoutTestData
import org.jetbrains.kotlin.test.utils.isCustomTestData
import org.junit.runner.RunWith
import java.io.File

@TestDataPath($$"$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class PsiDistinctSourceElementsTest : AbstractRawFirBuilderTestCase() {
    /**
     * Walks all Kotlin source files in the repository (excluding test data) and checks that the source elements of FIR declarations are
     * distinct via [checkDistinctSourceElements].
     *
     * Test data is excluded due to diagnostic markup in these files. Test data is instead covered by [testDiagnosticsTestData].
     */
    fun testTotalKotlin() {
        val root = File(testDataPath)

        testDataPath.walkRepositoryKotlinFilesWithoutTestData { file ->
            val ktFile = createKtFile(file.toRelativeString(root))
            val firFile = ktFile.toFirFile()

            checkDistinctSourceElements(listOf(firFile)) { _, _ -> "Duplicate source elements in '${file.toRelativeString(root)}'" }
        }
    }

    /**
     * Walks diagnostic test data files, strips diagnostic markup, and checks that the source elements of FIR declarations are distinct via
     * [checkDistinctSourceElements].
     *
     * This test covers FIR files in their raw state. See `FirDistinctSourceElementsHandler` for the handler that checks already transformed
     * FIR files during compiler frontend tests.
     */
    fun testDiagnosticsTestData() {
        COMPILER_DIAGNOSTICS_TEST_DATA_DIRECTORY.walkRepositoryKotlinFilesWithTestData { file ->
            if (file.isCustomTestData) return@walkRepositoryKotlinFilesWithTestData

            file.toStrippedCompilerDiagnosticsTestDataFiles()?.forEach { (filePath, fileText) ->
                val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileText) as KtFile
                val firFile = ktFile.toFirFile()

                checkDistinctSourceElements(listOf(firFile)) { _, _ -> "Duplicate source elements in '$filePath'" }
            }
        }
    }
}
