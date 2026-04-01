/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirWhenExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.isExhaustive
import org.jetbrains.kotlin.fir.java.enhancement.enhancedTypeForWarning
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedOrFlexiblyNullable
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType

object FirJavaWhenExhaustivenessWarningChecker : FirWhenExpressionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        if (!expression.isExhaustive) return
        val variable = expression.subjectVariable ?: return
        val coneType = variable.returnTypeRef.coneType
        val enhancedType = coneType.enhancedTypeForWarning ?: return
        if (!enhancedType.lowerBoundIfFlexible().canBeNull(context.session) ||
            coneType.lowerBoundIfFlexible().canBeNull(context.session)
        ) return

        val hasNullCheck = expression.branches.any {
            if (it.hasGuard) return@any false
            when (val condition = it.condition) {
                is FirEqualityOperatorCall -> condition.arguments[1].resolvedType.isNullableNothing
                is FirTypeOperatorCall -> condition.operation == FirOperation.IS && condition.conversionTypeRef.coneType.isMarkedOrFlexiblyNullable
                else -> false
            }
        }

        if (!hasNullCheck) reporter.reportOn(expression.source, FirJvmErrors.UNEXHAUSTIVE_WHEN_BASED_ON_JAVA_ANNOTATIONS, enhancedType)
    }
}