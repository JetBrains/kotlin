/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.rendering.KaClassTypeQualification
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.withAdditionalVariant
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.test.framework.utils.resolveSymbolPreferringCall
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Renders the symbol that the reference at the caret resolves to with [KaRenderer], like [AbstractSymbolRenderingByReferenceTest] does
 * with the legacy renderer. The test data is shared: the output is compared to the `.new.txt` file if it exists, and to the `.txt` file
 * of the legacy test otherwise.
 */
abstract class AbstractRendererByReferenceTest : AbstractAnalysisApiBasedTest() {
    override val variantChain: TestVariantChain
        get() = super.variantChain.withAdditionalVariant("new")

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val referenceExpression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtReferenceExpression>(mainFile)

        val renderedString = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(referenceExpression) { contextReferenceExpression ->
                val symbol = checkNotNull(contextReferenceExpression.resolveSymbolPreferringCall()) {
                    "The reference at the caret is not resolved to a single symbol"
                }

                val renderer = KaRenderer.default.copy {
                    set(KaRenderingOption.ClassTypeQualification, KaClassTypeQualification.FULLY_QUALIFIED)
                    orderMembersByRenderedText()
                }

                val output = KaRenderingOutput.plainString(indentationUnit = "  ")
                renderer.render(symbol, output)
                output.toString()
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(renderedString)
    }
}
