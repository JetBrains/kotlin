/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.fir.low.level.api.api.withFirDeclaration
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirLibraryModuleDeclarationResolveTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    override fun getProjectDescriptor(): LightProjectDescriptor {
        val pathToLibraryFiles = File(testDataPath).resolve("_library")
        assertExists(pathToLibraryFiles)

        return SdkAndMockLibraryProjectDescriptor(pathToLibraryFiles.toString(), true)
    }

    /**
     * We want to check that resolving 'compiled' PSI-elements (i.e. elements from libraries)
     * works as expected.
     *
     * Compiled PSI-elements might come from indices, for example, and we need to be able to work with them
     * and to resolve them to FIR declarations.
     */
    fun doTest(path: String) {
        val testDataFile = File(path)
        val expectedFile = File(path.removeSuffix(".kt") + ".txt")

        val ktFile = myFixture.configureByFile(testDataFile.name) as KtFile

        val caretResolutionTarget = myFixture.elementAtCaret

        require(caretResolutionTarget is KtDeclaration) {
            "Element at caret should be referencing some declaration, but referenced ${caretResolutionTarget::class} instead"
        }

        // We intentionally use ktFile here as a context element, because resolving
        // from compiled PSI-elements (e.g. caretResolutionTarget) is not yet supported
        resolveWithClearCaches(ktFile) { resolveState ->
            val renderedDeclaration = caretResolutionTarget.withFirDeclaration(resolveState) { firDeclaration ->
                firDeclaration.render(FirRenderer.RenderMode.WithResolvePhases)
            }

            KotlinTestUtils.assertEqualsToFile(expectedFile, renderedDeclaration)
        }
    }
}