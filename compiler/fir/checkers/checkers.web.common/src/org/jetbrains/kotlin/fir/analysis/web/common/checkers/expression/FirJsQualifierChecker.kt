/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.js.validateQualifier
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsQualifier

object FirJsQualifierChecker : FirAnnotationCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.toAnnotationClassId(context.session) != JsQualifier) {
            return
        }

        val string = (expression.argumentMapping.mapping.values.firstOrNull() as? FirLiteralExpression<*>)?.value as? String ?: return

        if (!validateQualifier(string)) {
            reporter.reportOn(expression.argumentList.arguments.first().source, FirWebCommonErrors.WRONG_JS_QUALIFIER, context)
        }
    }
}
