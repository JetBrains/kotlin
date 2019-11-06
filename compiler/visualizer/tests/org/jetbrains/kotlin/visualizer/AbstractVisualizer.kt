/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractVisualizer : KotlinMultiFileTestWithJava<Void?, Void?>() {
    lateinit var replacement: Pair<String, String>

    override fun createTestModule(name: String): Void? = null

    override fun createTestFile(module: Void?, fileName: String?, text: String?, directives: MutableMap<String, String>?): Void? = null

    override fun doMultiFileTest(file: File, modules: MutableMap<String, ModuleAndDependencies>?, files: MutableList<Void?>) {
        val environment = createEnvironment(file)
        doVisualizerTest(file, environment)
    }

    abstract fun doVisualizerTest(file: File, environment: KotlinCoreEnvironment)

    override fun isKotlinSourceRootNeeded(): Boolean {
        return true
    }

    override fun getTestJdkKind(file: File): TestJdkKind {
        return TestJdkKind.FULL_JDK
    }

    override fun getConfigurationKind(): ConfigurationKind {
        return ConfigurationKind.ALL
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