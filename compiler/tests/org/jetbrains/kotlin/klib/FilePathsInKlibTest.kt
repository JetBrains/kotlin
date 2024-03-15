/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.cli.js.K2JsIrCompiler
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.incremental.md5
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.util.DummyLogger
import java.io.File
import java.nio.file.Path

class FilePathsInKlibTest : CodegenTestCase() {

    companion object {
        private const val MODULE_NAME = "M"
        private const val testDataFile = "compiler/testData/ir/klibLayout/multiFiles.kt"
    }

    private val runtimeKlibPath = "libraries/stdlib/build/classes/kotlin/js/main"

    private fun File.md5(): Long = readBytes().md5()

    private fun File.loadKlibFilePaths(): List<String> {
        val libs = CommonKLibResolver.resolve(listOf(runtimeKlibPath, canonicalPath), DummyLogger).getFullList()
        val lib = libs.last()
        val fileSize = lib.fileCount()
        val extReg = ExtensionRegistryLite.newInstance()

        val result = ArrayList<String>(fileSize)

        for (i in 0 until fileSize) {
            val fileStream = lib.file(i).codedInputStream
            val fileProto = IrFile.parseFrom(fileStream, extReg)
            val fileName = fileProto.fileEntry.name

            result.add(fileName)
        }

        return result
    }

    private fun createTestFiles(): List<TestFile> {
        val file = File(testDataFile)
        val expectedText = KtTestUtil.doLoadFile(file)

        return createTestFilesFromFile(file, expectedText)
    }

    private fun compileKlib(testFiles: List<TestFile>, extraArgs: List<String>, workingDir: File): File {
        for (testFile in testFiles) {
            val file = File(workingDir, testFile.name).also { it.parentFile.let { p -> if (!p.exists()) p.mkdirs() } }
            file.writeText(testFile.content)
        }

        val sourceFiles = walkKtFiles(workingDir)
        val artifact = File(workingDir, "$MODULE_NAME.klib")
        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JsIrCompiler(),
            sourceFiles + extraArgs + listOf(
                "-Xir-produce-klib-file",
                "-ir-output-dir", artifact.parentFile.absolutePath,
                "-ir-output-name", MODULE_NAME,
                "-libraries", runtimeKlibPath,
            )
        )

        return artifact
    }

    fun testStableCompilation() {
        withTempDir { dirA ->
            withTempDir { dirB ->
                val testFiles = createTestFiles()
                val extraArgs = listOf("-Xklib-relative-path-base=${dirA.canonicalPath},${dirB.canonicalPath}")

                val moduleA = compileKlib(testFiles, extraArgs, dirA)
                val moduleB = compileKlib(testFiles, extraArgs, dirB)

                assertEquals(moduleA.md5(), moduleB.md5())
            }
        }
    }

    fun testRelativePaths() {
        withTempDir { testTempDir ->
            val testFiles = createTestFiles()
            val extraArgs = listOf("-Xklib-relative-path-base=${testTempDir.canonicalPath}")

            val artifact = compileKlib(testFiles, extraArgs, testTempDir)
            val modulePaths = artifact.loadKlibFilePaths().map { it.replace("/", File.separator) }
            val dirPaths = walkKtFiles(testTempDir) { it.relativeTo(testTempDir).path }

            assertSameElements(modulePaths, dirPaths)
        }
    }

    fun testAbsoluteNormalizedPath() {
        withTempDir { testTempDir ->
            val testFiles = createTestFiles()
            val extraArgs = listOf("-Xklib-normalize-absolute-path")

            val artifact = compileKlib(testFiles, extraArgs, testTempDir)
            val modulePaths = artifact.loadKlibFilePaths().map { it.replace("/", File.separator) }
            val dirCanonicalPaths = walkKtFiles(testTempDir)

            assertSameElements(modulePaths, dirCanonicalPaths)
        }
    }

    private fun String.normalizePath(): String = replace(File.separator, "/")

    fun testUnrelatedBase() {
        withTempDir { testTempDir ->
            val testFiles = createTestFiles()
            val dummyPath = kotlin.io.path.createTempDirectory()
            val dummyFile = dummyPath.toFile().also { assert(it.isDirectory) }

            try {
                val extraArgs = listOf("-Xklib-relative-path-base=${dummyFile.canonicalPath}")

                val artifact = compileKlib(testFiles, extraArgs, testTempDir)
                val modulePaths = artifact.loadKlibFilePaths()
                val dirCanonicalPaths = walkKtFiles(testTempDir)

                assertSameElements(modulePaths.map { it.normalizePath() }, dirCanonicalPaths.map { it.normalizePath() })
            } finally {
                dummyFile.deleteRecursively()
            }
        }
    }

    private inline fun walkKtFiles(dir: File, crossinline convert: (File) -> String = { it.canonicalPath }): List<String> =
        dir.walk().filter { it.name.endsWith(".kt") }.map { convert(it) }.toList()

    private fun withTempDir(f: (File) -> Unit) {
        val workingPath: Path = kotlin.io.path.createTempDirectory()
        val workingDirFile = workingPath.toFile().also { assert(it.isDirectory) }
        try {
            f(workingDirFile)
        } finally {
            workingDirFile.deleteRecursively()
        }
    }
}
