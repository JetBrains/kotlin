/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.util.PathUtilRt.suggestFileName
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.js.klib.compileModuleToAnalyzedFirWithPsi
import org.jetbrains.kotlin.cli.js.klib.serializeFirKlib
import org.jetbrains.kotlin.cli.js.klib.transformFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.Companion.createForParallelTests
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.util.KtTestUtil.getHomeDirectory
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import kotlin.jvm.optionals.getOrNull
import org.jetbrains.kotlin.konan.file.File as KFile

abstract class AbstractLibraryAbiReaderTest {
    private lateinit var testName: String
    private lateinit var buildDir: File

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        val projectBuildDir = System.getenv(ENV_VAR_PROJECT_BUILD_DIR)
            ?.let { File(it) }
            ?: fail("$ENV_VAR_PROJECT_BUILD_DIR environment variable not specified")

        assertTrue(projectBuildDir.isDirectory) { "Project build dir does not exist: $projectBuildDir" }

        testName = suggestFileName(testInfo.testMethod.getOrNull()?.name ?: fail("Can't get test method name"))
        buildDir = projectBuildDir.resolve("t").resolve(testName).apply {
            deleteRecursively()
            mkdirs()
        }
    }

    fun runTest(relativePath: String) {
        val (sourceFile, dumpFile) = computeTestFiles(relativePath)

        val library = buildLibrary(sourceFile)
        val libraryAbi = LibraryAbiReader.readAbiInfo(library)

        val abiDump = libraryAbi.topLevelDeclarations.renderTopLevels(AbiRenderingSettings(setOf(AbiSignatureVersion.V1)))

        assertEqualsToFile(dumpFile, abiDump)
    }

    private fun computeTestFiles(relativePath: String): Pair<File, File> {
        val sourceFile = File(getHomeDirectory()).resolve(relativePath)
        assertEquals("kt", sourceFile.extension) { "Invalid source file: $sourceFile" }
        assertTrue(sourceFile.isFile) { "Source file does not exist: $sourceFile" }

        val dumpFile = sourceFile.withReplacedExtensionOrNull("kt", "txt")!!
        assertTrue(dumpFile.isFile) { "Dump file does not exist: $dumpFile" }

        return sourceFile to dumpFile
    }

    private fun buildLibrary(sourceFile: File): File {
        val configuration = CompilerConfiguration()
        val environment = createForParallelTests(TestDisposable(), configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, testName)
        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        configuration.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)
        configuration.put(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, ZipFileSystemCacheableAccessor(2))

        val psiManager = PsiManager.getInstance(environment.project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

        val virtualFile = fileSystem.findFileByIoFile(sourceFile) ?: error("VirtualFile for $sourceFile not found")
        val ktFiles = SingleRootFileViewProvider(psiManager, virtualFile).allFiles.filterIsInstance<KtFile>()

        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val jsStdlib = File("libraries/stdlib/js-ir/build/classes/kotlin/js/main").absoluteFile
        val dependencies = listOf(jsStdlib.absolutePath)

        val moduleStructure = ModulesStructure(
            project = environment.project,
            mainModule = MainModule.SourceFiles(ktFiles),
            compilerConfiguration = configuration,
            dependencies = dependencies,
            friendDependenciesPaths = emptyList()
        )

        val outputStream = ByteArrayOutputStream()
        val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)

        val analyzedOutput = compileModuleToAnalyzedFirWithPsi(
            moduleStructure = moduleStructure,
            ktFiles = ktFiles,
            libraries = dependencies,
            friendLibraries = emptyList(),
            diagnosticsReporter = diagnosticsReporter,
            incrementalDataProvider = null,
            lookupTracker = null
        )

        val fir2IrActualizedResult = transformFirToIr(moduleStructure, analyzedOutput.output, diagnosticsReporter)

        if (analyzedOutput.reportCompilationErrors(moduleStructure, diagnosticsReporter, messageCollector)) {
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            fail<Nothing>("The following errors occurred compiling test:\n$messages")
        }

        val libraryDir = KFile(buildDir.resolve(testName).absolutePath)
        val libraryFile = KFile(libraryDir.absolutePath + ".klib")

        serializeFirKlib(
            moduleStructure = moduleStructure,
            firOutputs = analyzedOutput.output,
            fir2IrActualizedResult = fir2IrActualizedResult,
            outputKlibPath = libraryDir.absolutePath,
            messageCollector = messageCollector,
            diagnosticsReporter = diagnosticsReporter,
            jsOutputName = testName
        )

        if (messageCollector.hasErrors()) {
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            fail<Nothing>("The following errors occurred serializing test klib:\n$messages")
        }

        libraryDir.zipDirAs(libraryFile)
        libraryDir.deleteRecursively()

        return File(libraryFile.absolutePath)
    }

    companion object {
        private const val ENV_VAR_PROJECT_BUILD_DIR = "PROJECT_BUILD_DIR"
    }
}
