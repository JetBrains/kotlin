/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.symbols

import org.jetbrains.kotlin.idea.fir.analyseOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.fir.test.framework.AbstractKtIdeaTestWithSingleTestFileTest
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolTest : AbstractKtIdeaTestWithSingleTestFileTest() {
    abstract fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): List<KtSymbol>

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val createPointers = false //TODO !fileStructure.directives.isDirectivePresent(DIRECTIVES.DO_NOT_CHECK_SYMBOL_RESTORE)
        val pointersWithRendered = analyseOnPooledThreadInReadAction(ktFile) {
            collectSymbols(ktFile, testServices).map { symbol ->
                PointerWithRenderedSymbol(
                    if (createPointers) symbol.createPointer() else null,
                    DebugSymbolRenderer.render(symbol)
                )
            }
        }

        compareResults(pointersWithRendered, testServices)

        doOutOfBlockModification(ktFile)


        if (createPointers) {
            restoreSymbolsInOtherReadActionAndCompareResults(ktFile, pointersWithRendered, testServices)
        }
    }

    private fun compareResults(
        pointersWithRendered: List<PointerWithRenderedSymbol>,
        testServices: TestServices,
    ) {
        val actual = pointersWithRendered.joinToString(separator = "\n") { it.rendered }
        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }

    private fun restoreSymbolsInOtherReadActionAndCompareResults(
        ktFile: KtFile,
        pointersWithRendered: List<PointerWithRenderedSymbol>,
        testServices: TestServices,
    ) {
        val restored = analyseOnPooledThreadInReadAction(ktFile) {
            pointersWithRendered.map { (pointer, expectedRender) ->
                val restored = pointer!!.restoreSymbol()
                    ?: error("Symbol $expectedRender was not not restored")
                DebugSymbolRenderer.render(restored)
            }
        }
        val actual = restored.joinToString(separator = "\n")
        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }

    private fun doOutOfBlockModification(ktFile: KtFile) {
        //TODO
    }
}

private data class PointerWithRenderedSymbol(val pointer: KtSymbolPointer<*>?, val rendered: String)