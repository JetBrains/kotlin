/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.firRenderingOptions
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractStdLibBasedGetOrBuildFirTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val project = mainFile.project
        assert(!project.isDisposed) { "$project is disposed" }
        val caretPosition = testServices.expressionMarkerProvider.getCaret(mainFile)
        val ktReferences = mainFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()
        if (ktReferences.size != 1) {
            testServices.assertions.fail { "No references at caret found" }
        }
        val declaration =
            analyseForTest(ktReferences.first().element) {
                ktReferences.first().resolveToSymbol()?.psi as KtDeclaration
            }

        val resolutionFacade = LLResolutionFacadeService.getInstance(project).getResolutionFacade(mainModule.ktModule)
        val fir = declaration.resolveToFirSymbol(resolutionFacade).fir
        testServices.assertions.assertEqualsToTestOutputFile(renderActualFir(fir, declaration, testServices.firRenderingOptions))
    }
}