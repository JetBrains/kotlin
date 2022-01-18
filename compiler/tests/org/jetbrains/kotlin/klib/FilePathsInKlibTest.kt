/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.incremental.md5
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.generateKLib
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.util.DummyLogger
import java.io.File

class FilePathsInKlibTest : CodegenTestCase() {

    companion object {
        private const val MODULE_NAME = "M"
        private const val testDataFile = "compiler/testData/ir/klibLayout/multiFiles.kt"
    }

    private fun loadKtFiles(directory: File): List<KtFile> {
        val psiManager = PsiManager.getInstance(myEnvironment.project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val vDirectory = fileSystem.findFileByPath(directory.canonicalPath) ?: error("File not found: $directory")
        return psiManager.findDirectory(vDirectory)?.files?.map { it as KtFile } ?: error("Cannot load KtFiles")
    }

    private val runtimeKlibPath = "libraries/stdlib/js-ir/build/classes/kotlin/js/main"

    private fun analyseKtFiles(configuration: CompilerConfiguration, ktFiles: List<KtFile>): ModulesStructure {
        return prepareAnalyzedSourceModule(
            myEnvironment.project,
            ktFiles,
            configuration,
            listOf(runtimeKlibPath),
            emptyList(),
            AnalyzerWithCompilerReport(configuration),
        )
    }

    private fun produceKlib(module: ModulesStructure, destination: File) {
        generateKLib(module, irFactory = IrFactoryImpl, outputKlibPath = destination.path, nopack = false, jsOutputName = MODULE_NAME)
    }

    private fun setupEnvironment(): CompilerConfiguration {
        val configuration = CompilerConfiguration()
        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, MODULE_NAME)
        return configuration
    }

    private fun File.md5(): Long = readBytes().md5()

    private fun File.loadKlibFilePaths(denormalize: Boolean): List<String> {
        val libs = jsResolveLibraries(listOf(runtimeKlibPath, canonicalPath), emptyList(), DummyLogger).getFullList()
        val lib = libs.last()
        val fileSize = lib.fileCount()
        val extReg = ExtensionRegistryLite.newInstance()

        val result = ArrayList<String>(fileSize)

        for (i in 0 until fileSize) {
            val fileStream = lib.file(i).codedInputStream
            val fileProto = IrFile.parseFrom(fileStream, extReg)
            val fileName = fileProto.fileEntry.name

            if (denormalize) {
                result.add(fileName.replace("/", File.separator))
            } else {
                result.add(fileName)
            }
        }

        return result
    }

    private fun createTestFiles(): List<TestFile> {
        val file = File(testDataFile)
        val expectedText = KtTestUtil.doLoadFile(file)

        return createTestFilesFromFile(file, expectedText)
    }

    private fun compileKlibs(testFiles: List<TestFile>, configuration: CompilerConfiguration, workingDir: File): File {
        for (testFile in testFiles) {
            val testFileA = File(workingDir, testFile.name).also { it.parentFile.let { p -> if (!p.exists()) p.mkdirs() } }
            testFileA.writeText(testFile.content)
        }

        val ktFilesA = loadKtFiles(workingDir)
        val module = analyseKtFiles(configuration, ktFilesA)
        val artifact = File(workingDir, "$MODULE_NAME.klib")

        produceKlib(module, artifact)

        return artifact
    }

    fun testStableCompilation() {
        val testFiles = createTestFiles()

        val dirAPath = kotlin.io.path.createTempDirectory()
        val dirBPath = kotlin.io.path.createTempDirectory()

        val dirAFile = dirAPath.toFile().also { assert(it.isDirectory) }
        val dirBFile = dirBPath.toFile().also { assert(it.isDirectory) }

        try {
            val configuration = setupEnvironment()

            configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, listOf(dirAFile.canonicalPath, dirBFile.canonicalPath))

            val moduleA = compileKlibs(testFiles, configuration, dirAFile)
            val moduleB = compileKlibs(testFiles, configuration, dirBFile)

            assertEquals(moduleA.md5(), moduleB.md5())
        } finally {
            dirAFile.deleteRecursively()
            dirBFile.deleteRecursively()
        }
    }

    fun testRelativePaths() {
        val testFiles = createTestFiles()
        val workingPath = kotlin.io.path.createTempDirectory()
        val workingDirFile = workingPath.toFile().also { assert(it.isDirectory) }

        try {
            val configuration = setupEnvironment()

            configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, listOf(workingDirFile.canonicalPath))

            val artifact = compileKlibs(testFiles, configuration, workingDirFile)
            val moduleAPaths = artifact.loadKlibFilePaths(denormalize = true)
            val dirAPaths = workingDirFile.listFiles { _, name -> name.endsWith(".kt") }!!.map { it.relativeTo(workingDirFile).path }

            assertSameElements(dirAPaths, moduleAPaths)
        } finally {
            workingDirFile.deleteRecursively()
        }
    }

    fun testAbsoluteNormalizedPath() {
        val testFiles = createTestFiles()
        val workingPath = kotlin.io.path.createTempDirectory()
        val workingDirFile = workingPath.toFile().also { assert(it.isDirectory) }

        try {
            val configuration = setupEnvironment()
            configuration.put(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH, true)

            val artifact = compileKlibs(testFiles, configuration, workingDirFile)
            val modulePaths = artifact.loadKlibFilePaths(denormalize = true)
            val dirCanonicalPaths = workingDirFile.listFiles { _, name -> name.endsWith(".kt") }!!.map { it.canonicalPath }

            assertSameElements(modulePaths, dirCanonicalPaths)
        } finally {
            workingDirFile.deleteRecursively()
        }
    }

    private fun String.normalizePath(): String = replace(File.separator, "/")

    fun testUnrelatedBase() {
        val testFiles = createTestFiles()
        val workingPath = kotlin.io.path.createTempDirectory()
        val dummyPath = kotlin.io.path.createTempDirectory()

        val workingDirFile = workingPath.toFile().also { assert(it.isDirectory) }
        val dummyFile = dummyPath.toFile().also { assert(it.isDirectory) }

        try {
            val configuration = setupEnvironment()
            configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, listOf(dummyFile.canonicalPath))

            val artifact = compileKlibs(testFiles, configuration, workingDirFile)
            val modulePaths = artifact.loadKlibFilePaths(denormalize = false)
            val dirCanonicalPaths = workingDirFile.listFiles { _, name -> name.endsWith(".kt") }!!.map { it.canonicalPath }

            assertSameElements(modulePaths.map { it.normalizePath() }, dirCanonicalPaths.map { it.normalizePath() })
        } finally {
            workingDirFile.deleteRecursively()
            dummyFile.deleteRecursively()
        }
    }
}