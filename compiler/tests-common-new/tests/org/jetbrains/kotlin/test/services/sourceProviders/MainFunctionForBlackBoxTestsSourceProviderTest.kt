/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.SourceFileProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MainFunctionForBlackBoxTestsSourceProviderTest {
    @Test
    fun `given transformed source when inspecting a box file then every property uses transformed content`() {
        val file = testFile("test.kt", "package original\nfun notBox(): String = \"OK\"")
        val transformedContent = "package transformed\nsuspend fun box(): String = \"OK\""
        val sourceFileProvider = StubSourceFileProvider(transformedContent)

        assertFalse(
            MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(
                file,
                SourceContentView.ORIGINAL,
                sourceFileProvider,
            )
        )
        assertTrue(
            MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(
                file,
                SourceContentView.TRANSFORMED,
                sourceFileProvider,
            )
        )
        assertEquals(
            "original",
            MainFunctionForBlackBoxTestsSourceProvider.detectPackage(
                file,
                SourceContentView.ORIGINAL,
                sourceFileProvider,
            ),
        )
        assertEquals(
            "transformed",
            MainFunctionForBlackBoxTestsSourceProvider.detectPackage(
                file,
                SourceContentView.TRANSFORMED,
                sourceFileProvider,
            ),
        )
        assertTrue(
            MainFunctionForBlackBoxTestsSourceProvider.containsSuspendBoxMethod(
                file,
                SourceContentView.TRANSFORMED,
                sourceFileProvider,
            ),
        )
    }

    @Test
    fun `given no transformed source when inspecting a file then original content remains the fallback`() {
        val file = testFile("test.kt", "package original\nfun box(): String = \"OK\"")

        assertTrue(MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(file))
        assertEquals("original", MainFunctionForBlackBoxTestsSourceProvider.detectPackage(file))
        assertFalse(MainFunctionForBlackBoxTestsSourceProvider.containsSuspendBoxMethod(file))
    }

    private fun testFile(name: String, content: String): TestFile = TestFile(
        relativePath = name,
        originalContent = content,
        originalFile = File(name),
        startLineNumberInOriginalFile = 0,
        isAdditional = false,
        directives = RegisteredDirectives.Empty,
    )

    private class StubSourceFileProvider(
        private val transformedContent: String,
    ) : SourceFileProvider() {
        override val preprocessors: List<SourceFilePreprocessor> = emptyList()

        override fun getKotlinSourceDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getJavaSourceDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getAdditionalFilesDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getContentOfSourceFile(
            testFile: TestFile,
            preprocessorFilter: ((SourceFilePreprocessor) -> Boolean)?,
        ): String = transformedContent

        override fun getOrCreateRealFileForSourceFile(testFile: TestFile): File = error("Not used in this test")
    }
}
