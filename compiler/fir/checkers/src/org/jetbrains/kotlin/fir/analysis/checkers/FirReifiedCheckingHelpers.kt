/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.chooseFactory
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

fun checkIfSuitableForReifiedTypeParameterAndReport(
    typeArgument: ConeKotlinType?,
    source: KtSourceElement,
    context: CheckerContext,
    reporter: DiagnosticReporter
) =
    checkIfSuitableForReifiedTypeParameterAndReport(typeArgument, source, isArrayTypeArgument = false, context, reporter)

private fun checkIfSuitableForReifiedTypeParameterAndReport(
    typeArgument: ConeKotlinType?,
    source: KtSourceElement,
    isArrayTypeArgument: Boolean,
    context: CheckerContext,
    reporter: DiagnosticReporter
) {
    if (typeArgument?.classId == StandardClassIds.Array) {
        checkIfSuitableForReifiedTypeParameterAndReport(typeArgument.typeArguments[0].type, source, true, context, reporter)
        return
    }

    if (typeArgument is ConeTypeParameterType) {
        val factory = if (isArrayTypeArgument) {
            FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY.chooseFactory(context)
        } else {
            FirErrors.TYPE_PARAMETER_AS_REIFIED
        }
        val symbol = typeArgument.lookupTag.typeParameterSymbol
        if (!symbol.isReified) {
            reporter.reportOn(source, factory, symbol, context)
        }
    } else if (typeArgument != null && typeArgument.cannotBeReified()) {
        reporter.reportOn(source, FirErrors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, typeArgument, context)
        return
    }
}

private fun ConeKotlinType.cannotBeReified(): Boolean {
    return this.isNothing || this.isNullableNothing || this is ConeCapturedType
}