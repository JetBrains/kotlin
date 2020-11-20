/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirSuperNotAvailableChecker : FirQualifiedAccessChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.calleeReference.safeAs<FirSuperReference>()?.hadExplicitTypeInSource() != true) return

        val isInsideClass = context.containingDeclarations.any {
            it is FirClass<*>
        }

        if (!isInsideClass) {
            reporter.report(expression.source)
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.SUPER_NOT_AVAILABLE.on(it))
        }
    }
}
