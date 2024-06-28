/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isExtensionMember
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.*

object FirCallableReferenceChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirCallableReferenceAccess) return

        if (expression.hasQuestionMarkAtLHS && expression.explicitReceiver !is FirResolvedQualifier) {
            reporter.reportOn(expression.source, FirErrors.SAFE_CALLABLE_REFERENCE_CALL, context)
        }

        // UNRESOLVED_REFERENCE will be reported separately.
        val reference = expression.calleeReference.resolved ?: return
        val referredSymbol = reference.resolvedSymbol
        val source = reference.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        checkReferenceIsToAllowedMember(referredSymbol, source, context, reporter)
        checkCapturedTypeInMutableReference(expression, referredSymbol, source, context, reporter)
    }

    // See FE 1.0 [DoubleColonExpressionResolver#checkReferenceIsToAllowedMember]
    private fun checkReferenceIsToAllowedMember(
        referredSymbol: FirBasedSymbol<*>,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (referredSymbol is FirConstructorSymbol && referredSymbol.getContainingClassSymbol(context.session)?.classKind == ClassKind.ANNOTATION_CLASS) {
            reporter.reportOn(source, FirErrors.CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR, context)
        }
        if ((referredSymbol as? FirCallableSymbol<*>)?.isExtensionMember == true &&
            !referredSymbol.isLocalMember
        ) {
            reporter.reportOn(source, FirErrors.EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED, referredSymbol, context)
        }
    }

    private fun checkCapturedTypeInMutableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        referredSymbol: FirBasedSymbol<*>,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (!callableReferenceAccess.resolvedType.isKMutableProperty(context.session)) return
        if (referredSymbol !is FirCallableSymbol<*>) return

        val returnType = context.returnTypeCalculator.tryCalculateReturnType(referredSymbol)
        if (returnType.type.hasCapture()) {
            reporter.reportOn(source, FirErrors.MUTABLE_PROPERTY_WITH_CAPTURED_TYPE, context)
        }
    }
}
