/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferType
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirDesugaredAssignmentValueReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType

object FirExpressionWithErrorTypeChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        // Pure statements have no type
        if (expression !is FirExpression) return
        // Filter non-error types from the beginning
        val type = expression.resolvedType
        if (type !is ConeErrorType) return
        // Block always takes its type from somewhere else
        if (expression is FirBlock) return
        // Inherits error from its selector
        if (expression is FirSafeCallExpression) return
        // All these variants inherit an error from a referred expression
        if (expression is FirDesugaredAssignmentValueReferenceExpression ||
            expression is FirCheckedSafeCallSubject
        ) return
        // Below we do a return in case expression has its own diagnostic or has a diagnostic inside child nodes
        // (as, again, ErrorNodeDiagnosticCollectorComponent handles such situations itself)
        if (expression is FirDiagnosticHolder) return
        if (expression is FirResolvable) {
            val calleeReference = expression.calleeReference
            if (calleeReference is FirDiagnosticHolder) return
            if (calleeReference is FirSuperReference && calleeReference.superTypeRef is FirErrorTypeRef) return
            if (calleeReference is FirResolvedNamedReference) {
                val symbol = calleeReference.symbol as? FirCallableSymbol
                // Additional variant with variable/function error return type -- already reported on a declaration
                if (symbol?.resolvedReturnTypeRef is FirErrorTypeRef) return
            }
        }
        // This case is separate from FirResolvable as FirThisReference contains a nullable diagnostic
        if (expression is FirThisReceiverExpression && expression.calleeReference.diagnostic != null) return
        // Expression child is an explicit FirErrorTypeRef
        if (expression is FirAnnotationCall && expression.annotationTypeRef is FirErrorTypeRef) return
        if (expression is FirTypeOperatorCall && expression.conversionTypeRef is FirErrorTypeRef) return
        // TODO: remove me after fix of KT-73333
        if (expression is FirWhenExpression && expression.calleeReference is FirStubReference) return
        // Now we decide that the error could be not reported
        val source = expression.source
        if (source != null) {
            val diagnostic = type.diagnostic
            // Always inherited from somewhere else
            if (diagnostic is ConeCannotInferType) return
            if (diagnostic is ConeSimpleDiagnostic) {
                when (diagnostic.kind) {
                    // Specific checker
                    DiagnosticKind.RecursionInImplicitTypes -> return
                    // Always reported on a type alias itself
                    DiagnosticKind.RecursiveTypealiasExpansion -> return
                    // Always reported on an unsigned literal itself
                    DiagnosticKind.UnsignedNumbersAreNotPresent -> return
                    else -> {}
                }
            }
            ErrorNodeDiagnosticCollectorComponent.reportFirDiagnostic(
                type.diagnostic,
                source,
                context,
                reporter = reporter
            )
        }
    }
}
