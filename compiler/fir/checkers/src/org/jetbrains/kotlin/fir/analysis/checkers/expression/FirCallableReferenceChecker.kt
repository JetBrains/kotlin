/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isExtensionMember
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
import org.jetbrains.kotlin.fir.types.*

object FirCallableReferenceChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (expression !is FirCallableReferenceAccess) return

        if (expression.hasQuestionMarkAtLHS && expression.explicitReceiver?.unwrapSmartcastExpression() !is FirResolvedQualifier) {
            reporter.reportOn(expression.source, FirErrors.SAFE_CALLABLE_REFERENCE_CALL)
        }

        // UNRESOLVED_REFERENCE will be reported separately.
        val reference = expression.calleeReference.resolved ?: return
        val referredSymbol = reference.resolvedSymbol
        val source = reference.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        checkReferenceIsToAllowedMember(referredSymbol, source)
        checkCapturedTypeInMutableReference(expression, referredSymbol, source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
// See FE 1.0 [DoubleColonExpressionResolver#checkReferenceIsToAllowedMember]
    private fun checkReferenceIsToAllowedMember(
        referredSymbol: FirBasedSymbol<*>,
        source: KtSourceElement,
    ) {
        if (referredSymbol is FirConstructorSymbol && referredSymbol.getContainingClassSymbol()?.classKind == ClassKind.ANNOTATION_CLASS) {
            reporter.reportOn(source, FirErrors.CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR)
        }

        if (referredSymbol is FirCallableSymbol) {
            if (referredSymbol.isExtensionMember && referredSymbol.visibility != Visibilities.Local) {
                reporter.reportOn(source, FirErrors.EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED, referredSymbol)
            }

            if (referredSymbol.hasContextParameters && LanguageFeature.ContextParameters.isEnabled()) {
                reporter.reportOn(source, FirErrors.CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION, referredSymbol)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCapturedTypeInMutableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        referredSymbol: FirBasedSymbol<*>,
        source: KtSourceElement,
    ) {
        if (!callableReferenceAccess.resolvedType.isKMutableProperty(context.session)) return
        if (referredSymbol !is FirCallableSymbol<*>) return

        val returnType = context.returnTypeCalculator.tryCalculateReturnType(referredSymbol)
        if (returnType.coneType.hasCapture()) {
            reporter.reportOn(source, FirErrors.MUTABLE_PROPERTY_WITH_CAPTURED_TYPE)
        }
    }
}
