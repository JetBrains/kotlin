/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.KotlinBaseTest
import java.io.File

abstract class AbstractVisualizer : KotlinMultiFileTestWithJava<KotlinBaseTest.TestModule, KotlinBaseTest.TestFile>() {
    lateinit var replacement: Pair<String, String>

    override fun createTestModule(
        name: String,
        dependencies: List<String>,
        friends: List<String>
    ): TestModule? = null


    override fun createTestFile(module: TestModule?, fileName: String, text: String, directives: Map<String, String?>): TestFile =
        TestFile(fileName, text, directives)


    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        val environment = createEnvironment(wholeFile, files)
        doVisualizerTest(wholeFile, environment)
    }

    abstract fun doVisualizerTest(file: File, environment: KotlinCoreEnvironment)

    override fun isKotlinSourceRootNeeded(): Boolean {
        return true
    }

    fun doFirBuilderDataTest(filePath: String) {
        replacement = "fir${File.separator}psi2fir" to "visualizer"
        doTest(filePath)
    }

    fun doUncommonCasesTest(filePath: String) {
        replacement = "testFiles" to "resultFiles"
        doTest(filePath)
    }
}