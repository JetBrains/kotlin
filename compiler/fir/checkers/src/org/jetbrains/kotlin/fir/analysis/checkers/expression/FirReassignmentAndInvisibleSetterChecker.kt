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
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.resolve.getContainingSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeVisibilityError
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.visibilityChecker

object FirReassignmentAndInvisibleSetterChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        checkInvisibleSetter(expression)
        checkValReassignmentViaBackingField(expression)
        checkValReassignmentOnValueParameterOrEnumEntry(expression)
        checkVariableExpected(expression)
        checkValReassignment(expression)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInvisibleSetter(
        expression: FirVariableAssignment,
    ) {
        fun shouldInvisibleSetterBeReported(symbol: FirPropertySymbol): Boolean {
            val setterSymbol = symbol.unwrapFakeOverrides().setterSymbol ?: return false
            return !context.session.visibilityChecker.isVisible(
                setterSymbol,
                context.session,
                context.containingFileSymbol!!,
                context.containingDeclarations,
                expression.dispatchReceiver,
            )
        }

        if (expression.calleeReference?.isVisibilityError == true) {
            return
        }

        val callableSymbol = expression.calleeReference?.toResolvedCallableSymbol()
        if (callableSymbol is FirPropertySymbol && shouldInvisibleSetterBeReported(callableSymbol)) {
            reporter.reportOn(
                expression.lValue.source,
                FirErrors.INVISIBLE_SETTER,
                callableSymbol,
                callableSymbol.setterSymbol?.visibility ?: Visibilities.Private,
                callableSymbol.callableId!!
            )
        }
    }

    private val FirReference.isVisibilityError: Boolean
        get() = this is FirResolvedErrorReference && diagnostic is ConeVisibilityError

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkValReassignmentViaBackingField(
        expression: FirVariableAssignment,
    ) {
        val backingFieldReference = expression.calleeReference as? FirBackingFieldReference ?: return
        val propertySymbol = backingFieldReference.resolvedSymbol
        if (propertySymbol.isVar) return
        val closestGetter = context.findClosest<FirPropertyAccessorSymbol> { it.isGetter } ?: return
        if (propertySymbol.getterSymbol != closestGetter) return

        reporter.reportOn(backingFieldReference.source, FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR, propertySymbol)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkValReassignmentOnValueParameterOrEnumEntry(
        expression: FirVariableAssignment,
    ) {
        when (val symbol = expression.calleeReference?.toResolvedVariableSymbol()) {
            is FirValueParameterSymbol,
            is FirEnumEntrySymbol
                -> {
                reporter.reportOn(expression.lValue.source, FirErrors.VAL_REASSIGNMENT, symbol)
            }
            else -> {}
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVariableExpected(
        expression: FirVariableAssignment,
    ) {
        val calleeReference = expression.calleeReference

        if (expression.unwrapLValue() !is FirPropertyAccessExpression ||
            (calleeReference?.isConflictingError() != true && calleeReference?.toResolvedVariableSymbol() == null)
        ) {
            reporter.reportOn(expression.lValue.source, FirErrors.VARIABLE_EXPECTED)
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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkValReassignment(expression: FirVariableAssignment) {
        val variable = expression.calleeReference?.toResolvedVariableSymbol() ?: return
        if (variable.isVar) return
        when (variable) {
            is FirPropertySymbol -> {
                /**
                 * Assignments of uninitialized `val`s must be checked via CFG, since the first one is OK.
                 *
                 * See [org.jetbrains.kotlin.fir.analysis.cfa.FirPropertyInitializationAnalyzer] for locals,
                 * [FirMemberPropertiesChecker] for backing fields in initializers,
                 * and [FirTopLevelPropertiesChecker] for top-level properties.
                 */
                if (
                    (variable is FirLocalPropertySymbol || isInFileGraph(variable))
                    && variable.requiresInitialization(isForInitialization = false)
                ) return
                if (
                    variable.requiresInitialization(isForInitialization = true)
                    && isInOwnersInitializer(expression.dispatchReceiver?.unwrapSmartcastExpression(), variable)
                ) return
            }
            is FirFieldSymbol -> {
                // Java fields also must be checked here
            }
            /**
             * [FirBackingFieldSymbol] is reported in [checkValReassignmentViaBackingField],
             * [FirDelegateFieldSymbol] is not needed at all,
             * [FirValueParameterSymbol] & [FirEnumEntrySymbol] are reported in [checkValReassignmentOnValueParameterOrEnumEntry].
             */
            is FirBackingFieldSymbol,
            is FirDelegateFieldSymbol,
            is FirValueParameterSymbol,
            is FirEnumEntrySymbol,
                -> return
        }

        reporter.reportOn(expression.lValue.source, FirErrors.VAL_REASSIGNMENT, variable)
    }

    context(context: CheckerContext)
    private fun isInFileGraph(property: FirPropertySymbol): Boolean {
        val declarations = context.containingDeclarations.dropWhile { it !is FirFileSymbol }
        val file = declarations.firstOrNull() as? FirFileSymbol ?: return false
        if (file != property.getContainingSymbol(context.session)) return false

        // Starting with the CFG for the containing FirFile, check if all following declarations are contained as sub-CFGs.
        // If there is a break in the chain, then the variable assignment is not part of the file CFG, and VAL_REASSIGNMENT should be
        // reported by this checker.
        val containingGraph = declarations
            .map {
                @OptIn(SymbolInternals::class)
                (it.fir as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph
            }
            .reduceOrNull { acc, graph -> graph?.takeIf { acc != null && it in acc.subGraphs } }
        return containingGraph != null
    }

    context(context: CheckerContext)
    private fun isInOwnersInitializer(receiver: FirExpression?, property: FirPropertySymbol): Boolean {
        val uninitializedThisSymbol = (receiver as? FirThisReceiverExpression)?.calleeReference?.boundSymbol ?: return false

        // For a property substitution override, while they may be considered in their owner's initializer, they do not require
        // initialization. The `requiresInitialization(isForInitialization = true)` check correctly handles this case since substitution
        // overrides never have backing fields.
        if (uninitializedThisSymbol != property.getContainingSymbol(context.session)) return false

        val containingDeclarations = context.containingDeclarations
        val index = containingDeclarations.indexOfFirst { it == uninitializedThisSymbol }
        if (index == -1) return false

        for (i in index until containingDeclarations.size) {
            if (containingDeclarations[i] is FirClassSymbol) {
                // Properties need special consideration as some parts are evaluated in-place (initializers) and others are not (accessors).
                // So it is not enough to just check the FirProperty - which is treated as in-place - but the following declaration needs to
                // be checked if and only if it is a property accessor.
                val container = when (val next = containingDeclarations.getOrNull(i + 1)) {
                    is FirPropertySymbol -> containingDeclarations.getOrNull(i + 2)?.takeIf { it is FirPropertyAccessorSymbol } ?: next
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
