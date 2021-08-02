/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirBackingFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

object FirValReassignmentChecker : FirVariableAssignmentChecker() {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        checkValReassignmentViaBackingField(expression, context, reporter)
        checkValReassignmentOnValueParameter(expression, context, reporter)
    }

    private fun checkValReassignmentViaBackingField(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val backingFieldReference = expression.lValue as? FirBackingFieldReference ?: return
        val propertySymbol = backingFieldReference.resolvedSymbol
        if (propertySymbol.isVar) return
        val closestGetter = context.findClosest<FirPropertyAccessor> { it.isGetter }?.symbol ?: return
        if (propertySymbol.getterSymbol != closestGetter) return

        backingFieldReference.source?.let {
            if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.RestrictionOfValReassignmentViaBackingField)) {
                reporter.reportOn(it, FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR, propertySymbol, context)
            } else {
                reporter.reportOn(it, FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD, propertySymbol, context)
            }
        }
    }

    private fun checkValReassignmentOnValueParameter(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val valueParameter = (expression.lValue as? FirResolvedNamedReference)?.resolvedSymbol as? FirValueParameterSymbol ?: return
        reporter.reportOn(expression.lValue.source, FirErrors.VAL_REASSIGNMENT, valueParameter, context)
    }
}
