/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.renderer.FirFileAnnotationsContainerRenderer
import org.jetbrains.kotlin.fir.renderer.FirPackageDirectiveRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractStdLibBasedGetOrBuildFirTest : AbstractLowLevelApiSingleFileTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val caretPosition = testServices.expressionMarkerProvider.getCaretPosition(ktFile)
        val ktReference = ktFile.findReferenceAt(caretPosition) ?: testServices.assertions.fail { "No references at caret found" }
        val declaration =
            analyseForTest(ktReference.element as KtElement) {
                ktReference.resolve() as KtDeclaration
            }

        val resolveSession = LLFirResolveSessionService.getInstance(ktFile.project).getFirResolveSession(ktFile.getKtModule())
        val fir = resolveSession.resolveToFirSymbol(declaration, FirResolvePhase.BODY_RESOLVE).fir

        val renderedFir = FirRenderer(
            fileAnnotationsContainerRenderer = FirFileAnnotationsContainerRenderer(),
            packageDirectiveRenderer = FirPackageDirectiveRenderer(),
            resolvePhaseRenderer = FirResolvePhaseRenderer(),
        ).renderElementAsString(fir).trimEnd()
        val actual = """|KT element: ${declaration::class.simpleName}
               |FIR element: ${fir::class.simpleName}
               |FIR source kind: ${fir.source?.kind?.let { it::class.simpleName }}
               |
               |FIR element rendered:
               |$renderedFir""".trimMargin()
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}