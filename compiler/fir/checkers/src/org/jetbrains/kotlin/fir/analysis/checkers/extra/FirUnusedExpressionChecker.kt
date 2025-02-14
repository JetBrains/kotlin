/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirUnusedCheckerBase
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

object FirUnusedExpressionChecker : FirUnusedCheckerBase() {
    override fun isEnabled(context: CheckerContext): Boolean = true // Controlled by FIR_EXTRA_CHECKERS

    override fun isExpressionUnused(
        expression: FirExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        source: KtSourceElement?,
    ): Boolean {
        if (expression.hasSideEffect()) return false

        val factory = when {
            expression is FirAnonymousFunctionExpression && expression.anonymousFunction.isLambda
                -> FirErrors.UNUSED_LAMBDA_EXPRESSION
            else -> FirErrors.UNUSED_EXPRESSION
        }
        reporter.reportOn(source, factory, context)
        return true
    }
}


/**
 * Elements with side effects are those that may execute some other expressions when executed.
 * This includes functions (as they inherently are defined as having side effects), access of
 * properties with custom getters, and may other types within the FIR tree.
 *
 * Note: ***be conservative***. Indicating an [FirExpression] is side-effect-free should only be
 * done for elements, which when removed from the code, won't impact the behavior of the code.
 */
private fun FirExpression.hasSideEffect(): Boolean {
    return when (this) {
        // Literals and references that are known to be side-effect-free.
        is FirLiteralExpression,
        is FirClassReferenceExpression,
        is FirResolvedQualifier,
        is FirThisReceiverExpression,
            -> false

        // The definition of an anonymous function is side-effect-free.
        // Invoking an anonymous function has side effects, but this is performed by another FIR element.
        is FirAnonymousFunctionExpression,
            -> false

        // A smart cast has a side effect iff its original expression has a side effect.
        is FirSmartCastExpression -> {
            originalExpression.hasSideEffect()
        }

        // A callable reference is side-effect-free only if all of its receivers are side-effect-free.
        is FirCallableReferenceAccess -> {
            dispatchReceiver?.hasSideEffect() == true ||
                    extensionReceiver?.hasSideEffect() == true ||
                    explicitReceiver?.hasSideEffect() == true
        }

        // String concatenation and class access are side-effect-free if all arguments are side-effect-free.
        is FirStringConcatenationCall,
        is FirGetClassCall,
            -> {
            arguments.any { it.hasSideEffect() }
        }

        // Property access is side-effect-free if the referenced property does not have a custom getter.
        // However, this check is limited to just considering value parameters, receiver parameters, and
        // local properties without delegates as side-effect-free, to be conservative and match K1 behavior.
        is FirPropertyAccessExpression -> {
            if (source?.kind == KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess) true
            else when (val propertySymbol = calleeReference.symbol) {
                is FirValueParameterSymbol, is FirReceiverParameterSymbol -> false
                is FirPropertySymbol -> !propertySymbol.isLocal || propertySymbol.hasDelegate
                else -> true
            }
        }

        else -> true
    }
}
