/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirDeprecationChecker
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*

object FirDeprecatedTypeChecker : FirTypeRefChecker() {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = typeRef.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        val resolved = typeRef.coneTypeSafe<ConeClassLikeType>() ?: return
        checkType(resolved, null, source, context, reporter, symbolToIgnore = null)
    }

    private fun checkType(
        type: ConeClassLikeType,
        typeAliasSymbol: FirTypeAliasSymbol?,
        source: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        symbolToIgnore: FirClassLikeSymbol<*>?,
    ) {
        val symbol = type.lookupTag.toSymbol(context.session) ?: return
        reportDeprecationsRecursively(symbol, typeAliasSymbol, source, context, reporter, symbolToIgnore)
    }

    @OptIn(SymbolInternals::class)
    private fun reportDeprecationsRecursively(
        symbol: FirClassLikeSymbol<*>,
        // If not-null, TYPEALIAS_EXPANSION_DEPRECATION will be reported instead of DEPRECATION.
        typeAliasSymbol: FirTypeAliasSymbol?,
        source: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        symbolToIgnore: FirClassLikeSymbol<*>?,
    ) {
        if (symbol == symbolToIgnore) return

        FirDeprecationChecker.reportApiStatusIfNeeded(source, symbol, context, reporter, typealiasSymbol = typeAliasSymbol)
        if (symbol is FirTypeAliasSymbol) {
            val typeAlias = symbol.fir
            typeAlias.lazyResolveToPhase(FirResolvePhase.TYPES)
            typeAlias.expandedTypeRef.coneType.forEachType {
                if (it is ConeClassLikeType) checkType(it, symbol, source, context, reporter, symbolToIgnore)
            }
        }
    }

    /**
     * Reports deprecations on [symbol]. If [symbol] is a typealias, deprecations will be reported on the expansions recursively.
     *
     * @param symbolToIgnore If equal to [symbol], no deprecations on the [symbol] and its expansions will be reported.
     * It's passed to recursive calls and can be used to only report deprecations on a typealias and its intermediary expansions
     * but not on the final expansion if set to the fully expanded class symbol.
     */
    fun reportDeprecationsRecursively(
        symbol: FirClassLikeSymbol<*>,
        source: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        symbolToIgnore: FirClassLikeSymbol<*>?,
    ) {
        reportDeprecationsRecursively(symbol, typeAliasSymbol = null, source, context, reporter, symbolToIgnore)
    }
}

