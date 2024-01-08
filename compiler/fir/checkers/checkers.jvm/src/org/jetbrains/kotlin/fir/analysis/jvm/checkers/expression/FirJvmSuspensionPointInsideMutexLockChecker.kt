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
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.unwrapAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isSuspendOrKSuspendFunctionType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object FirJvmSuspensionPointInsideMutexLockChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val synchronizedCallableId = CallableId(FqName("kotlin"), Name.identifier("synchronized"))
    private val withLockCallableId = CallableId(FqName("kotlin.concurrent"), Name.identifier("withLock"))
    private val synchronizedBlockParamName = Name.identifier("block")

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (!symbol.isSuspend) return
        var anonymousFunctionArg: FirAnonymousFunction? = null
        var isMutexLockFound = false
        var isSuspendFunctionFound = false

        for (element in context.containingElements.asReversed()) {
            if (element is FirFunctionCall) {
                val callableSymbol = element.calleeReference.toResolvedCallableSymbol() ?: continue
                val enclosingAnonymousFuncParam = element.resolvedArgumentMapping?.firstNotNullOfOrNull { entry ->
                    entry.key.unwrapAnonymousFunctionExpression()?.let {
                        runIf(it == anonymousFunctionArg) { entry.value }
                    }
                }

                if ((enclosingAnonymousFuncParam?.returnTypeRef as? FirResolvedTypeRef)?.type?.isSuspendOrKSuspendFunctionType(context.session) == true) {
                    isSuspendFunctionFound = true
                    break
                }

                if (callableSymbol.callableId == synchronizedCallableId &&
                    enclosingAnonymousFuncParam?.name == synchronizedBlockParamName ||
                    callableSymbol.callableId == withLockCallableId
                ) {
                    isMutexLockFound = true
                }
            } else if (element is FirFunction) {
                if (element.isSuspend) {
                    isSuspendFunctionFound = true
                    break
                }
                if (element is FirAnonymousFunction) {
                    anonymousFunctionArg = element // For anonymous function argument `isSuspend` can be detected from the respective parameter
                }
            }
        }

        // There is no need to report SUSPENSION_POINT_INSIDE_CRITICAL_SECTION if enclosing suspend function is not found
        // Because ILLEGAL_SUSPEND_FUNCTION_CALL is reported in this case
        if (isMutexLockFound && isSuspendFunctionFound) {
            reporter.reportOn(expression.source, FirJvmErrors.SUSPENSION_POINT_INSIDE_CRITICAL_SECTION, symbol, context)
        }
    }
}
