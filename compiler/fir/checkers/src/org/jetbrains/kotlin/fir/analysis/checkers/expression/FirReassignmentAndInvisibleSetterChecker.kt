/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.evaluatedInPlace
import org.jetbrains.kotlin.fir.analysis.cfa.requiresInitialization
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.references.FirBackingFieldReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.references.toResolvedValueParameterSymbol
import org.jetbrains.kotlin.fir.resolve.calls.ExpressionReceiverValue
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.visibilityChecker

object FirReassignmentAndInvisibleSetterChecker : FirVariableAssignmentChecker() {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        checkInvisibleSetter(expression, context, reporter)
        checkValReassignmentViaBackingField(expression, context, reporter)
        checkValReassignmentOnValueParameter(expression, context, reporter)
        checkAssignmentToThis(expression, context, reporter)
        checkValReassignment(expression, context, reporter)
    }

    private fun checkInvisibleSetter(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        fun shouldInvisibleSetterBeReported(symbol: FirPropertySymbol): Boolean {
            @OptIn(SymbolInternals::class)
            val setterFir = symbol.setterSymbol?.fir ?: symbol.originalForSubstitutionOverride?.setterSymbol?.fir
            if (setterFir != null) {
                return !context.session.visibilityChecker.isVisible(
                    setterFir,
                    context.session,
                    context.findClosest()!!,
                    context.containingDeclarations,
                    ExpressionReceiverValue(expression.dispatchReceiver),
                )
            }

            return false
        }

        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol()
        if (callableSymbol is FirPropertySymbol && shouldInvisibleSetterBeReported(callableSymbol)) {
            val explicitReceiver = expression.explicitReceiver
            // Try to get type from smartcast
            if (explicitReceiver is FirSmartCastExpression) {
                val symbol = explicitReceiver.originalExpression.typeRef.toRegularClassSymbol(context.session)
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
                callableSymbol.setterSymbol?.visibility ?: Visibilities.Private,
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

        reporter.reportOn(backingFieldReference.source, FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD, propertySymbol, context)
    }

    private fun checkValReassignmentOnValueParameter(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val valueParameter = expression.lValue.toResolvedValueParameterSymbol() ?: return
        reporter.reportOn(expression.lValue.source, FirErrors.VAL_REASSIGNMENT, valueParameter, context)
    }

    private fun checkAssignmentToThis(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (expression.lValue is FirThisReference) {
            reporter.reportOn(expression.lValue.source, FirErrors.VARIABLE_EXPECTED, context)
        }
    }

    private fun checkValReassignment(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        val property = expression.lValue.toResolvedPropertySymbol() ?: return
        if (property.isVar) return
        // Assignments of uninitialized `val`s must be checked via CFG, since the first one is OK.
        // See `FirPropertyInitializationAnalyzer` for locals and `FirMemberPropertiesChecker` for backing fields in initializers.
        if (property.requiresInitialization && (property.isLocal || isInOwnersInitializer(expression.dispatchReceiver, context))) return

        reporter.reportOn(expression.lValue.source, FirErrors.VAL_REASSIGNMENT, property, context)
    }

    private fun isInOwnersInitializer(receiver: FirExpression, context: CheckerContext): Boolean {
        val uninitializedThisSymbol = (receiver as? FirThisReceiverExpression)?.calleeReference?.boundSymbol ?: return false
        var foundInitializer = false
        for ((i, declaration) in context.containingDeclarations.withIndex()) {
            if (declaration is FirClass) {
                foundInitializer = if (context.containingDeclarations.getOrNull(i + 1)?.evaluatedInPlace == false) {
                    // In member function of a class, assume all outer classes are already initialized
                    // by the time this function is called.
                    false
                } else {
                    foundInitializer || declaration.symbol == uninitializedThisSymbol
                }
            }
        }
        return foundInitializer
    }
}
