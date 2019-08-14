/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava
import org.jetbrains.kotlin.compiler.visualizer.PsiRenderer
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractPsiVisualizer: KotlinMultiFileTestWithJava<Void?, Void?>() {
    lateinit var replacement: Pair<String, String>

    override fun createTestModule(name: String): Void? = null

    override fun createTestFile(module: Void?, fileName: String?, text: String?, directives: MutableMap<String, String>?): Void? = null

    override fun doMultiFileTest(file: File, modules: MutableMap<String, ModuleAndDependencies>?, files: MutableList<Void?>) {
        val environment = createEnvironment(file)
        val ktFiles = environment.getSourceFiles()
        val analysisResult = JvmResolveUtil.analyze(ktFiles, environment)

        val renderer = PsiRenderer(ktFiles.first(), analysisResult)
        val psiRenderResult = renderer.render()

        val expectedPath = file.absolutePath.replace(replacement.first, replacement.second)
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), psiRenderResult)
    }

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
        replacement = "fir\\psi2fir" to "visualizer"
        doTest(filePath)
    }
}
