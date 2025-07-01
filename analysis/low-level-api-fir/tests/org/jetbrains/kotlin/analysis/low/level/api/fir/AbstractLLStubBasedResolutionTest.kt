/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * The test suite is supposed to check how far the resolution process can go for stub-only files.
 */
abstract class AbstractLLStubBasedResolutionTest : AbstractLLStubBasedTest() {
    override fun doTest(astBasedFile: KtFile, stubBasedFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        withResolutionFacade(stubBasedFile) { facade ->
            val firFile = stubBasedFile.getOrBuildFirFile(facade)
            firFile.collectAllElementsOfType<FirElementWithResolveState>().forEach { element ->
                computeAstLoadingAware {
                    element.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                }
            }

            val dump = lazyResolveRenderer(StringBuilder()).renderElementAsString(firFile, trim = true)
            testServices.assertions.assertEqualsToTestOutputFile(dump, extension = ".stubBasedResolve.txt")
        }
    }
}


abstract class AbstractLLSourceStubBasedResolutionTest : AbstractLLStubBasedResolutionTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractLLScriptStubBasedResolutionTest : AbstractLLStubBasedResolutionTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
