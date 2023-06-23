/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirJvmSuspensionPointInsideMutexLockChecker : FirFunctionCallChecker() {
    private val synchronizedCallableId = CallableId(FqName("kotlin"), Name.identifier("synchronized"))
    private val withLockCallableId = CallableId(FqName("kotlin.concurrent"), Name.identifier("withLock"))

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (!symbol.isSuspend) return
        val closestAnonymousFunction = context.findClosest<FirAnonymousFunction>() ?: return

        for (call in context.callsOrAssignments.asReversed()) {
            if (call is FirFunctionCall) {
                val callableSymbol = call.calleeReference.toResolvedCallableSymbol() ?: continue
                if (callableSymbol.callableId == synchronizedCallableId) {
                    val unwrappedFirstArgument = call.arguments.elementAtOrNull(1)?.unwrapArgument() ?: return
                    val firstArgumentAnonymousFunction =
                        (unwrappedFirstArgument as? FirAnonymousFunctionExpression)?.anonymousFunction ?: return

                    if (closestAnonymousFunction == firstArgumentAnonymousFunction) {
                        reporter.reportOn(expression.source, FirJvmErrors.SUSPENSION_POINT_INSIDE_CRITICAL_SECTION, symbol, context)
                    }
                    return
                } else if (callableSymbol.callableId == withLockCallableId) {
                    reporter.reportOn(expression.source, FirJvmErrors.SUSPENSION_POINT_INSIDE_CRITICAL_SECTION, symbol, context)
                    return
                }
            }
        }
    }
}
