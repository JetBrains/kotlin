/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirSuperclassNotAccessibleFromInterfaceChecker : FirQualifiedAccessChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        expression.explicitReceiver.safeAs<FirQualifiedAccessExpression>()
            ?.calleeReference.safeAs<FirSuperReference>()
            ?: return

        val closestClass = context.findClosest<FirRegularClass>() ?: return

        if (closestClass.classKind == ClassKind.INTERFACE) {
            val origin = getClassLikeDeclaration(expression, context)
                ?.symbol.safeAs<FirRegularClassSymbol>()
                ?.fir
                ?: return

            if (origin.source != null && origin.classKind == ClassKind.CLASS) {
                reporter.report(expression.explicitReceiver?.source)
            }
        }
    }

    /**
     * Returns the ClassLikeDeclaration where the function has been defined
     * or null if no proper declaration has been found.
     */
    private fun getClassLikeDeclaration(functionCall: FirQualifiedAccessExpression, context: CheckerContext): FirClassLikeDeclaration<*>? {
        return functionCall.calleeReference.safeAs<FirResolvedNamedReference>()?.resolvedSymbol?.fir?.getContainingClass(context)
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE.on(it))
        }
    }
}
