/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.call

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirNotASupertypeChecker : FirQualifiedAccessChecker() {
    override fun check(functionCall: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val surrounding = context.findClosestClass() ?: return

        val targetClass = functionCall.calleeReference.safeAs<FirSuperReference>()
            ?.superTypeRef
            ?.toRegularClass(context.session)
            ?: return

        if (!targetClass.isSupertypeOf(surrounding)) {
            reporter.report(functionCall.source)
        }
    }

    /**
     * Returns the closest to the end of context.containingDeclarations
     * item like FirRegularClass or FirAnonymousObject
     * or null if no such item could be found.
     */
    private fun CheckerContext.findClosestClass(): FirClass<*>? {
        for (it in containingDeclarations.reversed()) {
            if (
                it is FirRegularClass ||
                it is FirAnonymousObject
            ) {
                return it as FirClass<*>
            }
        }

        return null
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.NOT_A_SUPERTYPE.on(it))
        }
    }
}