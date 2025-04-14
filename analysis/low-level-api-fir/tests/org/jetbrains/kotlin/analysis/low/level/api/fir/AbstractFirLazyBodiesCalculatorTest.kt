/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import junit.framework.TestCase
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractFirLazyBodiesCalculatorTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private val lazyChecker = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            TestCase.assertFalse("${FirLazyBlock::class.qualifiedName} should not present in the tree", element is FirLazyBlock)
            TestCase.assertFalse("${FirLazyExpression::class.qualifiedName} should not present in the tree", element is FirLazyExpression)
            element.acceptChildren(this)
        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        if (Directives.IGNORE_BODY_CALCULATOR in mainModule.testModule.directives) return

        withResolutionFacade(mainFile) { resolutionFacade ->
            val session = resolutionFacade.useSiteFirSession
            val provider = session.kotlinScopeProvider

            val laziedFirFile = PsiRawFirBuilder(
                session,
                provider,
                bodyBuildingMode = BodyBuildingMode.LAZY_BODIES
            ).buildFirFile(mainFile)

            FirLazyBodiesCalculator.calculateAllLazyExpressionsInFile(laziedFirFile)
            laziedFirFile.accept(lazyChecker)
            val laziedFirFileDump = FirRenderer().renderElementAsString(laziedFirFile)

            val fullFirFile = PsiRawFirBuilder(
                session,
                provider,
                bodyBuildingMode = BodyBuildingMode.NORMAL
            ).buildFirFile(mainFile)

            val fullFirFileDump = FirRenderer().renderElementAsString(fullFirFile)

            TestCase.assertEquals(/* expected = */ fullFirFileDump, /* actual = */ laziedFirFileDump)
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_BODY_CALCULATOR by stringDirective("Ignore body calculator")
    }
}

abstract class AbstractFirSourceLazyBodiesCalculatorTest : AbstractFirLazyBodiesCalculatorTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractFirOutOfContentRootLazyBodiesCalculatorTest : AbstractFirLazyBodiesCalculatorTest() {
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractFirScriptLazyBodiesCalculatorTest : AbstractFirLazyBodiesCalculatorTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
