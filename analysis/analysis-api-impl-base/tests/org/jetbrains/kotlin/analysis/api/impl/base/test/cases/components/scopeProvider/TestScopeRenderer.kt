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
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KtRendererModifierFilter
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KtTypeErrorTypeRenderer
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint

internal object TestScopeRenderer {

    context (KtAnalysisSession)
    fun PrettyPrinter.renderForTests(
        scopeContext: KtScopeContext,
        printPretty: Boolean = false,
        fullyPrintScope: (KtScopeKind) -> Boolean,
    ) {
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
            renderScopeContext(scopeContext, printPretty, fullyPrintScope)
        }
    }

    context (KtAnalysisSession)
    private fun renderType(
        type: KtType,
        printPretty: Boolean
    ): String = prettyPrint {
        if (printPretty) {
            prettyPrintTypeRenderer.renderType(type, this)
        } else {
            append(debugRenderer.renderType(type))
        }
    }

    context(KtAnalysisSession)
    private fun PrettyPrinter.renderScopeContext(
        scopeContext: KtScopeContext,
        printPretty: Boolean,
        fullyPrintScope: (KtScopeKind) -> Boolean,
    ) {
        for (scopeWithKind in scopeContext.scopes) {
            appendLine(renderForTests(scopeWithKind.scope, scopeWithKind.kind, printPretty, fullyPrintScope))
        }
    }

    context (KtAnalysisSession)
    private fun renderForTests(
        scope: KtScope,
        scopeKind: KtScopeKind,
        printPretty: Boolean,
        fullyPrintScope: (KtScopeKind) -> Boolean,
    ): String = prettyPrint {
        append("${scopeKind::class.simpleName}, index = ${scopeKind.indexInTower}")

        if (!fullyPrintScope(scopeKind)) {
            appendLine()
            return@prettyPrint
        }

        renderScopeMembers(scope, printPretty)
    }

    context (KtAnalysisSession)
    private fun PrettyPrinter.renderScopeMembers(scope: KtScope, printPretty: Boolean) {
        val callables = scope.getCallableSymbols().toList()
        val classifiers = scope.getClassifierSymbols().toList()
        val isEmpty = callables.isEmpty() && classifiers.isEmpty()
        if (isEmpty) {
            appendLine(", empty")
        } else {
            appendLine()
            withIndent {
                appendLine("classifiers: ${classifiers.size}")
                withIndent { classifiers.forEach { appendLine(TestScopeRenderer.renderSymbol(it, printPretty)) } }
                appendLine("callables: ${callables.size}")
                withIndent { callables.forEach { appendLine(TestScopeRenderer.renderSymbol(it, printPretty)) } }
            }
        }
    }

    context(KtAnalysisSession)
    private fun renderSymbol(
        symbol: KtDeclarationSymbol,
        printPretty: Boolean
    ): String = if (printPretty) symbol.render(prettyPrintSymbolRenderer) else debugRenderer.render(symbol)

    private val debugRenderer = DebugSymbolRenderer()

    private val prettyPrintSymbolRenderer = KtDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        annotationRenderer = annotationRenderer.with { annotationFilter = KtRendererAnnotationsFilter.NONE }
        modifiersRenderer = modifiersRenderer.with { modifierFilter = KtRendererModifierFilter.NONE }
    }

    private val prettyPrintTypeRenderer = KtTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
        typeErrorTypeRenderer = KtTypeErrorTypeRenderer.WITH_ERROR_MESSAGE
    }
}