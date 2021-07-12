/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isExtensionMember
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol

object FirCallableReferenceChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirCallableReferenceAccess) return
        // [FirGetClassCallChecker] will check [FirGetClassCall].
        if (expression is FirGetClassCall) return

        checkReferenceIsToAllowedMember(expression, context, reporter)
    }

    // See FE 1.0 [DoubleColonExpressionResolver#checkReferenceIsToAllowedMember]
    private fun checkReferenceIsToAllowedMember(
        callableReferenceAccess: FirCallableReferenceAccess,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // UNRESOLVED_REFERENCE will be reported separately.
        val reference = callableReferenceAccess.calleeReference as? FirResolvedNamedReference ?: return

        val source = reference.source ?: return
        if (source.kind is FirFakeSourceElementKind) return

        val referredDeclaration = reference.resolvedSymbol.fir
        if (referredDeclaration is FirConstructor && referredDeclaration.getContainingClass(context.session)?.classKind == ClassKind.ANNOTATION_CLASS) {
            reporter.reportOn(source, FirErrors.CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR, context)
        }
        if ((referredDeclaration as? FirCallableDeclaration)?.isExtensionMember == true &&
            !referredDeclaration.isLocalMember
        ) {
            reporter.reportOn(source, FirErrors.EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED, referredDeclaration, context)
        }
        // The counterpart in FE 1.0 checks if the given descriptor is VariableDescriptor yet not PropertyDescriptor.
        // Here, we explicitly check if the referred declaration/symbol is value parameter, local variable, or backing field.
        if (referredDeclaration is FirValueParameter ||
            (referredDeclaration is FirProperty && (referredDeclaration.isLocal || reference.resolvedSymbol is FirBackingFieldSymbol))
        ) {
            // TODO: we can't set positioning strategy to meta error. Should report on reference expression, not entire reference access.
            reporter.reportOn(source, FirErrors.UNSUPPORTED, "References to variables aren't supported yet", context)
        }
    }
}
