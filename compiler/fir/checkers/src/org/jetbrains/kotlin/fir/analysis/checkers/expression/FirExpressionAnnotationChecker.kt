/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement

object FirExpressionAnnotationChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        // Declarations are checked separately
        // See KT-33658 about annotations on non-expression statements
        if (expression is FirDeclaration ||
            expression !is FirExpression ||
            expression is FirBlock && expression.source?.kind == FirFakeSourceElementKind.DesugaredForLoop
        ) return
        for (annotation in expression.annotations) {
            withSuppressedDiagnostics(annotation, context) {
                if (KotlinTarget.EXPRESSION !in annotation.getAllowedAnnotationTargets(context.session)) {
                    reporter.reportOn(annotation.source, FirErrors.WRONG_ANNOTATION_TARGET, "expression", context)
                }
            }
        }
    }
}