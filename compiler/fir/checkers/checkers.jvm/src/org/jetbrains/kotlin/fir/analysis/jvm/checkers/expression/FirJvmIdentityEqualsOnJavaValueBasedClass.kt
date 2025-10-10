/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirEqualityOperatorCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.disableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.resolvedType

internal object FirJvmIdentityEqualsOnJavaValueBasedClass : FirEqualityOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirEqualityOperatorCall) {
        if (expression.operation != FirOperation.IDENTITY && expression.operation != FirOperation.NOT_IDENTITY) return
        if (disableWarningsForValueBasedJavaClasses()) return
        val arguments = expression.argumentList.arguments
        require(arguments.size == 2) { "Expected arguments of size 2" }

        if (arguments.any { it.resolvedType.isNullableNothing }) return
        for (arg in arguments) {
            val type = arg.resolvedType
            if (type.isJavaValueBasedClass()) {
                reporter.reportOn(
                    arg.source, FirJvmErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, type
                )
            }
        }
    }
}
