/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirBackingFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ExpressionReceiverValue
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.visibilityChecker

object FirReassignmentAndInvisibleSetterChecker : FirVariableAssignmentChecker() {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        checkInvisibleSetter(expression, context, reporter)
        checkValReassignmentViaBackingField(expression, context, reporter)
        checkValReassignmentOnValueParameter(expression, context, reporter)
    }

    private fun checkInvisibleSetter(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        fun shouldInvisibleSetterBeReported(symbol: FirPropertySymbol): Boolean {
            val setterSymbol = symbol.setterSymbol

            @OptIn(SymbolInternals::class)
            val setterFir = setterSymbol?.fir
            if (setterFir != null) {
                val isVisible = context.session.visibilityChecker.isVisible(
                    setterFir,
                    context.session,
                    context.findClosest()!!,
                    context.containingDeclarations,
                    ExpressionReceiverValue(expression.dispatchReceiver),
                )
                if (!isVisible) {
                    // Report SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC in another checker
                    val dispatchReceiverTypeSymbol = symbol.dispatchReceiverType?.toSymbol(context.session)
                    return setterFir.visibility != Visibilities.Protected ||
                            dispatchReceiverTypeSymbol !is FirRegularClassSymbol ||
                            !dispatchReceiverTypeSymbol.isCompanion
                }
            }

            return false
        }

        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol()
        if (callableSymbol is FirPropertySymbol && shouldInvisibleSetterBeReported(callableSymbol)) {
            val explicitReceiver = expression.explicitReceiver
            // Try to get type from smartcast
            if (explicitReceiver is FirExpressionWithSmartcast) {
                val symbol = explicitReceiver.originalType.toRegularClassSymbol(context.session)
                if (symbol != null) {
                    for (declarationSymbol in symbol.declarationSymbols) {
                        if (declarationSymbol is FirPropertySymbol && declarationSymbol.name == callableSymbol.name) {
                            if (!shouldInvisibleSetterBeReported(declarationSymbol)) {
                                return
                            }
                        }
                    }
                }
            }
            reporter.reportOn(
                expression.source,
                FirErrors.INVISIBLE_SETTER,
                callableSymbol,
                callableSymbol.setterSymbol!!.visibility,
                callableSymbol.callableId,
                context
            )
        }
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
