/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import org.jetbrains.kotlin.analysis.test.data.manager.fakes.analysis.FakeGoldenAnalysisApiTestGenerated
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.engine.TestDescriptor
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

internal class TestMetadataFilterTest : AbstractPostDiscoveryFilterTest() {
    @TempDir
    lateinit var repositoryRoot: Path

    @Test
    fun `ClassSource matching path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `ClassSource not matching path is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/other")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `MethodSource matching combined path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/symbols")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource with partial path match is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource not matching path is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/other")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource matching path with trailing slash is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource of a file named like a directory is excluded by a trailing slash`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/symbols/")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource matching exact file path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/symbols.kt")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource with file name prefix is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/sym")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource with directory name prefix is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/analysis/ap")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `container type is always included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("nonexistent/path")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(type = TestDescriptor.Type.CONTAINER),
        )
    }

    @Test
    fun `descriptor without source is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            descriptor = descriptorWithSource(source = null),
        )
    }

    @Test
    fun `multiple includePaths - any match results in included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("nonexistent/path", "testData/analysis", "another/path")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `multiple includePaths - none match results in excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("nonexistent/path", "another/path", "third/path")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `class without TestMetadata annotation is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            descriptor = descriptorFromClass<NoMetadataClass>(),
        )
    }

    @Test
    fun `Nested class matching path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/expressions")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated.Expressions>(),
        )
    }

    @Test
    fun `Nested class not matching path is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/other")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated.Expressions>(),
        )
    }

    @Test
    fun `Method in nested class with combined path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/expressions/call")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated.Expressions::testCall),
        )
    }

    @Test
    fun `Method in nested class not matching path is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/other")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated.Expressions::testCall),
        )
    }

    // ========== testDataPath option resolution ==========

    /**
     * Creates `module/testData/dir/file.kt` under [repositoryRoot], resolves [option] with `module` as the project dir
     * and renders the resolved paths and errors, the root replaced with `<root>`.
     */
    private fun assertResolved(option: String, expected: String) {
        repositoryRoot.resolve("module/testData/dir").createDirectories().resolve("file.kt").createFile()
        val resolved = resolveTestDataPaths(option, repositoryRoot, projectDir = repositoryRoot.resolve("module"))
        val root = repositoryRoot.toAbsolutePath().normalize().toString()
        val actual = (resolved.paths.map { "path: $it" } + resolved.errors.map { "error: $it" })
            .joinToString("\n")
            .replace(root, "<root>")

        assertEquals(expected.trimIndent(), actual)
    }

    @Test
    fun `repository-relative directory and file paths resolve as is`() {
        assertResolved(
            option = "module/testData/dir, module/testData/dir/file.kt,module/testData/dir/",
            expected = """
                path: module/testData/dir
                path: module/testData/dir/file.kt
                path: module/testData/dir/
            """,
        )
    }

    @Test
    fun `file path without extension resolves`() {
        assertResolved(
            option = "module/testData/dir/file",
            expected = "path: module/testData/dir/file",
        )
    }

    @Test
    fun `absolute path inside the repository becomes relative`() {
        assertResolved(
            option = repositoryRoot.resolve("module/testData/dir").toString(),
            expected = "path: module/testData/dir",
        )
    }

    @Test
    fun `module-relative path is an error with a suggestion`() {
        assertResolved(
            option = "testData/dir",
            expected = "error: testDataPath 'testData/dir' does not exist; paths are relative to the repository root <root>. " +
                    "Did you mean 'module/testData/dir'?",
        )
    }

    @Test
    fun `nonexistent path is an error`() {
        assertResolved(
            option = "module/testData/dir/fi,module/testData/dir",
            expected = """
                path: module/testData/dir
                error: testDataPath 'module/testData/dir/fi' does not exist; paths are relative to the repository root <root>
            """,
        )
    }

    @Test
    fun `path with glob syntax is an error, not a crash`() {
        assertResolved(
            option = "module/testData/dir/fi[le",
            expected = "error: testDataPath 'module/testData/dir/fi[le' does not exist; paths are relative to the repository root <root>",
        )
    }

    @Test
    fun `path outside the repository is an error`() {
        assertResolved(
            option = "../elsewhere",
            expected = "error: testDataPath '../elsewhere' is outside the repository root <root>",
        )
    }
}
