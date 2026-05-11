/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.firRenderingOptions
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractStdLibBasedGetOrBuildFirTest : AbstractAnalysisApiBasedTest() {
    override val configurator = LLSourceLikeTestConfigurator()

    @OptIn(KtExperimentalApi::class)
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val project = mainFile.project
        assert(!project.isDisposed) { "$project is disposed" }
        val caretPosition = testServices.expressionMarkerProvider.getCaret(mainFile)
        val element = mainFile.findElementAt(caretPosition)?.parentOfType<KtElement>(true)
            ?: testServices.assertions.fail { "No element at caret found" }

        if (element !is KtResolvable) {
            testServices.assertions.fail { "Element at caret is not resolvable" }
        }

        val declaration = analyzeForTest(element) {
            element.resolveSymbol()?.psi as KtDeclaration
        }

        val resolutionFacade = LLResolutionFacadeService.getInstance(project).getResolutionFacade(mainModule.ktModule)
        val fir = declaration.resolveToFirSymbol(resolutionFacade).fir
        testServices.assertions.assertEqualsToTestOutputFile(renderActualFir(fir, declaration, testServices.firRenderingOptions))
    }
}
