/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirValueParameterChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.hasExplicitReturnType
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.checkExpressionForEnhancedTypeMismatch
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.types.coneType

object FirPropertyJavaNullabilityWarningChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.symbol.hasExplicitReturnType) {
            declaration.initializer?.checkExpressionForEnhancedTypeMismatch(
                declaration.returnTypeRef.coneType,
                reporter,
                context,
                FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
            )
        }
    }
}

object FirFunctionJavaNullabilityWarningChecker : FirFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val body = declaration.body
        if (body is FirSingleExpressionBlock && declaration.symbol.hasExplicitReturnType) {
            (body.statement as? FirReturnExpression)?.result?.checkExpressionForEnhancedTypeMismatch(
                declaration.returnTypeRef.coneType,
                reporter,
                context,
                FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
            )
        }
    }
}

object FirValueParameterJavaNullabilityWarningChecker : FirValueParameterChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirValueParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.defaultValue?.checkExpressionForEnhancedTypeMismatch(
            declaration.returnTypeRef.coneType,
            reporter,
            context,
            FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
        )
    }
}
