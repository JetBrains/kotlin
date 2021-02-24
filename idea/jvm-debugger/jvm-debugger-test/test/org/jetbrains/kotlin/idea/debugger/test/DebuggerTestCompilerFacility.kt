/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.doWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.findMainClass
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.resolve.getLanguageVersionSettings
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinBaseTest.TestFile
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import java.io.File

class DebuggerTestCompilerFacility(
    files: List<TestFile>,
    private val jvmTarget: JvmTarget,
    private val useIrBackend: Boolean
) {
    private val kotlinStdlibPath = ForTestCompileRuntime.runtimeJarForTests().absolutePath

    private val mainFiles: TestFilesByLanguage
    private val libraryFiles: TestFilesByLanguage
    private val mavenArtifacts = mutableListOf<String>()

    init {
        val splitFiles = splitByTarget(files)
        mainFiles = splitByLanguage(splitFiles.main)
        libraryFiles = splitByLanguage(splitFiles.library)
    }

    fun compileExternalLibrary(name: String, srcDir: File, classesDir: File) {
        val libSrcPath = File(DEBUGGER_TESTDATA_PATH_BASE, "lib/$name")
        if (!libSrcPath.exists()) {
            error("Library $name does not exist")
        }

        val testFiles = libSrcPath.walk().filter { it.isFile }.toList().map {
            val path = it.toRelativeString(libSrcPath)
            TestFile(path, FileUtil.loadFile(it, true))
        }

        val libraryFiles = splitByLanguage(testFiles)
        compileLibrary(libraryFiles, srcDir, classesDir)
    }

    fun addDependencies(libraryPaths: List<String>) {
        for (libraryPath in libraryPaths) {
            mavenArtifacts.add(libraryPath)
        }
    }

    fun kotlinStdlibInMavenArtifacts() =
        mavenArtifacts.find { it.contains(Regex("""kotlin-stdlib-\d+\.\d+\.\d+(\-\w+)?""")) }

    fun compileLibrary(srcDir: File, classesDir: File) {
        compileLibrary(this.libraryFiles, srcDir, classesDir)

        srcDir.refreshAndToVirtualFile()?.let { KtUsefulTestCase.refreshRecursively(it) }
        classesDir.refreshAndToVirtualFile()?.let { KtUsefulTestCase.refreshRecursively(it) }
    }

    private fun compileLibrary(libraryFiles: TestFilesByLanguage, srcDir: File, classesDir: File) = with(libraryFiles) {
        resources.copy(classesDir)
        (kotlin + java).copy(srcDir)

        if (kotlinStdlibInMavenArtifacts() == null)
            mavenArtifacts.add(kotlinStdlibPath)

        val options = mutableListOf("-jvm-target", jvmTarget.description)

        if (!useIrBackend) {
            options.add("-Xuse-old-backend")
        }

        if (kotlin.isNotEmpty()) {
            MockLibraryUtil.compileKotlin(
                srcDir.absolutePath,
                classesDir,
                options,
                *(mavenArtifacts.toTypedArray())
            )
        }

        if (java.isNotEmpty()) {
            CodegenTestUtil.compileJava(
                java.map { File(srcDir, it.name).absolutePath },
                mavenArtifacts + classesDir.absolutePath,
                listOf("-g"),
                classesDir,
                JUnit4Assertions
            )
        }
    }

    // Returns the qualified name of the main test class.
    fun compileTestSources(module: Module, srcDir: File, classesDir: File, libClassesDir: File): String = with(mainFiles) {
        resources.copy(srcDir)
        resources.copy(classesDir) // sic!
        (kotlin + java).copy(srcDir)

        val ktFiles = mutableListOf<KtFile>()

        doWriteAction {
            for (file in kotlin + java) {
                val ioFile = File(srcDir, file.name)
                val virtualFile = ioFile.refreshAndToVirtualFile() ?: error("Cannot find a VirtualFile instance for file $file")
                val psiFile = PsiManager.getInstance(module.project).findFile(virtualFile) ?: continue

                if (psiFile is KtFile) {
                    ktFiles += psiFile
                }
            }
        }

        if (ktFiles.isEmpty()) {
            error("No Kotlin files found")
        }

        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(classesDir)
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(libClassesDir)

        lateinit var mainClassName: String

        doWriteAction {
            mainClassName = compileKotlinFilesInIde(module, ktFiles, classesDir)
        }

        if (java.isNotEmpty()) {
            CodegenTestUtil.compileJava(
                java.map { File(srcDir, it.name).absolutePath },
                getClasspath(module) + listOf(classesDir.absolutePath),
                listOf("-g"),
                classesDir,
                JUnit4Assertions
            )
        }

        return mainClassName
    }

    private fun compileKotlinFilesInIde(module: Module, files: List<KtFile>, classesDir: File): String {
        val project = module.project
        val resolutionFacade = KotlinCacheService.getInstance(project).getResolutionFacade(files)

        val analysisResult = resolutionFacade.analyzeWithAllCompilerChecks(files)
        analysisResult.throwIfError()

        val configuration = CompilerConfiguration()
        configuration.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
        configuration.put(JVMConfigurationKeys.IR, useIrBackend)
        configuration.put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)

        val state = GenerationUtils.generateFiles(project, files, configuration, ClassBuilderFactories.BINARIES, analysisResult) {
            generateDeclaredClassFilter(GenerationState.GenerateClassFilter.GENERATE_ALL)
        }

        val extraDiagnostics = state.collectedExtraJvmDiagnostics
        if (!extraDiagnostics.isEmpty()) {
            val compoundMessage = extraDiagnostics.joinToString("\n") { DefaultErrorMessages.render(it) }
            error("One or more errors occurred during code generation: \n$compoundMessage")
        }

        state.factory.writeAllTo(classesDir)

        return findMainClass(analysisResult.bindingContext, resolutionFacade.getLanguageVersionSettings(), files)?.asString()
            ?: error("Cannot find main class name")
    }

    private fun getClasspath(module: Module): List<String> {
        val moduleRootManager = ModuleRootManager.getInstance(module)
        val classpath = moduleRootManager.orderEntries.filterIsInstance<LibraryOrderEntry>()
            .flatMap { it.library?.rootProvider?.getFiles(OrderRootType.CLASSES)?.asList().orEmpty() }

        val paths = mutableListOf<String>()
        for (entry in classpath) {
            val fileSystem = entry.fileSystem
            if (fileSystem is ArchiveFileSystem) {
                val localFile = fileSystem.getLocalByEntry(entry) ?: continue
                paths += localFile.path
            } else if (fileSystem is LocalFileSystem) {
                paths += entry.path
            }
        }

        return paths
    }
}

