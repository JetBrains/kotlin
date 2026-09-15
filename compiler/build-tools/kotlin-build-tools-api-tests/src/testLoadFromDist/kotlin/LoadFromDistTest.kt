/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines
import kotlin.io.path.writeText

@DisplayName("KotlinToolchains.loadImplementation(kotlinDist)")
class LoadFromDistTest {
    private val distRoot: Path
        get() = Path(System.getProperty("kotlin.dist.path") ?: error("kotlin.dist.path system property is not set"))

    private fun compileHelloWorld(toolchains: KotlinToolchains, workingDirectory: Path) {
        val source = workingDirectory.resolve("foo.kt").apply {
            writeText(
                """
                class Foo

                fun main() {
                    println(Foo())
                }
                """.trimIndent()
            )
        }
        val outputDirectory = workingDirectory.resolve("classes")
        val operation = toolchains.jvm.jvmCompilationOperationBuilder(listOf(source), outputDirectory).apply {
            compilerArguments[JvmCompilerArguments.NO_STDLIB] = true
            compilerArguments[JvmCompilerArguments.NO_REFLECT] = true
            compilerArguments[JvmCompilerArguments.CLASSPATH] = listOf(distRoot.resolve("kotlinc/lib/kotlin-stdlib.jar"))
        }.build()
        val result = toolchains.createBuildSession().use { session ->
            session.executeOperation(operation)
        }
        assertEquals(CompilationResult.COMPILATION_SUCCESS, result)
        assertTrue(outputDirectory.resolve("FooKt.class").exists()) {
            "Expected FooKt.class to be produced in $outputDirectory"
        }
    }

    @Test
    @DisplayName("loads a working implementation from the dist root directory")
    fun testLoadFromDistRoot(@TempDir workingDirectory: Path) {
        val toolchains = KotlinToolchains.loadImplementation(distRoot)
        assertTrue(toolchains.getCompilerVersion().isNotBlank()) { "Compiler version must not be blank" }
        compileHelloWorld(toolchains, workingDirectory)
    }

    @Test
    @DisplayName("loads a working implementation from the kotlinc directory")
    fun testLoadFromKotlincDir(@TempDir workingDirectory: Path) {
        val toolchains = KotlinToolchains.loadImplementation(distRoot.resolve("kotlinc"))
        compileHelloWorld(toolchains, workingDirectory)
    }

    @Test
    @DisplayName("the classpath metadata file lists only files present in the dist")
    fun testMetadataListsOnlyExistingFiles() {
        val kotlincDir = distRoot.resolve("kotlinc")
        val metadataFile = kotlincDir.resolve("lib/kotlin-build-tools-classpath.txt")
        assertTrue(metadataFile.isRegularFile()) { "Expected the classpath metadata file at $metadataFile" }
        val entries = metadataFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(entries.isNotEmpty()) { "The classpath metadata file must not be empty" }
        val missingEntries = entries.filterNot { kotlincDir.resolve(it).isRegularFile() }
        assertTrue(missingEntries.isEmpty()) {
            "The classpath metadata file lists files missing from the dist: $missingEntries"
        }
    }

    @Test
    @DisplayName("loads a working implementation from an incomplete dist missing optional artifacts")
    fun testLoadFromIncompleteDist(@TempDir temporaryDirectory: Path) {
        // the CRI implementation is loaded only when the CRI toolchain is actually used,
        // so a dist stripped of it must still be able to run plain JVM compilation
        val removedJar = "lib/kotlin-build-tools-cri-impl.jar"

        val kotlincDir = distRoot.resolve("kotlinc")
        val metadataFile = kotlincDir.resolve("lib/kotlin-build-tools-classpath.txt")
        val entries = metadataFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(removedJar in entries) {
            "Expected $removedJar to be listed in the classpath metadata file, but it lists only: $entries"
        }

        val incompleteDistDir = temporaryDirectory.resolve("incomplete-dist/kotlinc")
        incompleteDistDir.resolve("lib").createDirectories()
        metadataFile.copyTo(incompleteDistDir.resolve("lib/kotlin-build-tools-classpath.txt"))
        for (entry in entries) {
            if (entry == removedJar) continue
            kotlincDir.resolve(entry).copyTo(incompleteDistDir.resolve(entry))
        }

        val toolchains = KotlinToolchains.loadImplementation(incompleteDistDir)
        compileHelloWorld(toolchains, temporaryDirectory.resolve("working-dir").createDirectories())
    }

    @Test
    @DisplayName("fails with a clear error on a directory that is not a Kotlin dist")
    fun testFailsOnNonDistDirectory(@TempDir emptyDirectory: Path) {
        val exception = assertThrows<IllegalArgumentException> {
            KotlinToolchains.loadImplementation(emptyDirectory)
        }
        assertTrue("kotlin-build-tools-classpath.txt" in (exception.message ?: "")) {
            "The error message must mention the classpath metadata file, but was: ${exception.message}"
        }
    }

    @Test
    @DisplayName("fails with a clear error on a dist without the classpath metadata file")
    fun testFailsOnDistWithoutMetadataFile(@TempDir fakeDistRoot: Path) {
        fakeDistRoot.resolve("kotlinc/lib").createDirectories()
        val exception = assertThrows<IllegalArgumentException> {
            KotlinToolchains.loadImplementation(fakeDistRoot)
        }
        assertTrue(exception.message?.contains("2.5.0") == true) {
            "The error message must mention the minimum supported version, but was: ${exception.message}"
        }
    }

    @Test
    @DisplayName("fails with a clear error when all files listed in the metadata file are missing")
    fun testFailsWhenAllListedFilesAreMissing(@TempDir fakeDistRoot: Path) {
        val libDir = fakeDistRoot.resolve("kotlinc/lib").createDirectories()
        libDir.resolve("kotlin-build-tools-classpath.txt").writeText("lib/kotlin-build-tools-impl.jar\n")
        val exception = assertThrows<IllegalArgumentException> {
            KotlinToolchains.loadImplementation(fakeDistRoot)
        }
        assertTrue("does not contain any of the files" in (exception.message ?: "")) {
            "The error message must explain that no listed files are present, but was: ${exception.message}"
        }
    }
}
