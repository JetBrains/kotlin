/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compatibility.binary

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.DFS
import java.io.File

class TestFile(val module: TestModule, fileName: String, text: String, directives: Directives)
    : KotlinBaseTest.TestFile(fileName, text, directives) {
    init {
        module.files.add(this)
    }
    val version: Int? = directives["VERSION"]?.toInt()
}

class TestModule(name: String, dependenciesSymbols: List<String>, friends: List<String>)
    : KotlinBaseTest.TestModule(name, dependenciesSymbols, friends) {

    val files = mutableListOf<TestFile>()
    val hasVersions get() = files.any { it.version != null }
    fun versionFiles(version: Int) = files.filter { it.version == null || it.version == version }
}

abstract class AbstractKlibBinaryCompatibilityTest : KotlinTestWithEnvironment() {
    abstract val extensionConfigs: EnvironmentConfigFiles
    abstract val pathToRootOutputDir: String
    private val testGroupSuffix = "binaryCompatibility/"
    protected lateinit var workingDir: File

    protected open val stdlibDependency: String? = null

    @K1Deprecation
    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), extensionConfigs)
    }

    protected fun List<TestModule>.toLibrariesArg(version: Int): String {
        val fileNames = this.map { module ->
            if (module.hasVersions) "version$version/${module.name}" else module.name
        }
        val klibs = fileNames.map { File(workingDir, "$it.${org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION}").absolutePath }
        val allDeps = stdlibDependency?.let { klibs + it } ?: klibs
        return allDeps.joinToString(File.pathSeparator)
    }

    protected fun KotlinBaseTest.TestModule.transitiveDependencies(): Set<KotlinBaseTest.TestModule> {
        val uniqueDependencies = mutableSetOf(this)
        dependencies.forEach { testModule ->
            if (testModule !in uniqueDependencies) {
                val transitiveDependencies = testModule.transitiveDependencies()
                uniqueDependencies.addAll(transitiveDependencies)
            }
        }
        return uniqueDependencies
    }

    protected fun TestModule.dependenciesToLibrariesArg(version: Int): String =
        this.dependencies
            .flatMap { it.transitiveDependencies() }
            .map { it as? TestModule ?: error("Unexpected dependency kind: $it") }
            .toLibrariesArg(version)

    protected fun TestModule.name(version: Int) = if (this.hasVersions) "version$version/${this.name}" else this.name

    protected fun createFiles(files: List<TestFile>): List<String> =
        files.map {
            val file = File(workingDir, it.name)
            file.writeText(it.content)
            file.absolutePath
        }

    protected val jsOutDir get() = workingDir.resolve("out")

    fun doTest(filePath: String) {
        workingDir = File(pathToRootOutputDir + "out/$testGroupSuffix" + filePath)
        workingDir.mkdirs()
        doTestWithIgnoringByFailFile(filePath)
    }

    fun doTestWithIgnoringByFailFile(filePath: String) {
        val failFile = File("$filePath.fail")
        try {
            doTest(filePath, "OK")
        } catch (e: Throwable) {
            if (failFile.exists()) {
                KotlinTestUtils.assertEqualsToFile(failFile, e.message ?: "")
            } else {
                throw e
            }
        }
    }

    // Const evaluation tests muted for FIR because FIR does const propagation.
    protected fun isIgnoredTest(filePath: String): Boolean {
        val fileName = filePath.substringAfterLast('/')
        return fileName == "addOrRemoveConst.kt" || fileName == "changeConstInitialization.kt"
    }

    fun doTest(filePath: String, expectedResult: String) {
        if (isIgnoredTest(filePath))
            return

        doTest(
            filePath,
            expectedResult,
            produceKlib = ::produceKlib,
            produceAndRunProgram = ::produceAndRunProgram,
        )
    }

    abstract fun produceKlib(module: TestModule, version: Int)
    abstract fun produceProgram(module: TestModule)
    abstract fun runProgram(module: TestModule, expectedResult: String)

    open fun produceAndRunProgram(module: TestModule, expectedResult: String) {
        produceProgram(module)
        runProgram(module, expectedResult)
    }

    companion object {
        const val TEST_FUNCTION = "box"
        const val DEFAULT_MODULE = "main"

        @OptIn(ObsoleteTestInfrastructure::class)
        fun doTest(
            filePath: String,
            expectedResult: String,
            produceKlib: (TestModule, Int) -> Unit,
            produceAndRunProgram: (TestModule, String) -> Unit,
        ) {
            val file = File(filePath)
            val fileContent = KtTestUtil.doLoadFile(file)

            val inputFiles = TestFiles.createTestFiles(
                file.name,
                fileContent,
                object : TestFiles.TestFileFactory<TestModule, TestFile> {
                    override fun createFile(module: TestModule?, fileName: String, text: String, directives: Directives) =
                        module?.let {
                            TestFile(module, fileName, text, directives)
                        } ?: error("Expected a module for $fileName in $filePath")

                    override fun createModule(name: String, dependencies: List<String>, friends: List<String>, dependsOn: List<String>) =
                        TestModule(name, dependencies, friends)

                },
                true,
                true
            )
            val modules = inputFiles
                .map { it.module }.distinct()
                .map { it.name to it }.toMap()

            val orderedModules = DFS.topologicalOrder(modules.values) { module ->
                module.dependenciesSymbols.mapNotNull { modules[it] }
            }

            val mainModuleName = DEFAULT_MODULE
            val mainModule = modules[mainModuleName] ?: error("No module with name \"$mainModuleName\"")

            orderedModules.reversed().filterNot { it === mainModule }.forEach {
                produceKlib(it, 1)
                if (it.hasVersions) {
                    produceKlib(it, 2)
                }
            }

            produceAndRunProgram(mainModule, expectedResult)
        }
    }
}
