/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.psi

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.compiler.visualizer.PsiVisualizer
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.visualizer.AbstractVisualizer
import java.io.File

abstract class AbstractPsiVisualizer : AbstractVisualizer() {
    override fun doVisualizerTest(file: File, environment: KotlinCoreEnvironment) {
        val ktFiles = environment.getSourceFiles()
        val analysisResult = JvmResolveUtil.analyze(ktFiles, environment)

        val renderer = PsiVisualizer(ktFiles.first(), analysisResult)
        val psiRenderResult = renderer.render()

        val expectedPath = file.absolutePath.replace(replacement.first, replacement.second)
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), psiRenderResult) {
            return@assertEqualsToFile it.replace("// FIR_IGNORE\n", "")
        }
    }
}
