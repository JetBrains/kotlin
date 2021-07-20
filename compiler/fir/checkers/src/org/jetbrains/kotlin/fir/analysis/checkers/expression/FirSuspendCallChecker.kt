/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_1_2_20_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_1_2_30_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_1_3_FQ_NAME

object FirSuspendCallChecker : FirQualifiedAccessExpressionChecker() {
    private val SUSPEND_PROPERTIES_FQ_NAMES = setOf(
        COROUTINE_CONTEXT_1_2_20_FQ_NAME, COROUTINE_CONTEXT_1_2_30_FQ_NAME, COROUTINE_CONTEXT_1_3_FQ_NAME
    )

    @OptIn(SymbolInternals::class)
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val reference = expression.calleeReference as? FirResolvedNamedReference ?: return
        if (reference is FirResolvedCallableReference) return
        val symbol = reference.resolvedSymbol as? FirCallableSymbol ?: return
        symbol.ensureResolved(FirResolvePhase.STATUS)
        val fir = symbol.fir as? FirMemberDeclaration ?: return
        when (fir) {
            is FirSimpleFunction -> if (!fir.isSuspend) return
            is FirProperty -> if (symbol.callableId.asSingleFqName() !in SUSPEND_PROPERTIES_FQ_NAMES) return
            else -> return
        }
        val enclosingSuspendFunction = findEnclosingSuspendFunction(context)
        if (enclosingSuspendFunction == null) {
            when (fir) {
                is FirSimpleFunction -> reporter.reportOn(expression.source, FirErrors.ILLEGAL_SUSPEND_FUNCTION_CALL, symbol, context)
                is FirProperty -> reporter.reportOn(expression.source, FirErrors.ILLEGAL_SUSPEND_PROPERTY_ACCESS, symbol, context)
                else -> {
                }
            }
        }
    }

    private fun findEnclosingSuspendFunction(context: CheckerContext): FirFunction? {
        return context.containingDeclarations.lastOrNull {
            when (it) {
                is FirAnonymousFunction -> it.typeRef.coneType.isSuspendFunctionType(context.session)
                is FirSimpleFunction -> it.isSuspend
                else -> false
            }
        } as? FirFunction
    }
}
