/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.isAnyOrNullableAny
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object FirJvmPolymorphicSignatureCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val polymorphicSignatureContainers = listOf(
        ClassId.topLevel(FqName("java.lang.invoke.MethodHandle")),
        ClassId.topLevel(FqName("java.lang.invoke.VarHandle")),
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (!isPolymorphicCall(callableSymbol)) return

        for (valueArgument in expression.arguments) {
            if (valueArgument is FirVarargArgumentsExpression) {
                for (argument in valueArgument.arguments) {
                    if (argument is FirSpreadArgumentExpression) {
                        reporter.reportOn(argument.source, FirJvmErrors.SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR)
                    }
                }
            }
        }
    }

    private fun isPolymorphicCall(symbol: FirCallableSymbol<*>): Boolean {
        // See JLS §15.12.3.
        // > A method is _signature polymorphic_ if all of the following are true:
        // >     * It is declared in the `java.lang.invoke.MethodHandle` class or the `java.lang.invoke.VarHandle` class.
        // >     * It has a single variable arity parameter (§8.4.1) whose declared type is `Object[]`.
        // >     * It is `native`.
        if (symbol !is FirFunctionSymbol<*>) return false
        if (!symbol.isExternal) return false
        if (symbol.dispatchReceiverClassLookupTagOrNull()?.classId !in polymorphicSignatureContainers) return false
        val parameter = symbol.valueParameterSymbols.singleOrNull() ?: return false
        if (!parameter.isVararg) return false
        return parameter.resolvedReturnType.arrayElementType()?.lowerBoundIfFlexible()?.isAnyOrNullableAny == true
    }
}
