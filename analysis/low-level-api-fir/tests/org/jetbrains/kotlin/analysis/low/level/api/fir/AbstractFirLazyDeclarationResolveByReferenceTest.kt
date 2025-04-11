/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Consider to use [AbstractFirLazyDeclarationResolveTest] instead.
 *
 * This test case may require preliminary resolution to find a declaration by reference which
 * may be unacceptable in some cases.
 */
abstract class AbstractFirLazyDeclarationResolveByReferenceTest : AbstractFirLazyDeclarationResolveOverAllPhasesTest() {
    override fun checkSession(resolutionFacade: LLResolutionFacade) {
        require(resolutionFacade.isSourceSession)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        doLazyResolveTest(
            mainFile,
            testServices,
            outputRenderingMode = OutputRenderingMode.ONLY_TARGET_DECLARATION,
        ) { resolutionFacade ->
            val position = testServices.expressionMarkerProvider.getCaret(mainFile)
            val reference = mainFile.findReferenceAt(position)
            if (reference == null) {
                error("No reference found at caret")
            }

            val declaration = reference.resolve()
            if (declaration !is KtDeclaration) {
                error("Element at caret should be referencing some `${KtDeclaration::class.simpleName}`, but referenced  `${declaration?.javaClass?.simpleName}` instead")
            }

            val declarationSymbol = declaration.resolveToFirSymbol(resolutionFacade).fir
            declarationSymbol to fun(phase: FirResolvePhase) {
                declarationSymbol.lazyResolveToPhaseByDirective(phase, testServices)
            }
        }
    }
}

abstract class AbstractFirSourceLazyDeclarationResolveByReferenceTest : AbstractFirLazyDeclarationResolveByReferenceTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}
