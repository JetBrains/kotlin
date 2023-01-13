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
    fun KtAnalysisSession.renderResolvedTo(
        symbols: List<KtSymbol>,
        renderer: KtDeclarationRenderer = KtDeclarationRendererForDebug.WITH_QUALIFIED_NAMES,
    ) =
        symbols.map { renderResolveResult(it, renderer) }
            .sorted()
            .withIndex()
            .joinToString(separator = "\n") { "${it.index}: ${it.value}" }

    private fun KtAnalysisSession.renderResolveResult(
        symbol: KtSymbol,
        renderer: KtDeclarationRenderer
    ): String {
        return buildString {
            symbolContainerFqName(symbol)?.let { fqName ->
                append("(in $fqName) ")
            }
            when (symbol) {
                is KtDeclarationSymbol -> append(symbol.render(renderer))
                is KtPackageSymbol -> append("package ${symbol.fqName}")
                is KtReceiverParameterSymbol -> {
                    append("extension receiver with type ")
                    append(symbol.type.render(renderer.typeRenderer, position = Variance.INVARIANT))
                }
                else -> error("Unexpected symbol ${symbol::class}")
            }
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
