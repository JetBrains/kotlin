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
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirBackingFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference

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
        val property = backingFieldReference.resolvedSymbol.fir
        if (property.isVar) return
        val closestGetter = context.findClosest<FirPropertyAccessor> { it.isGetter } ?: return
        if (property.getter != closestGetter) return

        backingFieldReference.source?.let {
            if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.RestrictionOfValReassignmentViaBackingField)) {
                reporter.reportOn(it, FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR, property.symbol, context)
            } else {
                reporter.reportOn(it, FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD, property.symbol, context)
            }
        }
    }

    private fun checkValReassignmentOnValueParameter(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val valueParameter = (expression.lValue as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirValueParameter ?: return
        reporter.reportOn(expression.lValue.source, FirErrors.VAL_REASSIGNMENT, valueParameter.symbol, context)
    }
}
