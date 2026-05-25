/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.imports.KaExplicitImport
import org.jetbrains.kotlin.analysis.api.imports.KaImport
import org.jetbrains.kotlin.analysis.api.imports.KaStarImport
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

@OptIn(KaExperimentalApi::class)
abstract class AbstractFileImportsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val rendered = render(this@copyAwareAnalyzeForTest, contextFile)
            testServices.assertions.assertEqualsToTestOutputFile(rendered)
        }
    }

    private fun render(session: KaSession, contextFile: KtFile): String = prettyPrint {
        with(session) {
            appendLine("imports:")
            withIndent {
                val imports = contextFile.imports
                if (imports.isEmpty()) {
                    appendLine("<none>")
                } else {
                    for (import in imports) {
                        renderImport(contextFile, import)
                    }
                }
            }
        }
    }

    context(_: KaSession)
    private fun PrettyPrinter.renderImport(contextFile: KtFile, import: KaImport) {
        val kind = when (import) {
            is KaExplicitImport -> "KaExplicitImport"
            is KaStarImport -> "KaStarImport"
        }
        appendLine("$kind:")
        withIndent {
            appendLine("importedFqName: ${import.importedFqName?.asString()}")
            when (import) {
                is KaExplicitImport -> {
                    appendLine("aliasName: ${import.aliasName?.asString()}")
                    appendLine("importedName: ${import.importedName?.asString()}")
                    renderSymbol(contextFile, "classifierSymbol", import.classifierSymbol)
                    renderCallableSymbols(contextFile, import.callableSymbols)
                }
                is KaStarImport -> {
                    renderSymbol(contextFile, "classifierSymbol", import.classifierSymbol)
                }
            }
        }
    }

    context(session: KaSession)
    private fun PrettyPrinter.renderSymbol(contextFile: KtFile, label: String, symbol: KaSymbol?) {
        if (symbol == null) {
            appendLine("$label: null")
            return
        }
        appendLine("$label:")
        withIndent {
            appendLine(stringRepresentation(symbol))
            appendLine("importAlias: ${with(session) { contextFile.importAlias(symbol) }?.asString()}")
        }
    }

    context(session: KaSession)
    private fun PrettyPrinter.renderCallableSymbols(contextFile: KtFile, symbols: List<KaSymbol>) {
        if (symbols.isEmpty()) {
            appendLine("callableSymbols: <none>")
            return
        }
        appendLine("callableSymbols: ${symbols.size}")
        withIndent {
            // The list order is not specified by the API; sort by debug representation for stability.
            symbols
                .map { it to stringRepresentation(it) }
                .sortedBy { it.second }
                .forEach { (symbol, repr) ->
                    appendLine(repr)
                    withIndent {
                        appendLine("importAlias: ${with(session) { contextFile.importAlias(symbol) }?.asString()}")
                    }
                }
        }
    }
}
