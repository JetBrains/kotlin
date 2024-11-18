/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolFromPointerWithCacheTest : AbstractAnalysisApiBasedTest() {

    override fun doTest(testServices: TestServices) {
        val elementsAtCarets =
            testServices.expressionMarkerProvider.getElementsOfTypeAtCarets<KtDeclaration>(testServices)

        if (elementsAtCarets.isNotEmpty()) {
            runTest(elementsAtCarets.single().first, testServices)
        }
    }

    private fun runTest(declaration: KtDeclaration, testServices: TestServices) {
        val restoredSymbol = analyseForTest(declaration) {
            val symbol = declaration.symbol
            val pointer = symbol.createPointer()

            val originalSymbol = if (pointer is KaPsiBasedSymbolPointer) {
                // This first restore is required to cache the resulting symbol
                pointer.restoreSymbol()
            } else {
                symbol
            }

            val restoredSymbol = pointer.restoreSymbol() as? KaDeclarationSymbol

            return@analyseForTest if (restoredSymbol === originalSymbol && restoredSymbol != null) {
                DebugSymbolRenderer().render(useSiteSession, restoredSymbol)
            } else {
                NOT_RESTORED
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(restoredSymbol)
    }

    companion object {
        private const val NOT_RESTORED = "<NOT RESTORED>"
    }
}