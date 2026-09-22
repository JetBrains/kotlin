/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.reportIfWillBecomeValueClass
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.resolvedType

/**
 * 'a === b' distinguishes two instances by identity, which a value class does not have.
 */
object FirIdentityEqualsOnWillBecomeValueClassChecker : FirEqualityOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirEqualityOperatorCall) {
        if (expression.operation != FirOperation.IDENTITY && expression.operation != FirOperation.NOT_IDENTITY) return
        val arguments = expression.argumentList.arguments
        // Comparison with 'null' is a nullability check rather than an identity one.
        if (arguments.any { it.resolvedType.isNullableNothing }) return
        for (argument in arguments) {
            reportIfWillBecomeValueClass(argument.source, argument.resolvedType)
        }
    }
}
