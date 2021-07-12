/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.checkers.getDeclaration
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirAbstractSuperCallChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // require the receiver to be the super reference
        expression.explicitReceiver.safeAs<FirQualifiedAccessExpression>()
            ?.calleeReference.safeAs<FirSuperReference>()
            ?: return

        val closestClass = context.findClosest<FirRegularClass>()
            ?: return

        if (closestClass.classKind == ClassKind.CLASS) {
            // handles all the FirSimpleFunction/FirProperty/etc.
            val item = expression.getDeclaration<FirCallableDeclaration>()
                ?: return

            val declaration = item.getContainingClass(context).safeAs<FirRegularClass>()
                ?: return

            if (
                declaration.modality == Modality.ABSTRACT &&
                item.modality == Modality.ABSTRACT
            ) {
                reporter.reportOn(expression.calleeReference.source, FirErrors.ABSTRACT_SUPER_CALL, context)
            }
        }
    }
}
