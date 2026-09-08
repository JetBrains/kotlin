/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.varargElementType

object FirJsSuspendLambdaAsObjectCallChecker : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        when (expression) {
            is FirFunctionCall -> checkArguments(expression)
            is FirTypeOperatorCall -> checkCast(expression)
            else -> {}
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkArguments(call: FirFunctionCall) {
        // Check against the declared parameter type, not the argument's own type, so generic substitution
        // (e.g. a suspend-functional value passed as `K`) isn't flagged.
        val argumentMapping = call.resolvedArgumentMapping ?: return
        for (entry in argumentMapping) {
            val argument = entry.key
            val parameter = entry.value
            var parameterType = parameter.returnTypeRef.coneType
            if (parameter.isVararg) {
                parameterType = parameterType.varargElementType()
            }
            reportIfUnsafeSuspendFunctionalValue(argument, forcedType = parameterType)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCast(call: FirTypeOperatorCall) {
        if (call.operation != FirOperation.AS && call.operation != FirOperation.SAFE_AS) return
        val targetType = call.conversionTypeRef.coneType
        if (!targetType.isSuspendFunctionTypeOrSubtype(context.session)) return
        val argument = call.argumentList.arguments.singleOrNull() ?: return
        reportIfUnsafeSuspendFunctionalValue(argument, forcedType = targetType, reportSource = call.source)
    }
}
