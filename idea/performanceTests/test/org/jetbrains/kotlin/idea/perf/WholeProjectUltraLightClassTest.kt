/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import kotlin.system.measureNanoTime

// abstract so that it doesn't run in CI until known issues (JDK absence in the test project, different module names in mangled methods) are addressed
abstract class WholeProjectUltraLightClassTest : WholeProjectPerformanceTest(), WholeProjectKotlinFileProvider {

    override fun doTest(file: VirtualFile): PerFileTestResult {
        val psiFile = file.toPsiFile(project) as? KtFile ?: run {
            return PerFileTestResult(mapOf(), 0, listOf(AssertionError("PsiFile not found for $file")))
        }

        val errors = mutableListOf<Throwable>()
        val elapsed = measureNanoTime {
            try {
                UltraLightChecker.checkClassEquivalence(psiFile)
            } catch (t: Throwable) {
                t.printStackTrace()
                errors += t
            }
        }

        return PerFileTestResult(mapOf("ultraLightEquivalence" to elapsed), elapsed, errors)
    }
}