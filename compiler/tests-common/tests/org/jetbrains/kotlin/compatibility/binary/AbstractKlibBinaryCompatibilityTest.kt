/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compatibility.binary

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
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

    private val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
    private val testGroupSuffix = "binaryCompatibility/"
    protected lateinit var workingDir: File

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

    open fun isIgnoredTest(filePath: String): Boolean = false

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
