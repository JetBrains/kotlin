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
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.StandardClassIds

object FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        val kotlinActual = StandardClassIds.Annotations.KotlinActual
        when (expression) {
            is FirResolvedQualifier if expression.resolvedType.classId == kotlinActual ->
                reporter.reportOn(expression.source, FirErrors.KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN, context)
            is FirCallableReferenceAccess if expression.calleeReference.toResolvedConstructorSymbol()?.containingClassLookupTag()?.classId == kotlinActual ->
                reporter.reportOn(expression.source, FirErrors.KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN, context)
            is FirFunctionCall if expression.calleeReference.toResolvedConstructorSymbol()?.containingClassLookupTag()?.classId == kotlinActual ->
                reporter.reportOn(expression.source, FirErrors.KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN, context)
        }
    }
}
