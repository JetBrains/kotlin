/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.canBeEvaluatedAtCompileTime
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsCodeConstantArgumentChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.calleeReference.toResolvedCallableSymbol()?.callableId != JsStandardClassIds.Callables.JsCode) {
            return
        }

        val jsCodeExpression = expression.arguments.firstOrNull()
        if (jsCodeExpression == null || !jsCodeExpression.resolvedType.isString) {
            reporter.reportOn(jsCodeExpression?.source ?: expression.source, FirJsErrors.JSCODE_ARGUMENT_NON_CONST_EXPRESSION, context)
            return
        }

        jsCodeExpression.accept(object : FirVisitorVoid() {
            var lastReportedElement: FirElement? = null

            override fun visitElement(element: FirElement) {
                val lastReported = lastReportedElement
                element.acceptChildren(this)
                if (lastReported == lastReportedElement && !canBeEvaluatedAtCompileTime(element as? FirExpression, context.session)) {
                    lastReportedElement = element
                    val source = element.source ?: jsCodeExpression.source
                    reporter.reportOn(source, FirJsErrors.JSCODE_ARGUMENT_NON_CONST_EXPRESSION, context)
                }
            }

            override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                if (propertyAccessExpression.calleeReference.toResolvedCallableSymbol()?.isConst != true) {
                    super.visitPropertyAccessExpression(propertyAccessExpression)
                }
            }
        })
    }
}
