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
import org.jetbrains.kotlin.fir.backend.native.interop.isVariadicObjCMethod
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.resolvedType

internal object FirNativeObjCStringAsVariadicChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return
        if (!symbol.isVariadicObjCMethod(context.session)) return

        val argumentMapping = expression.resolvedArgumentMapping ?: return
        for ((argument, parameter) in argumentMapping) {
            if (parameter.isVararg && argument is FirVarargArgumentsExpression) {
                for (element in argument.arguments) {
                    if (element.resolvedType.isString) {
                        reporter.reportOn(
                            element.source,
                            FirNativeErrors.STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS,
                            context
                        )
                    }
                }
            }
        }
    }
}
