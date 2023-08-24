/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isOptionalAnnotationClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirOptionalExpectationExpressionChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val constructorSymbol = expression.calleeReference.toResolvedConstructorSymbol() ?: return
        val declarationClass = constructorSymbol.resolvedReturnTypeRef.coneType.toRegularClassSymbol(context.session) ?: return
        if (!declarationClass.isOptionalAnnotationClass(context.session)) return

        if (!context.session.moduleData.isCommon) {
            reporter.reportOn(expression.source, FirErrors.OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE, context)
        }

        reporter.reportOn(expression.source, FirErrors.OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY, context)
    }
}