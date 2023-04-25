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
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*

object FirDeprecatedTypeChecker : FirTypeRefChecker() {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = typeRef.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        val resolved = typeRef.coneTypeSafe<ConeClassLikeType>() ?: return
        checkType(resolved, null, source, context, reporter)
    }

    @OptIn(SymbolInternals::class)
    private fun checkType(
        type: ConeClassLikeType,
        typeAliasSymbol: FirTypeAliasSymbol?,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val symbol = type.lookupTag.toSymbol(context.session) ?: return
        FirDeprecationChecker.reportApiStatusIfNeeded(source, symbol, context, reporter, typealiasSymbol = typeAliasSymbol)

        if (symbol is FirTypeAliasSymbol) {
            val typeAlias = symbol.fir
            typeAlias.lazyResolveToPhase(FirResolvePhase.TYPES)
            typeAlias.expandedTypeRef.coneType.forEachType {
                if (it is ConeClassLikeType) checkType(it, symbol, source, context, reporter)
            }
        }
    }

}

