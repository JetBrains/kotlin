/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.analysis.decompiler.stub.files.findMainTestKotlinFile
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.readText

abstract class AbstractDecompiledKnmFileTest : KotlinTestWithEnvironment() {
    abstract val knmTestSupport: KnmTestSupport

    protected abstract fun doTest(testDirectoryPath: Path)

    override fun setUp() {
        super.setUp()

        environment.projectEnvironment.environment.registerFileType(
            KlibMetaFileType, KlibMetaFileType.defaultExtension
        )
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(
            ApplicationEnvironmentDisposer.ROOT_DISPOSABLE,
            KotlinTestUtils.newConfiguration(
                ConfigurationKind.JDK_NO_RUNTIME,
                TestJdkKind.MOCK_JDK,
            ),
            EnvironmentConfigFiles.METADATA_CONFIG_FILES,
        )
    }

    fun runTest(testDirectory: String) {
        val testDirectoryPath = Paths.get(testDirectory)
        withKnmIgnoreDirective(testDirectoryPath) { doTest(testDirectoryPath) }
    }

    protected fun compileToKnmFiles(testDirectoryPath: Path): List<VirtualFile> {
        val (compilationOutputFile, outputType) = compileCommonMetadata(testDirectoryPath)
        return getKnmFiles(compilationOutputFile, outputType)
    }

    private fun compileCommonMetadata(testDirectory: Path): Pair<File, OutputType> {
        val ktFiles = Files.list(testDirectory).filter { it.extension == "kt" }.collect(Collectors.toList())
        val testKlib = KtTestUtil.tmpDir("testLibrary").resolve("library.klib")
        val additionalArgumentsFromLanguageDirectives = ktFiles.flatMap { path ->
            path.readText().let { fileText ->
                InTextDirectivesUtils.findListWithPrefixes(fileText, "// LANGUAGE: ").map { "-XXLanguage:$it" }
            }
        }

        val outputType = knmTestSupport.compileCommonMetadata(ktFiles, testKlib.toPath(), additionalArgumentsFromLanguageDirectives)
        return testKlib to outputType
    }

    private fun getKnmFiles(compilationOutput: File, outputType: OutputType): List<VirtualFile> {
        val root = extractVirtualFileRootFromCompiledMetadata(compilationOutput, outputType)
        val files = mutableListOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(
            root,
            { virtualFile -> virtualFile.isDirectory || virtualFile.name.endsWith(KLIB_METADATA_FILE_EXTENSION_WITH_DOT) },
            { virtualFile ->
                if (!virtualFile.isDirectory) {
                    files.addIfNotNull(virtualFile)
                }
                true
            })

        return files
    }

    private fun extractVirtualFileRootFromCompiledMetadata(rootFile: File, outputType: OutputType): VirtualFile {
        return when (outputType) {
            OutputType.KLIB -> {
                val path = rootFile.toPath()
                val jarFileSystem = environment.projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
                jarFileSystem.refreshAndFindFileByPath(path.absolutePathString() + "!/")!!
            }
            OutputType.UNPACKED -> {
                environment.projectEnvironment.environment.localFileSystem.findFileByIoFile(rootFile)!!
            }
        }
    }

    private fun isTestIgnored(testDirectory: Path): Boolean {
        val mainKotlinFile = findMainTestKotlinFile(testDirectory).toFile()
        return InTextDirectivesUtils.isDirectiveDefined(mainKotlinFile.readText(), "// ${knmTestSupport.ignoreDirective.name}")
    }

    private fun withKnmIgnoreDirective(testDirectory: Path, block: () -> Unit) {
        val isIgnored = isTestIgnored(testDirectory)

        try {
            block()
        } catch (e: Throwable) {
            if (isIgnored) return
            else throw e
        }

        if (isIgnored) error("The test is passing. Please, remove the `// ${knmTestSupport.ignoreDirective.name}` directive")
    }
}
