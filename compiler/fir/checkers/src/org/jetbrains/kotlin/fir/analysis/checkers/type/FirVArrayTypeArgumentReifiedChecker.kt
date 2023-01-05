/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractArgumentsTypeRefAndSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.*

object FirVArrayTypeArgumentReifiedChecker : FirTypeRefChecker() {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!typeRef.isVArray && !typeRef.isNullableVArray) return
        val type = (typeRef as FirResolvedTypeRef).type
        val typeArgument = type.typeArguments.singleOrNull() ?: return
        val typeArgumentSource = extractArgumentsTypeRefAndSource(typeRef)?.singleOrNull()?.source ?: return
        if (typeArgument is ConeTypeParameterType) {
            val symbol = typeArgument.lookupTag.typeParameterSymbol
            if (!symbol.isReified) {
                reporter.reportOn(typeArgumentSource, FirErrors.TYPE_PARAMETER_AS_REIFIED, symbol, context)
            }
        }
    }
}