/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import junit.framework.TestCase
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.utils.ignoreExceptionIfIgnoreDirectivePresent
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.contracts.FirLazyContractDescription
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.replSnippetDelegatedPropertyCopies
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractLLLazyBodiesCalculatorTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private val lazyChecker = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            TestCase.assertFalse(
                "${FirLazyBlock::class.simpleName} should not present in the tree",
                element is FirLazyBlock,
            )

            TestCase.assertFalse(
                "${FirLazyExpression::class.simpleName} should not present in the tree",
                element is FirLazyExpression,
            )

            TestCase.assertFalse(
                "${FirLazyContractDescription::class.simpleName} should not present in the tree",
                element is FirLazyContractDescription,
            )

            if (element is FirNamedFunction) {
                @OptIn(FirImplementationDetail::class)
                element.replSnippetDelegatedPropertyCopies?.values?.forEach(this::visitElement)
            }

            element.acceptChildren(this)
        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        mainModule.testModule.directives.ignoreExceptionIfIgnoreDirectivePresent(Directives.IGNORE_BODY_CALCULATOR) {
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
    }

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_BODY_CALCULATOR by stringDirective("Ignore body calculator")
    }
}

abstract class AbstractLLSourceLikeLazyBodiesCalculatorTest : AbstractLLLazyBodiesCalculatorTest() {
    override val configurator = LLSourceLikeTestConfigurator()
}

abstract class AbstractFirOutOfContentRootLazyBodiesCalculatorTest : AbstractLLLazyBodiesCalculatorTest() {
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}
