/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.fir

import com.intellij.openapi.extensions.Extensions
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.compiler.visualizer.FirVisualizer
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.createSession
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.visualizer.AbstractVisualizer
import org.junit.Assert
import java.io.File

abstract class AbstractFirVisualizer : AbstractVisualizer() {
    override fun doVisualizerTest(file: File, environment: KotlinCoreEnvironment) {
        Extensions.getArea(environment.project)
            .getExtensionPoint(PsiElementFinder.EP_NAME)
            .unregisterExtension(JavaElementFinder::class.java)

        val ktFiles = environment.getSourceFiles()
        val scope = GlobalSearchScope.filesScope(environment.project, ktFiles.mapNotNull { it.virtualFile })
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(environment.project))
        val session = createSession(environment, scope)

        val firProvider = (session.firProvider as FirProviderImpl)
        val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider, stubMode = false)

        val transformer = FirTotalResolveTransformer()
        val firFiles = ktFiles.map {
            val firFile = builder.buildFirFile(it)
            firProvider.recordFile(firFile)
            firFile
        }.also {
            try {
                transformer.processFiles(it)
            } catch (e: Exception) {
                it.forEach { println(it.render()) }
                throw e
            }
        }

        //compare
        val renderer = FirVisualizer(firFiles.first())
        val psiRenderResult = renderer.render()

        val expectedPath = file.absolutePath.replace(replacement.first, replacement.second)
        val expectedText = File(expectedPath).readLines()
        if (expectedText[0].startsWith("// FIR_IGNORE")) {
            Assert.assertFalse(
                "Files are identical, please delete ignore directive",
                expectedText.filterIndexed { index, _ -> index > 0 }.joinToString("\n") == psiRenderResult
            )
            return
        }
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), psiRenderResult) {
            return@assertEqualsToFile it.replace("// FIR_IGNORE\n", "")
        }

    }
}
