/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.unwrapArgument

object FirAnnotationUsedAsAnnotationArgumentChecker : FirAnnotationCallChecker() {
    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val args = expression.argumentList.arguments
        for (arg in args) {
            for (ann in arg.unwrapArgument().annotations) {
                reporter.reportOn(ann.source, FirErrors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT, context)
            }
        }
    }
}