private fun File.refreshAndToVirtualFile(): VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(this)

private fun List<TestFile>.copy(destination: File) {
    for (file in this) {
        val target = File(destination, file.name)
        target.parentFile.mkdirs()
        target.writeText(file.content)
    }
}

class TestFilesByTarget(val main: List<TestFile>, val library: List<TestFile>)

class TestFilesByLanguage(val kotlin: List<TestFile>, val java: List<TestFile>, val resources: List<TestFile>)

private fun splitByTarget(files: List<TestFile>): TestFilesByTarget {
    val main = mutableListOf<TestFile>()
    val lib = mutableListOf<TestFile>()

    for (file in files) {
        val container = if (file.name.startsWith("lib/") || file.name.startsWith("customLib/")) lib else main
        container += file
    }

    return TestFilesByTarget(main = main, library = lib)
}

private fun splitByLanguage(files: List<TestFile>): TestFilesByLanguage {
    val kotlin = mutableListOf<TestFile>()
    val java = mutableListOf<TestFile>()
    val resources = mutableListOf<TestFile>()

    for (file in files) {
        @Suppress("MoveVariableDeclarationIntoWhen")
        val extension = file.name.substringAfterLast(".", missingDelimiterValue = "")

        val container = when (extension) {
            "kt", "kts" -> kotlin
            "java" -> java
            else -> resources
        }

        container += file
    }

    return TestFilesByLanguage(kotlin = kotlin, java = java, resources = resources)
}
