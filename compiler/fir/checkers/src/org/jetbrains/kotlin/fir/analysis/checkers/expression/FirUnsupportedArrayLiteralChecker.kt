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
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType

object FirUnsupportedArrayLiteralChecker : FirArrayLiteralChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirArrayLiteral) {
        if (isInsideAnnotationConstructor()) return

        when (containingCallKind()) {
            ContainingCallKind.Annotation -> {}
            ContainingCallKind.FunctionReturningAnnotation -> {
                reporter.reportOn(expression.source, FirErrors.UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION)
            }
            ContainingCallKind.NotFound -> {
                reporter.reportOn(expression.source, FirErrors.UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION.errorFactory)
            }
        }
    }

    private enum class ContainingCallKind {
        NotFound, FunctionReturningAnnotation, Annotation
    }

    // See KT-81141
    context(context: CheckerContext)
    private fun containingCallKind(): ContainingCallKind {
        var functionCallFound = ContainingCallKind.NotFound
        for (call in context.callsOrAssignments.asReversed()) {
            when (call) {
                is FirAnnotationCall ->
                    return ContainingCallKind.Annotation
                is FirFunctionCall if call.resolvedType.isAnnotationClass() ->
                    functionCallFound = ContainingCallKind.FunctionReturningAnnotation
            }
        }
        return functionCallFound
    }

    context(context: CheckerContext)
    private fun isInsideAnnotationConstructor(): Boolean {
        return context.findClosest<FirConstructorSymbol>()?.resolvedReturnType.isAnnotationClass()
    }

    context(context: CheckerContext)
    private fun ConeKotlinType?.isAnnotationClass(): Boolean {
        return this?.toRegularClassSymbol()?.classKind == ClassKind.ANNOTATION_CLASS
    }
}
