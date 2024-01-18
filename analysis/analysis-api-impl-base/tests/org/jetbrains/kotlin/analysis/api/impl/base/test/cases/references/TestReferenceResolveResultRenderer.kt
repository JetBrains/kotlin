/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance

object TestReferenceResolveResultRenderer {
    private const val UNRESOLVED_REFERENCE_RESULT = "Nothing (Unresolved reference)"

    /**
     * Empty [symbols] list equals to unresolved reference.
     */
    fun KtAnalysisSession.renderResolvedTo(
        symbols: List<KtSymbol>,
        renderPsiClassName: Boolean = false,
        renderer: KtDeclarationRenderer = KtDeclarationRendererForDebug.WITH_QUALIFIED_NAMES,
        additionalInfo: KtAnalysisSession.(KtSymbol) -> String? = { null }
    ): String {
        if (symbols.isEmpty()) return UNRESOLVED_REFERENCE_RESULT

        return symbols.map { renderResolveResult(it, renderPsiClassName, renderer, additionalInfo) }
            .sorted()
            .withIndex()
            .joinToString(separator = "\n") { "${it.index}: ${it.value}" }
    }

    private fun KtAnalysisSession.renderResolveResult(
        symbol: KtSymbol,
        renderPsiClassName: Boolean,
        renderer: KtDeclarationRenderer,
        additionalInfo: KtAnalysisSession.(KtSymbol) -> String?
    ): String {
        return buildString {
            symbolContainerFqName(symbol)?.let { fqName ->
                append("(in $fqName) ")
            }
            when (symbol) {
                is KtDeclarationSymbol -> {
                    append(symbol.render(renderer))
                    if (renderPsiClassName) {
                        append(" (psi: ${symbol.psi?.let { it::class.simpleName }})")
                    }
                }
                is KtPackageSymbol -> append("package ${symbol.fqName}")
                is KtReceiverParameterSymbol -> {
                    append("extension receiver with type ")
                    append(symbol.type.render(renderer.typeRenderer, position = Variance.INVARIANT))
                }
                else -> error("Unexpected symbol ${symbol::class}")
            }
            additionalInfo(symbol)?.let { append(" [$it]") }
        }
    }

    @Suppress("unused")// KtAnalysisSession receiver
    private fun KtAnalysisSession.symbolContainerFqName(symbol: KtSymbol): String? {
        if (symbol is KtPackageSymbol || symbol is KtValueParameterSymbol) return null
        val nonLocalFqName = when (symbol) {
            is KtConstructorSymbol -> symbol.containingClassIdIfNonLocal?.asSingleFqName()
            is KtCallableSymbol -> symbol.callableIdIfNonLocal?.asSingleFqName()?.parent()
            is KtClassLikeSymbol -> symbol.classIdIfNonLocal?.asSingleFqName()?.parent()
            else -> null
        }
        when (nonLocalFqName) {
            null -> Unit
            FqName.ROOT -> return "ROOT"
            else -> return nonLocalFqName.asString()
        }
        val container = (symbol as? KtSymbolWithKind)?.getContainingSymbol() ?: return null
        val parents = generateSequence(container) { it.getContainingSymbol() }.toList().asReversed()
        return "<local>: " + parents.joinToString(separator = ".") { (it as? KtNamedSymbol)?.name?.asString() ?: "<no name>" }
    }
}
