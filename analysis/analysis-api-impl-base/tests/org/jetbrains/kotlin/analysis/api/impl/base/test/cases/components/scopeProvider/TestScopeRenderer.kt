/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtScopeContext
import org.jetbrains.kotlin.analysis.api.components.KtScopeKind
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KtRendererKeywordFilter
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KtTypeErrorTypeRenderer
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeLike
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.name.Name

internal object TestScopeRenderer {
    fun renderForTests(
        analysisSession: KtAnalysisSession,
        scopeContext: KtScopeContext,
        printer: PrettyPrinter,
        printPretty: Boolean = false,
        fullyPrintScope: (KtScopeKind) -> Boolean,
    ) {
        with(analysisSession) {
            with(printer) {
                appendLine("implicit receivers:")

                withIndent {
                    for (implicitReceiver in scopeContext.implicitReceivers) {
                        val type = implicitReceiver.type
                        appendLine("type: ${renderType(type, printPretty)}")
                        appendLine("owner symbol: ${implicitReceiver.ownerSymbol::class.simpleName}")
                        appendLine()
                    }
                }
                appendLine("scopes:")
                withIndent {
                    renderScopeContext(scopeContext, printer, printPretty, fullyPrintScope)
                }
            }
        }
    }

    fun KtAnalysisSession.renderForTests(
        scope: KtScope,
        printer: PrettyPrinter,
        printPretty: Boolean,
        additionalSymbolInfo: KtAnalysisSession.(KtSymbol) -> String? = { null }
    ) {
        renderScopeMembers(scope, printer, printPretty, additionalSymbolInfo)
    }

    context (KtAnalysisSession)
    private fun renderType(
        type: KtType,
        printPretty: Boolean
    ): String = prettyPrint {
        if (printPretty) {
            prettyPrintTypeRenderer.renderType(analysisSession, type, this)
        } else {
            append(debugRenderer.renderType(analysisSession, type))
        }
    }

    private fun KtAnalysisSession.renderScopeContext(
        scopeContext: KtScopeContext,
        printer: PrettyPrinter,
        printPretty: Boolean,
        fullyPrintScope: (KtScopeKind) -> Boolean,
    ) {
        for (scopeWithKind in scopeContext.scopes) {
            renderForTests(scopeWithKind.scope, scopeWithKind.kind, printer, printPretty, fullyPrintScope)
            printer.appendLine()
        }
    }

    private fun KtAnalysisSession.renderForTests(
        scope: KtScope,
        scopeKind: KtScopeKind,
        printer: PrettyPrinter,
        printPretty: Boolean,
        fullyPrintScope: (KtScopeKind) -> Boolean,
    ) {
        with(printer) {
            appendLine("${scopeKind::class.simpleName}, index = ${scopeKind.indexInTower}")

            if (!fullyPrintScope(scopeKind)) {
                return
            }

            withIndent {
                renderScopeMembers(scope, printer, printPretty) { null }
            }
        }
    }

    private fun KtAnalysisSession.renderScopeMembers(
        scope: KtScope,
        printer: PrettyPrinter,
        printPretty: Boolean,
        additionalSymbolInfo: KtAnalysisSession.(KtSymbol) -> String?,
    ) {
        fun <T : KtSymbol> List<T>.renderAll(
            symbolKind: String,
            renderPrettySymbol: KtAnalysisSession.(T) -> String,
        ) = with(printer) {
            appendLine("$symbolKind: $size")
            withIndent {
                forEach {
                    appendLine(
                        if (printPretty) {
                            this@KtAnalysisSession.renderPrettySymbol(it)
                        } else {
                            debugRenderer.render(analysisSession, it)
                        }
                    )
                    this@KtAnalysisSession.additionalSymbolInfo(it)?.let {
                        withIndent { appendLine(it) }
                    }
                }
            }
        }

        scope.getPackageSymbols()
            .toMutableList()
            .apply { sortBy { it.fqName.asString() } }
            .renderAll("packages") { prettyRenderPackage(it) }
        scope.getClassifierSymbols().toList().renderAll("classifiers") { prettyRenderDeclaration(it) }
        scope.getCallableSymbols().toList().renderAll("callables") { prettyRenderDeclaration(it) }
        scope.getConstructors().toList().renderAll("constructors") { prettyRenderDeclaration(it) }
    }

    private fun prettyRenderPackage(symbol: KtPackageSymbol): String =
        symbol.fqName.asString()

    private fun KtAnalysisSession.prettyRenderDeclaration(symbol: KtDeclarationSymbol): String =
        symbol.render(prettyPrintSymbolRenderer)

    private val debugRenderer = DebugSymbolRenderer()

    private val prettyPrintSymbolRenderer = KtDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        annotationRenderer = annotationRenderer.with { annotationFilter = KtRendererAnnotationsFilter.NONE }
        modifiersRenderer = modifiersRenderer.with {
            keywordsRenderer = keywordsRenderer.with { keywordFilter = KtRendererKeywordFilter.NONE }
        }
    }

    private val prettyPrintTypeRenderer = KtTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
        typeErrorTypeRenderer = KtTypeErrorTypeRenderer.WITH_ERROR_MESSAGE
    }
}

/**
 * Render the names contained in the scope, provided by [KtScope.getPossibleClassifierNames] and [KtScope.getPossibleCallableNames].
 * Scope tests should not forget checking contained names, as they're a public part of the [KtScope] API.
 *
 * Note: Many scopes wouldn't work correctly if the contained name sets were broken, as these names are often the basis for the search.
 * But this is not a good reason for a lack of tests, as the scope implementation is not forced to use these name sets internally, and
 * the contained names are still part of the public API.
 */
fun PrettyPrinter.renderNamesContainedInScope(scope: KtScopeLike) {
    appendLine("Classifier names:")
    withIndent {
        renderSortedNames(scope.getPossibleClassifierNames())
    }
    appendLine()

    appendLine("Callable names:")
    withIndent {
        renderSortedNames(scope.getPossibleCallableNames())
    }
}

private fun PrettyPrinter.renderSortedNames(names: Set<Name>) {
    names.sorted().forEach { appendLine(it.toString()) }
}
