/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirNotASupertypeChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val superReference = expression.calleeReference.safeAs<FirSuperReference>()?.takeIf { it.hadExplicitTypeInSource() }

        val targetClass = superReference
            ?.superTypeRef
            ?.toRegularClass(context.session)
            ?: return

        val surrounding = context.findClosestClass(superReference.labelName) ?: return
        if (!targetClass.isSupertypeOf(surrounding, context.session)) {
            reporter.reportOn(expression.source, FirErrors.NOT_A_SUPERTYPE, context)
        }
    }

    /**
     * Returns the closest to the end of context.containingDeclarations
     * item like FirRegularClass or FirAnonymousObject
     * or null if no such item could be found.
     */
    private fun CheckerContext.findClosestClass(label: String?): FirClass? {
        for (it in containingDeclarations.reversed()) {
            if (it is FirRegularClass || it is FirAnonymousObject) {
                val firClass = it as FirClass
                val className = firClass.symbol.classId.shortClassName
                if (label == null || (!className.isSpecial && className.identifier == label)) {
                    return firClass
                }
            }
        }

        return null
    }
}
