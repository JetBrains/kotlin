/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.evaluatedInPlace
import org.jetbrains.kotlin.fir.analysis.cfa.requiresInitialization
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

object FirReassignmentAndInvisibleSetterChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        checkValReassignmentViaBackingField(expression, context, reporter)
        checkValReassignmentOnValueParameter(expression, context, reporter)
        checkVariableExpected(expression, context, reporter)
        checkValReassignment(expression, context, reporter)
    }

    private fun checkValReassignmentViaBackingField(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val backingFieldReference = expression.calleeReference as? FirBackingFieldReference ?: return
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
        val valueParameter = expression.calleeReference?.toResolvedValueParameterSymbol() ?: return
        reporter.reportOn(expression.lValue.source, FirErrors.VAL_REASSIGNMENT, valueParameter, context)
    }

    private fun checkVariableExpected(
        expression: FirVariableAssignment,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val calleeReference = expression.calleeReference

        if (expression.unwrapLValue() !is FirPropertyAccessExpression ||
            (calleeReference?.isConflictingError() != true && calleeReference?.toResolvedVariableSymbol() == null)
        ) {
            reporter.reportOn(expression.lValue.source, FirErrors.VARIABLE_EXPECTED, context)
        }
    }

    private fun FirReference.isConflictingError(): Boolean {
        if (!isError()) return false

        return when (val it = diagnostic) {
            is ConeSimpleDiagnostic -> it.kind == DiagnosticKind.VariableExpected
            is ConeUnresolvedNameError -> true
            is ConeDiagnosticWithCandidates -> it.candidates.any { it.symbol is FirPropertySymbol }
            else -> false
        }
    }

    private fun checkValReassignment(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        val property = expression.calleeReference?.toResolvedPropertySymbol() ?: return
        if (property.isVar) return
        // Assignments of uninitialized `val`s must be checked via CFG, since the first one is OK.
        // See `FirPropertyInitializationAnalyzer` for locals, `FirMemberPropertiesChecker` for backing fields in initializers,
        // and `FirTopLevelPropertiesChecker` for top-level properties.
        if (
            (property.isLocal || isInFileGraph(property, context))
            && property.requiresInitialization(isForInitialization = false)
        ) return
        if (
            isInOwnersInitializer(expression.dispatchReceiver?.unwrapSmartcastExpression(), context)
            && property.requiresInitialization(isForInitialization = true)
        ) return

        reporter.reportOn(expression.lValue.source, FirErrors.VAL_REASSIGNMENT, property, context)
    }

    private fun isInFileGraph(property: FirPropertySymbol, context: CheckerContext): Boolean {
        val declarations = context.containingDeclarations.dropWhile { it !is FirFile }
        val file = declarations.firstOrNull() as? FirFile ?: return false
        if (file.symbol != property.getContainingSymbol(context.session)) return false

        // Starting with the CFG for the containing FirFile, check if all following declarations are contained as sub-CFGs.
        // If there is a break in the chain, then the variable assignment is not part of the file CFG, and VAL_REASSIGNMENT should be
        // reported by this checker.
        val containingGraph = declarations
            .map { (it as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph }
            .reduceOrNull { acc, graph -> graph?.takeIf { acc != null && it in acc.subGraphs } }
        return containingGraph != null
    }

    private fun isInOwnersInitializer(receiver: FirExpression?, context: CheckerContext): Boolean {
        val uninitializedThisSymbol = (receiver as? FirThisReceiverExpression)?.calleeReference?.boundSymbol ?: return false
        val containingDeclarations = context.containingDeclarations

        val index = containingDeclarations.indexOfFirst { it is FirClass && it.symbol == uninitializedThisSymbol }
        if (index == -1) return false

        for (i in index until containingDeclarations.size) {
            if (containingDeclarations[i] is FirClass) {
                // Properties need special consideration as some parts are evaluated in-place (initializers) and others are not (accessors).
                // So it is not enough to just check the FirProperty - which is treated as in-place - but the following declaration needs to
                // be checked if and only if it is a property accessor.
                val container = when (val next = containingDeclarations.getOrNull(i + 1)) {
                    is FirProperty -> containingDeclarations.getOrNull(i + 2)?.takeIf { it is FirPropertyAccessor } ?: next
                    else -> next
                }

                // In member function of a class, assume all outer classes are already initialized
                // by the time this function is called.
                if (container?.evaluatedInPlace == false) {
                    return false
                }
            }
        }

        return true
    }
}
