/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES_DENOTABLE
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KaClassifierBodyRenderer
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.test.framework.utils.resolveSymbolPreferringCall
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolRenderingByReferenceTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val referenceExpression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtReferenceExpression>(mainFile)

        val renderedString = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(referenceExpression) { contextReferenceExpression ->
                val ktSymbol = contextReferenceExpression.resolveSymbolPreferringCall()
                testServices.assertions.assertNotNull(ktSymbol)
                testServices.assertions.assertTrue(ktSymbol is KaDeclarationSymbol)
                (ktSymbol as KaDeclarationSymbol).render(renderer)
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(renderedString)
    }

    /**
     * Renders the members of a referenced class as well, so that the test can show the members of library and Java classes, which
     * the renderer tests by PSI cannot render. The members are sorted as in [AbstractLegacyRenderingTest].
     */
    private val renderer = WITH_QUALIFIED_NAMES_DENOTABLE.with {
        classifierBodyRenderer = KaClassifierBodyRenderer.BODY_WITH_MEMBERS
        bodyMemberScopeSorter = MembersByRenderedTextSorter
    }
}
