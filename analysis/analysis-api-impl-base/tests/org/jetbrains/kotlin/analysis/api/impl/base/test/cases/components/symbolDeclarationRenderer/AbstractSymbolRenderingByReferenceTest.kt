/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES_DENOTABLE
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolRenderingByReferenceTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val renderedString = executeOnPooledThreadInReadAction {
            analyseForTest(mainFile) {
                val referenceExpression = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtReferenceExpression>(mainFile)
                val ktSymbol = referenceExpression.mainReference.resolveToSymbol()
                testServices.assertions.assertNotNull(ktSymbol)
                testServices.assertions.assertTrue(ktSymbol is KaDeclarationSymbol)
                (ktSymbol as KaDeclarationSymbol).render(WITH_QUALIFIED_NAMES_DENOTABLE)
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(renderedString)
    }
}