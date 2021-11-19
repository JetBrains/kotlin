/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolTest(configurator: FrontendApiTestConfiguratorService) : AbstractHLApiSingleFileTest(configurator) {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(SymbolTestDirectives)
        }
    }

    abstract fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): List<KtSymbol>

    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val createPointers = SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE !in module.directives
        val pointersWithRendered = analyseOnPooledThreadInReadAction(ktFile) {
            collectSymbols(ktFile, testServices).map { symbol ->
                PointerWithRenderedSymbol(
                    if (createPointers) symbol.createPointer() else null,
                    with(DebugSymbolRenderer) { renderExtra(symbol) }
                )
            }
        }

        compareResults(pointersWithRendered, testServices)

        configurator.doOutOfBlockModification(ktFile)

        if (createPointers) {
            restoreSymbolsInOtherReadActionAndCompareResults(ktFile, pointersWithRendered, testServices)
        }
    }

    private fun compareResults(
        pointersWithRendered: List<PointerWithRenderedSymbol>,
        testServices: TestServices,
    ) {
        val actual = pointersWithRendered.joinToString(separator = "\n\n") { it.rendered }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun restoreSymbolsInOtherReadActionAndCompareResults(
        ktFile: KtFile,
        pointersWithRendered: List<PointerWithRenderedSymbol>,
        testServices: TestServices,
    ) {
        val restored = analyseOnPooledThreadInReadAction(ktFile) {
            pointersWithRendered.map { (pointer, expectedRender) ->
                val restored = pointer!!.restoreSymbol()
                    ?: error("Symbol $expectedRender was not restored")
                with(DebugSymbolRenderer) { renderExtra(restored) }
            }
        }
        val actual = restored.joinToString(separator = "\n\n")
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}

private object SymbolTestDirectives : SimpleDirectivesContainer() {
    val DO_NOT_CHECK_SYMBOL_RESTORE by directive(
        description = "Symbol restoring for some symbols in current test is not supported yet",
        applicability = DirectiveApplicability.Global
    )
}

private data class PointerWithRenderedSymbol(val pointer: KtSymbolPointer<*>?, val rendered: String)
