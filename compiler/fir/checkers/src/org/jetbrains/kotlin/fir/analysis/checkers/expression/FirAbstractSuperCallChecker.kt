/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.explicitReceiverIsNotSuperReference
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

object FirAbstractSuperCallChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // require the receiver to be the super reference
        if (expression.explicitReceiverIsNotSuperReference()) return

        val closestClass = context.findClosest<FirRegularClass>()
            ?: return

        if (closestClass.classKind == ClassKind.CLASS) {
            // handles all the FirSimpleFunction/FirProperty/etc.
            val declarationSymbol = expression.toResolvedCallableSymbol() ?: return

            val containingClassSymbol = declarationSymbol.containingClassLookupTag()?.toSymbol(context.session) as? FirRegularClassSymbol ?: return

            if (containingClassSymbol.isAbstract) {
                if (declarationSymbol.isAbstract) {
                    reporter.reportOn(expression.calleeReference.source, FirErrors.ABSTRACT_SUPER_CALL, context)
                }
                if (declarationSymbol is FirIntersectionCallableSymbol) {
                    val symbolFromBaseClass = declarationSymbol.intersections.firstOrNull {
                        it.containingClassLookupTag()?.toSymbol(context.session)?.classKind != ClassKind.INTERFACE
                    }
                    if (symbolFromBaseClass?.isAbstract == true) {
                        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride)) {
                            reporter.reportOn(expression.calleeReference.source, FirErrors.ABSTRACT_SUPER_CALL, context)
                        } else {
                            reporter.reportOn(expression.calleeReference.source, FirErrors.ABSTRACT_SUPER_CALL_WARNING, context)
                        }
                    }
                }
            }
        }
    }
}
