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
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

internal object FirNativeVariadicSpreadChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return
        if (symbol.valueParameterSymbols.none { it.isVararg }) return
        val session = context.session
        val isObjC = symbol.isObjCMethod(session)
        val isC = symbol.isCFunctionOrGlobalAccessor(session) && symbol.valueParameterSymbols.any { it.isVararg }

        if (isObjC || isC)
            checkVarargArguments(expression, isObjC)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVarargArguments(
        call: FirFunctionCall,
        isObjC: Boolean
    ) {
        val argumentMapping = call.resolvedArgumentMapping ?: return
        for ((argument, parameter) in argumentMapping) {
            if (parameter.isVararg) {
                (argument as? FirVarargArgumentsExpression)?.let {
                    for (element in it.arguments) {
                        if (element is FirSpreadArgumentExpression)
                            checkSpreadArgument(element, isObjC)
                    }
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpreadArgument(
        argument: FirSpreadArgumentExpression,
        isObjC: Boolean,
    ) {
        val spreadExpression = argument.expression
        if (spreadExpression is FirFunctionCall && spreadExpression.isArrayOfCall(context.session)) {
            checkVarargArguments(spreadExpression, isObjC)
        } else {
            val factory = if (isObjC) {
                FirNativeErrors.VARIADIC_OBJC_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF
            } else {
                FirNativeErrors.VARIADIC_C_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF
            }
            reporter.reportOn(argument.source, factory)
        }
    }
}
