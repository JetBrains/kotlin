/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.backend.native.interop.isCFunctionOrGlobalAccessor
import org.jetbrains.kotlin.fir.backend.native.interop.isObjCMethod
import org.jetbrains.kotlin.fir.declarations.isArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.isNullableString
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.resolvedType

internal object FirNativeObjCStringAsVariadicChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return
        if (symbol.valueParameterSymbols.none { it.isVararg }) return

        val isObjC = symbol.isObjCMethod(context.session)
        if (!isObjC && !symbol.isCFunctionOrGlobalAccessor(context.session)) return

        checkVarargArguments(expression, reportStringAsError = isObjC)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVarargArguments(expression: FirFunctionCall, reportStringAsError: Boolean) {
        val argumentMapping = expression.resolvedArgumentMapping ?: return
        for ([argument, parameter] in argumentMapping) {
            if (!parameter.isVararg || argument !is FirVarargArgumentsExpression) continue

            for (element in argument.arguments) {
                if (element is FirSpreadArgumentExpression) {
                    val spreadExpression = element.expression
                    if (spreadExpression is FirFunctionCall && spreadExpression.isArrayOfCall()) {
                        checkVarargArguments(spreadExpression, reportStringAsError = false)
                    }
                    continue
                }

                val type = element.resolvedType.fullyExpandedType()
                if (!type.isString && !type.isNullableString) continue

                val diagnostic = if (reportStringAsError && element.resolvedType.isString) {
                    FirNativeErrors.STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS
                } else {
                    FirNativeErrors.STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT
                }
                reporter.reportOn(element.source, diagnostic, context)
            }
        }
    }
}
