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
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Mental model: `KotlinActual` is annotated with `@kotlin.Deprecated(level = DeprecationLevel.ERROR)`
 */
sealed class FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker<T : FirExpression> :
    FirExpressionChecker<T>(MppCheckerKind.Common) {
    object ResolvedQualifier : FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker<FirResolvedQualifier>() {
        override fun check(
            expression: FirResolvedQualifier,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            if (expression.resolvedType.classId == StandardClassIds.Annotations.KotlinActual) {
                reporter.reportOn(expression.source, FirErrors.KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN, context)
            }
        }
    }

    object CallableReference : FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker<FirCallableReferenceAccess>() {
        override fun check(
            expression: FirCallableReferenceAccess,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            if (expression.calleeReference.toResolvedConstructorSymbol()
                    ?.containingClassLookupTag()?.classId == StandardClassIds.Annotations.KotlinActual
            ) {
                reporter.reportOn(expression.source, FirErrors.KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN, context)
            }
        }
    }

    object FunctionCall : FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker<FirFunctionCall>() {
        override fun check(
            expression: FirFunctionCall,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            if (expression.calleeReference.toResolvedConstructorSymbol()
                    ?.containingClassLookupTag()?.classId == StandardClassIds.Annotations.KotlinActual
            ) {
                reporter.reportOn(expression.source, FirErrors.KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN, context)
            }
        }
    }
}
