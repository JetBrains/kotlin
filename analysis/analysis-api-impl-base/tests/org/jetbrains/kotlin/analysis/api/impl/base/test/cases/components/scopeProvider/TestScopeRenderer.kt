/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KaRendererKeywordFilter
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaErrorTypeRenderer
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeLike
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.name.Name

internal object TestScopeRenderer {
    fun renderForTests(
        analysisSession: KaSession,
        scopeContext: KaScopeContext,
        printer: PrettyPrinter,
        printPretty: Boolean = false,
        fullyPrintScope: (KaScopeKind) -> Boolean,
    ) {
        with(analysisSession) {
            with(printer) {
                appendLine("implicit receivers:")

                withIndent {
                    for (implicitReceiver in scopeContext.implicitReceivers) {
                        val type = implicitReceiver.type
                        appendLine("type: ${renderType(type, printPretty)}")
                        append("owner symbol: ").appendLine(implicitReceiver.ownerSymbol::class.simpleName)
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

    fun KaSession.renderForTests(
        scope: KaScope,
        printer: PrettyPrinter,
        printPretty: Boolean,
        additionalSymbolInfo: KaSession.(KaSymbol) -> String? = { null }
    ) {
        renderScopeMembers(scope, printer, printPretty, additionalSymbolInfo)
    }

    context (KaSession)
    private fun renderType(
        type: KaType,
        printPretty: Boolean
    ): String = prettyPrint {
        if (printPretty) {
            prettyPrintTypeRenderer.renderType(analysisSession, type, this)
        } else {
            append(debugRenderer.renderType(analysisSession, type))
        }
    }

    private fun KaSession.renderScopeContext(
        scopeContext: KaScopeContext,
        printer: PrettyPrinter,
        printPretty: Boolean,
        fullyPrintScope: (KaScopeKind) -> Boolean,
    ) {
        for (scopeWithKind in scopeContext.scopes) {
            renderForTests(scopeWithKind.scope, scopeWithKind.kind, printer, printPretty, fullyPrintScope)
            printer.appendLine()
        }
    }

    private fun KaSession.renderForTests(
        scope: KaScope,
        scopeKind: KaScopeKind,
        printer: PrettyPrinter,
        printPretty: Boolean,
        fullyPrintScope: (KaScopeKind) -> Boolean,
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

    private fun KaSession.renderScopeMembers(
        scope: KaScope,
        printer: PrettyPrinter,
        printPretty: Boolean,
        additionalSymbolInfo: KaSession.(KaSymbol) -> String?,
    ) {
        fun <T : KaSymbol> List<T>.renderAll(
            symbolKind: String,
            renderPrettySymbol: KaSession.(T) -> String,
        ) = with(printer) {
            appendLine("$symbolKind: $size")
            withIndent {
                forEach {
                    appendLine(
                        if (printPretty) {
                            this@KaSession.renderPrettySymbol(it)
                        } else {
                            debugRenderer.render(analysisSession, it)
                        }
                    )
                    this@KaSession.additionalSymbolInfo(it)?.let {
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

    private fun prettyRenderPackage(symbol: KaPackageSymbol): String =
        symbol.fqName.asString()

    private fun KaSession.prettyRenderDeclaration(symbol: KaDeclarationSymbol): String =
        symbol.render(prettyPrintSymbolRenderer)

    private val debugRenderer = DebugSymbolRenderer()

    private val prettyPrintSymbolRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        annotationRenderer = annotationRenderer.with { annotationFilter = KaRendererAnnotationsFilter.NONE }
        modifiersRenderer = modifiersRenderer.with {
            keywordsRenderer = keywordsRenderer.with { keywordFilter = KaRendererKeywordFilter.NONE }
        }
    }

    private val prettyPrintTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
        errorTypeRenderer = KaErrorTypeRenderer.WITH_ERROR_MESSAGE
    }
}

/**
 * Render the names contained in the scope, provided by [KaScope.getPossibleClassifierNames] and [KaScope.getPossibleCallableNames].
 * Scope tests should not forget checking contained names, as they're a public part of the [KaScope] API.
 *
 * Note: Many scopes wouldn't work correctly if the contained name sets were broken, as these names are often the basis for the search.
 * But this is not a good reason for a lack of tests, as the scope implementation is not forced to use these name sets internally, and
 * the contained names are still part of the public API.
 */
fun PrettyPrinter.renderNamesContainedInScope(scope: KaScopeLike) {
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
