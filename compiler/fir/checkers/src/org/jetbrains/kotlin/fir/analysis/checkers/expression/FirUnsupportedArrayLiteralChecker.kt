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
import org.jetbrains.kotlin.fir.analysis.checkers.nthLastContainer
import org.jetbrains.kotlin.fir.analysis.checkers.secondToLastContainer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.lastExpression
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
                reportUnsupported(expression, forceError = expression.isInDefinitelyFailingPosition())
            }
            ContainingCallKind.NotFound -> {
                reportUnsupported(expression, forceError = true)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportUnsupported(expression: FirArrayLiteral, forceError: Boolean) {
        if (forceError) {
            reporter.reportOn(expression.source, FirErrors.UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION.errorFactory)
        } else {
            reporter.reportOn(expression.source, FirErrors.UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION)
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

    /**
     * In some cases when a collection literal is used as an independent statement, it crashes Fir2Ir.
     * Therefore, we always need to report an error for such cases.
     * ```
     * run {
     *     ["42"]
     *     Anno()
     * }
     *
     * run {
     *     if (true) { ["42"] }
     *     Anno()
     * }
     * ```
     *
     * In other (similar) cases, the code might still work.
     * ```
     * run {
     *     if (true) ["42"]
     *     Anno()
     * }
     * ```
     */
    context(context: CheckerContext)
    private fun FirArrayLiteral.isInDefinitelyFailingPosition(): Boolean {
        val containingBlock = context.secondToLastContainer as? FirBlock ?: return false

        return when (context.nthLastContainer(3)) {
            is FirAnonymousFunction -> containingBlock.isUnitCoerced || containingBlock.lastExpression !== this
            else -> containingBlock !is FirSingleExpressionBlock
        }
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
