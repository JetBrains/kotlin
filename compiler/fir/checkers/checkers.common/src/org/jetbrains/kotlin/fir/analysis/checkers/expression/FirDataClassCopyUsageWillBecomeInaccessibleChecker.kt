/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.isDataClassCopy
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.visibilityChecker

object FirDataClassCopyUsageWillBecomeInaccessibleChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (expression !is FirFunctionCall && expression !is FirCallableReferenceAccess) return
        val copyFunction = expression.calleeReference.symbol as? FirCallableSymbol ?: return
        val dataClass = copyFunction.containingClassLookupTag()?.toRegularClassSymbol() ?: return
        if (copyFunction.isDataClassCopy(dataClass, context.session)) {
            val dataClassConstructor = dataClass.primaryConstructorIfAny(context.session) ?: return

            val hasCopyAlreadyBecameInaccessible = !context.session.visibilityChecker.isVisible(
                copyFunction,
                context.session,
                context.containingFileSymbol ?: return,
                context.containingDeclarations,
                dispatchReceiver = null
            )
            if (hasCopyAlreadyBecameInaccessible) {
                return
            }

            val isConstructorVisible = context.session.visibilityChecker.isVisible(
                dataClassConstructor,
                context.session,
                context.containingFileSymbol ?: return,
                context.containingDeclarations,
                dispatchReceiver = null
            )
            // We don't check the presence of @ExposedCopyVisibility annotations on purpose.
            // Even if the 'copy' is exposed, call-sites need to migrate.
            if (!isConstructorVisible) {
                reporter.reportOn(expression.calleeReference.source, FirErrors.DATA_CLASS_INVISIBLE_COPY_USAGE)
            }
        }
    }
}
