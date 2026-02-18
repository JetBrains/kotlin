/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.decompiler.psi.AbstractDecompiledTextTest.Companion.validateTree
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Test
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class BuiltinsStubsTest : AbstractAnalysisApiExecutionTest("testData/builtins/stubs") {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun test(testServices: TestServices) {
        val project = testServices.ktTestModuleStructure.project
        val psiManager = PsiManager.getInstance(project)
        val builtinFiles = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles().mapNotNull { virtualFile ->
            psiManager.findFile(virtualFile)
        }

        val stubsTestEngine = CompiledStubsTestEngine
        val assertionsService = testServices.assertions

        // The set of files that are expected to be present in the test data folder
        val expectedFileNames = hashSetOf("test.kt")

        for (builtinFile in builtinFiles) {
            requireIsInstance<KtDecompiledFile>(builtinFile)
            val builtinFileName = builtinFile.name
            fun assertEqualsToFile(fileExtension: String, actual: String) {
                val expectedFile = testDataPath.resolveSibling("$builtinFileName$fileExtension")
                assertionsService.assertEqualsToFile(expectedFile = expectedFile, actual = actual)
                expectedFileNames += expectedFile.name
            }

            // Compiled stub section
            val fileStub = stubsTestEngine.compute(builtinFile)

            val fileStubText = stubsTestEngine.render(fileStub)
            assertEqualsToFile(fileExtension = ".stubs.txt", actual = fileStubText)

            stubsTestEngine.validate(testServices, builtinFile, fileStub)

            // Decompiled text section
            val decompiledText = builtinFile.text
            assertionsService.assertNotNull(decompiledText) {
                "$builtinFileName unexpectedly doesn't have a decompiled text"
            }

            assertEqualsToFile(fileExtension = ".decompiled.text.kt", actual = decompiledText!!)

            builtinFile.validateTree(assertionsService)
        }

        val unexpectedTestDataFiles = testDataPath.parent.listDirectoryEntries().filter { it.name !in expectedFileNames }
        if (unexpectedTestDataFiles.isNotEmpty()) assertionsService.fail {
            unexpectedTestDataFiles.joinToString(
                separator = "\n",
                prefix = "Some files in $testDirPathString are redundant:\n"
            ) { it.name }
        }
    }
}