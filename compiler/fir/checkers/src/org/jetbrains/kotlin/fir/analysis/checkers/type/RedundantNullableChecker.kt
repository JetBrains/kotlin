/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.SourceNavigator
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.isMarkedNullable

object RedundantNullableChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        if (
            !typeRef.coneType.abbreviatedTypeOrSelf.isMarkedNullable ||
            typeRef.source?.kind !is KtRealSourceElementKind
        ) return

        var symbol = typeRef.coneType.abbreviatedTypeOrSelf.toSymbol()
        if (symbol is FirTypeAliasSymbol) {
            while (symbol is FirTypeAliasSymbol) {
                val resolvedExpandedTypeRef = symbol.resolvedExpandedTypeRef
                if (resolvedExpandedTypeRef.coneType.isMarkedNullable) {
                    reporter.reportOn(typeRef.source, FirErrors.REDUNDANT_NULLABLE)
                    break
                } else {
                    symbol = resolvedExpandedTypeRef.toClassLikeSymbol(context.session)
                }
            }
        } else {
            with(SourceNavigator.forElement(typeRef)) {
                if (typeRef.isRedundantNullable()) {
                    reporter.reportOn(typeRef.source, FirErrors.REDUNDANT_NULLABLE)
                }
            }
        }
    }
}