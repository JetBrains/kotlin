/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_CONSTRUCTOR_REFERENCE
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isFun
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference

object FirFunInterfaceConstructorReferenceChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirCallableReferenceAccess || expression is FirGetClassCall) return

        val reference = expression.calleeReference as? FirResolvedNamedReference ?: return
        val referredDeclaration = reference.resolvedSymbol.fir

        if (referredDeclaration is FirSimpleFunction &&
            referredDeclaration.origin == FirDeclarationOrigin.SamConstructor &&
            referredDeclaration.returnTypeRef.toRegularClass(context.session)?.isFun == true
        ) {
            reporter.reportOn(reference.source, FUN_INTERFACE_CONSTRUCTOR_REFERENCE, context)
        }
    }
}
