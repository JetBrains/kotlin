/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.buildtools.options.generator.determineSinceVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals

internal class DetermineSinceVersionTest {

    @TempDir
    private lateinit var tempDir: Path

    private val currentVersion = "2.4.0"
    private val className = ClassName("org.jetbrains.kotlin.buildtools.api.arguments.enums", "SomeMode")
    private val relativeFilePath = "${className.packageName.replace('.', '/')}/${className.simpleName}.kt"

    @Test
    @DisplayName("Returns currentVersion when the output file does not yet exist")
    fun newFileShouldUseCurrentVersion() {
        val result = determineSinceVersion(tempDir, currentVersion, className)

        assertEquals(currentVersion, result)
    }

    @Test
    @DisplayName("returns the @since version read from an existing file")
    fun existingFileShouldPreserveItsVersion() {
        writeFileWithSince("2.3.0")
        val result = determineSinceVersion(tempDir, currentVersion, className)
        assertEquals("2.3.0", result)
    }

    @Test
    @DisplayName("returns currentVersion when the existing file has no @since tag")
    fun existingFileWithNoSinceTagFallsBackToCurrentVersion() {
        writeFile("/** No @since annotation here. */\nclass SomeMode")
        val result = determineSinceVersion(tempDir, currentVersion, className)
        assertEquals(currentVersion, result)
    }

    @Test
    @DisplayName("returns the first @since version when there are multiple occurrences")
    fun existingFileWithMultipleSinceTagsReturnsFirst() {
        writeFile("/** @since 2.3.0 */\nfun foo() {}\n/** @since 2.4.0 */\nfun bar() {}")
        val result = determineSinceVersion(tempDir, currentVersion, className)
        assertEquals("2.3.0", result)
    }

    @Test
    @DisplayName("existing file version is preserved even when currentVersion is older")
    fun existingFileVersionPreservedRegardlessOfCurrentVersion() {
        writeFileWithSince("2.3.0")
        val result = determineSinceVersion(tempDir, "2.2.0", className)
        assertEquals("2.3.0", result)
    }

    private fun writeFileWithSince(version: String) {
        writeFile("/*\n * @since $version\n */\nenum class SomeMode { ENTRY }")
    }

    private fun writeFile(content: String) {
        val file = tempDir.resolve(relativeFilePath).toFile()
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}
