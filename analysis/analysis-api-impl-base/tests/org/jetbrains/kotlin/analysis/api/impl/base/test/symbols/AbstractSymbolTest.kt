/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossibleMemberSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolTest(configurator: FrontendApiTestConfiguratorService) : AbstractHLApiSingleFileTest(configurator) {
    private val renderingOptions = KtDeclarationRendererOptions.DEFAULT

    open val prettyRenderMode: PrettyRenderingMode = PrettyRenderingMode.RENDER_SYMBOLS_LINE_BY_LINE

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(SymbolTestDirectives)
        }
    }

    abstract fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData

    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val createPointers = SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE !in module.directives

        val prettyRenderOptions = when (prettyRenderMode) {
            PrettyRenderingMode.RENDER_SYMBOLS_LINE_BY_LINE -> renderingOptions
            PrettyRenderingMode.RENDER_SYMBOLS_NESTED -> renderingOptions.copy(renderClassMembers = true)
        }

        val pointersWithRendered = analyseOnPooledThreadInReadAction(ktFile) {
            val (symbols, symbolForPrettyRendering) = collectSymbols(ktFile, testServices)

            val pointerWithRenderedSymbol = symbols.map { symbol ->
                PointerWithRenderedSymbol(
                    if (createPointers) symbol.createPointer() else null,
                    renderSymbolForComparison(symbol),
                )
            }

            val pointerWithPrettyRenderedSymbol = symbolForPrettyRendering.map { symbol ->
                PointerWithRenderedSymbol(
                    if (createPointers) symbol.createPointer() else null,
                    when (symbol) {
                        is KtDeclarationSymbol -> symbol.render(prettyRenderOptions)
                        is KtFileSymbol -> prettyPrint {
                            printCollection(symbol.getFileScope().getAllSymbols().asIterable(), separator = "\n\n") {
                                append((it as KtDeclarationSymbol).render(prettyRenderOptions))
                            }
                        }
                        else -> error(symbol::class.toString())
                    }
                )
            }

            SymbolPointersData(pointerWithRenderedSymbol, pointerWithPrettyRenderedSymbol)
        }

        compareResults(pointersWithRendered, testServices)

        configurator.doOutOfBlockModification(ktFile)

        if (createPointers) {
            restoreSymbolsInOtherReadActionAndCompareResults(ktFile, pointersWithRendered.pointers, testServices)
        }
    }

    private fun compareResults(
        data: SymbolPointersData,
        testServices: TestServices,
    ) {
        val actual = data.pointers.joinToString(separator = "\n\n") { it.rendered }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)

        val actualPretty = data.pointersForPrettyRendering.joinToString(separator = "\n\n") { it.rendered }
        testServices.assertions.assertEqualsToTestDataFileSibling(actualPretty, extension = ".pretty.txt")
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

                renderSymbolForComparison(restored)
            }
        }
        val actual = restored.joinToString(separator = "\n\n")
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    protected open fun KtAnalysisSession.renderSymbolForComparison(symbol: KtSymbol): String {
        return with(DebugSymbolRenderer) { renderExtra(symbol) }
    }
}

object SymbolTestDirectives : SimpleDirectivesContainer() {
    val DO_NOT_CHECK_SYMBOL_RESTORE by directive(
        description = "Symbol restoring for some symbols in current test is not supported yet",
        applicability = DirectiveApplicability.Global
    )
}

enum class PrettyRenderingMode {
    RENDER_SYMBOLS_LINE_BY_LINE,
    RENDER_SYMBOLS_NESTED,
}

data class SymbolsData(
    val symbols: List<KtSymbol>,
    val symbolsForPrettyRendering: List<KtSymbol> = symbols,
)

private data class SymbolPointersData(
    val pointers: List<PointerWithRenderedSymbol>,
    val pointersForPrettyRendering: List<PointerWithRenderedSymbol>,
)

private data class PointerWithRenderedSymbol(
    val pointer: KtSymbolPointer<*>?,
    val rendered: String,
)
