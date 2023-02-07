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
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.incremental.md5
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.web.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.util.DummyLogger
import java.io.File
import java.nio.file.Path

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
        // TODO: improve API for generateIrForKlibSerialization and related functionality and remove code duplication here and in similar places in the code
        val sourceFiles = (module.mainModule as MainModule.SourceFiles).files
        val icData = module.compilerConfiguration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val (moduleFragment, _) = generateIrForKlibSerialization(
            module.project,
            sourceFiles,
            module.compilerConfiguration,
            module.webFrontEndResult.analysisResult,
            sortDependencies(module.moduleDependencies),
            icData,
            expectDescriptorToSymbol,
            IrFactoryImpl,
            verifySignatures = true
        ) {
            module.getModuleDescriptor(it)
        }

        val metadataSerializer =
            KlibMetadataIncrementalSerializer(module.compilerConfiguration, module.project, module.webFrontEndResult.hasErrors)

        generateKLib(
            module,
            outputKlibPath = destination.path,
            nopack = false,
            jsOutputName = MODULE_NAME,
            icData = icData,
            expectDescriptorToSymbol = expectDescriptorToSymbol,
            moduleFragment = moduleFragment
        ) { file ->
            metadataSerializer.serializeScope(file, module.webFrontEndResult.bindingContext, moduleFragment.descriptor)
        }
    }

    private fun setupEnvironment(): CompilerConfiguration {
        val configuration = CompilerConfiguration()
        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, MODULE_NAME)
        return configuration
    }

    private fun File.md5(): Long = readBytes().md5()

    private fun File.loadKlibFilePaths(): List<String> {
        val libs = webResolveLibraries(listOf(runtimeKlibPath, canonicalPath), DummyLogger).getFullList()
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

    private fun compileKlib(testFiles: List<TestFile>, configuration: CompilerConfiguration, workingDir: File): File {
        for (testFile in testFiles) {
            val file = File(workingDir, testFile.name).also { it.parentFile.let { p -> if (!p.exists()) p.mkdirs() } }
            file.writeText(testFile.content)
        }

        val ktFiles = loadKtFiles(workingDir)
        val module = analyseKtFiles(configuration, ktFiles)
        val artifact = File(workingDir, "$MODULE_NAME.klib")

        produceKlib(module, artifact)

        return artifact
    }

    fun testStableCompilation() {
        withTempDir { dirA ->
            withTempDir { dirB ->
                val testFiles = createTestFiles()
                val configuration = setupEnvironment()

                configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, listOf(dirA.canonicalPath, dirB.canonicalPath))

                val moduleA = compileKlib(testFiles, configuration, dirA)
                val moduleB = compileKlib(testFiles, configuration, dirB)

                assertEquals(moduleA.md5(), moduleB.md5())
            }
        }
    }

    fun testRelativePaths() {
        withTempDir { testTempDir ->
            val testFiles = createTestFiles()
            val configuration = setupEnvironment()

            configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, listOf(testTempDir.canonicalPath))

            val artifact = compileKlib(testFiles, configuration, testTempDir)
            val modulePaths = artifact.loadKlibFilePaths().map { it.replace("/", File.separator) }
            val dirPaths = testTempDir.listFiles { _, name -> name.endsWith(".kt") }!!.map { it.relativeTo(testTempDir).path }

            assertSameElements(modulePaths, dirPaths)
        }
    }

    fun testAbsoluteNormalizedPath() {
        withTempDir { testTempDir ->
            val testFiles = createTestFiles()

            val configuration = setupEnvironment()
            configuration.put(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH, true)

            val artifact = compileKlib(testFiles, configuration, testTempDir)
            val modulePaths = artifact.loadKlibFilePaths().map { it.replace("/", File.separator) }
            val dirCanonicalPaths = testTempDir.listFiles { _, name -> name.endsWith(".kt") }!!.map { it.canonicalPath }

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
                val configuration = setupEnvironment()
                configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, listOf(dummyFile.canonicalPath))

                val artifact = compileKlib(testFiles, configuration, testTempDir)
                val modulePaths = artifact.loadKlibFilePaths()
                val dirCanonicalPaths = testTempDir.listFiles { _, name -> name.endsWith(".kt") }!!.map { it.canonicalPath }

                assertSameElements(modulePaths.map { it.normalizePath() }, dirCanonicalPaths.map { it.normalizePath() })
            } finally {
                dummyFile.deleteRecursively()
            }
        }
    }

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
