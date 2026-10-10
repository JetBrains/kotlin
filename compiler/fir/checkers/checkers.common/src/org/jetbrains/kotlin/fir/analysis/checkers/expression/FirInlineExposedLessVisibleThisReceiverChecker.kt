/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isDispatchReceiver
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirInlineExposedLessVisibleTypeChecker
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

object FirInlineExposedLessVisibleThisReceiverChecker : FirThisReceiverExpressionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirThisReceiverExpression) {
        val inlineFunctionBodyContext = context.inlineFunctionBodyContext ?: return

        // Dispatch receivers are already checked in FirInlineExposedLessVisibleTypeQualifiedAccessChecker
        if (expression.isDispatchReceiver()) return

        val accessedClass = expression.calleeReference.boundSymbol
        if (accessedClass is FirClassLikeSymbol && accessedClass.isCompanion) {
            FirInlineExposedLessVisibleTypeChecker.check(accessedClass.defaultType(), expression.source, inlineFunctionBodyContext)
        }
    }
}