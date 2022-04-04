/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.name.FqName

object TestReferenceResolveResultRenderer {
    fun KtAnalysisSession.renderResolvedTo(
        symbols: List<KtSymbol>,
        renderingOptions: KtDeclarationRendererOptions = KtDeclarationRendererOptions.DEFAULT
    ) =
        symbols.map { renderResolveResult(it, renderingOptions) }
            .sorted()
            .withIndex()
            .joinToString(separator = "\n") { "${it.index}: ${it.value}" }

    private fun KtAnalysisSession.renderResolveResult(
        symbol: KtSymbol,
        renderingOptions: KtDeclarationRendererOptions
    ): String {
        return buildString {
            symbolContainerFqName(symbol)?.let { fqName ->
                append("(in $fqName) ")
            }
            when (symbol) {
                is KtDeclarationSymbol -> append(symbol.render(renderingOptions))
                is KtPackageSymbol -> append("package ${symbol.fqName}")
                is KtReceiverParameterSymbol -> {
                    append("extension receiver with type ")
                    append(symbol.type.render(renderingOptions.typeRendererOptions))
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
