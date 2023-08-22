/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirAnonymousFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationRetention
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol

object FirInlinedLambdaNonSourceAnnotationsChecker : FirAnonymousFunctionChecker() {
    override fun check(declaration: FirAnonymousFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.inlineStatus != InlineStatus.Inline && declaration.inlineStatus != InlineStatus.CrossInline) {
            return
        }

        for (it in declaration.annotations) {
            val annotationSymbol = it.toAnnotationClassLikeSymbol(context.session) ?: continue

            if (annotationSymbol.getAnnotationRetention(context.session) != AnnotationRetention.SOURCE) {
                reporter.reportOn(it.source, FirErrors.NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION, context)
            }
        }
    }
}
