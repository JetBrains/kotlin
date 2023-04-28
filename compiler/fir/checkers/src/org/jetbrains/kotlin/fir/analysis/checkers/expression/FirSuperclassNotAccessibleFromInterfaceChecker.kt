/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.explicitReceiverIsNotSuperReference
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.StandardClassIds

object FirSuperclassNotAccessibleFromInterfaceChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.explicitReceiverIsNotSuperReference()) return

        val closestClass = context.findClosest<FirClass>() ?: return

        if (closestClass.classKind == ClassKind.INTERFACE) {
            val containingClassSymbol =
                expression.toResolvedCallableSymbol()?.getContainingClassSymbol(context.session) as? FirRegularClassSymbol ?: return

            if (containingClassSymbol.source != null && containingClassSymbol.classKind == ClassKind.CLASS && containingClassSymbol.classId != StandardClassIds.Any) {
                reporter.reportOn(expression.explicitReceiver?.source, FirErrors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE, context)
            }
        }
    }
}
