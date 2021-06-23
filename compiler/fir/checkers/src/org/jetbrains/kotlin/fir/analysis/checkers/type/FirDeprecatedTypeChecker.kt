/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirDeprecationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

object FirDeprecatedTypeChecker : FirTypeRefChecker() {

    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.findClosest<FirPropertyAccessor>() != null) return

        val resolved = typeRef.coneTypeSafe<ConeClassLikeType>() ?: return
        val symbol = resolved.lookupTag.toSymbol(context.session) ?: return
        val referencedFir = symbol.fir

        FirDeprecationChecker.reportDeprecationIfNeeded(typeRef.source, referencedFir, null, context, reporter)
    }
    
}