/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES
import org.jetbrains.kotlin.psi.psiUtil.hasUnwrappableAsAssignmentLhs
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirParenthesizedLhsSetOperatorChecker : FirFunctionCallChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val callee = expression.calleeReference
        val source = expression.source ?: return

        // We're only interested in `set` convention calls
        if (callee.name != OperatorNameConventions.SET) return

        if (// For `(a[0]) = ...` where `a: Array<A>`
            (source.elementType in UNWRAPPABLE_TOKEN_TYPES) && callee.isArrayAccess ||
            // For `(a[0]) += ""` where `a: Array<A>`
            source.hasUnwrappableAsAssignmentLhs() && callee.isAugmentedAssign ||
            // For `(a[0])++` where `a` has `get`,`set` and `inc` operators
            source.hasUnwrappableAsAssignmentLhs() && callee.isIncrementOrDecrement
        ) {
            reporter.reportOn(source, FirErrors.WRAPPED_LHS_IN_ASSIGNMENT, context)
        }
    }

    private val FirNamedReference.isArrayAccess: Boolean
        get() = source?.kind == KtFakeSourceElementKind.ArrayAccessNameReference

    private val FirNamedReference.isAugmentedAssign: Boolean
        get() = source?.kind is KtFakeSourceElementKind.DesugaredAugmentedAssign

    private val FirNamedReference.isIncrementOrDecrement: Boolean
        get() = source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement
}
