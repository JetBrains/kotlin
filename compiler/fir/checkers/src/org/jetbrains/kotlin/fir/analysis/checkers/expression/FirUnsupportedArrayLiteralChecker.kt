/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.resolvedType

object FirUnsupportedArrayLiteralChecker : FirArrayLiteralChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirArrayLiteral) {
        if (!isInsideAnnotationCall() && !isInsideAnnotationConstructor()) {
            reporter.reportOn(
                expression.source,
                FirErrors.UNSUPPORTED,
                "Collection literals outside of annotations are unsupported."
            )
        }
    }

    context(context: CheckerContext)
    private fun isInsideAnnotationCall(): Boolean = context.callsOrAssignments.asReversed().any {
        when (it) {
            is FirFunctionCall -> it.resolvedType.toRegularClassSymbol()?.classKind == ClassKind.ANNOTATION_CLASS
            is FirAnnotationCall -> true
            else -> false
        }
    }

    context(context: CheckerContext)
    private fun isInsideAnnotationConstructor(): Boolean {
        return context.findClosest<FirConstructorSymbol>()?.resolvedReturnType?.toRegularClassSymbol()?.classKind == ClassKind.ANNOTATION_CLASS
    }
}
