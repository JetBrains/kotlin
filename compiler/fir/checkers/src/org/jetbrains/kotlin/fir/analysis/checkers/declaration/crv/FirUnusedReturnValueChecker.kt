/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration.crv

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirUnusedCheckerBase
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.hasSideEffect
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.mustUseReturnValueStatusComponent
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.resolve.ReturnValueStatus

object FirUnusedReturnValueChecker : FirUnusedCheckerBase() {
    context(context: CheckerContext)
    override fun isEnabled(): Boolean =
        context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) != ReturnValueCheckerMode.DISABLED

    context(context: CheckerContext, reporter: DiagnosticReporter, visitor: UsageVisitor)
    private fun checkIfExpressionUnused(
        expression: FirExpression,
        data: UsageState,
    ) {
        if (expression.resolvedType.isIgnorable()) return

        val resolvedSymbol = expression.toResolvedCallableSymbol(context.session)?.originalOrSelf()

        if (expression is FirFunctionCall) {
            // Special case for `x[y] = z` assigment:
            if (expression.origin == FirFunctionCallOrigin.Operator && resolvedSymbol?.name?.asString() == "set") return

            // returnsResultOf contracts:
            if (resolvedSymbol != null && hasContractAndCanBeIgnored(expression, resolvedSymbol, data)) return
        }

        // Special case for `condition() || throw/return` or `condition() && throw/return`:
        if (expression is FirBooleanOperatorExpression && expression.rightOperand.resolvedType.isIgnorable()) return

        reportForSymbol(expression, resolvedSymbol, data)
    }

    context(context: CheckerContext, visitor: UsageVisitor)
    private fun FirAnonymousFunction.canMarkAllReturnPoints(originalFunctionCall: FirFunctionCall): Boolean {
        if (body == null) return false

        val cfg = (this.controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return false
        val returns = cfg.exitNode.previousCfgNodes.mapNotNull { node ->
            when (val exp = node.fir) {
                is FirReturnExpression -> exp
                // BlockExit node contains the whole block expression:
                is FirBlock -> (exp.statements.lastOrNull() as? FirReturnExpression)?.takeIf { it.target.labeledElement == this@canMarkAllReturnPoints }
                else -> null
            }
        }

        for (result in returns) {
            if (result.result.resolvedType.isIgnorable()) continue
            visitor.returnsToCheck[result] = originalFunctionCall
        }
        // Treat function as ignorable, because we'll check return expressions when visitor reaches them.
        // NB: If we never encountered any `return`s, it means that all possible exits are `throw`s or some other Nothings (see ResolveUtils/addReturnToLastStatementIfNeeded),
        // and our judgement that lambda result is ignorable is still correct.
        return true
    }

    context(context: CheckerContext, visitor: UsageVisitor)
    private fun hasContractAndCanBeIgnored(functionCall: FirFunctionCall, resolvedSymbol: FirCallableSymbol<*>, data: UsageState): Boolean {
        val fpIndices = resolvedSymbol.indicesOfPropagatingFunctionalParameters()
        val functionalArguments = fpIndices.mapNotNull { functionCall.arguments.getOrNull(it) }
        if (functionalArguments.isEmpty()) return false

        val functionToReportOn = if (data is UsageState.UsedInReturn) {
            // If call of the function is used in the return, and this return is tracked, it means that it affects whether the outside lambda result is ignorable.
            // In that case, we should report a diagnostic on the outermost contracted function call.
            // If it is not in the map, then it is a non-propagating return and we can ignore the function because it is properly used (see UsageVisitor.checkExpression)
            visitor.returnsToCheck[data.returnExpression] ?: return true
        } else functionCall

        functionalArguments.forEach { functionalArgument ->
            val isIgnorable = when (functionalArgument) {
                is FirAnonymousFunctionExpression -> functionalArgument.anonymousFunction.canMarkAllReturnPoints(functionToReportOn)
                is FirCallableReferenceAccess -> functionalArgument.calleeReference.toResolvedCallableSymbol(discardErrorReference = true)
                    ?.let { refSymbol ->
                        refSymbol.resolvedReturnType.isIgnorable() || !refSymbol.isSubjectToCheck()
                    } ?: false
                else -> false
            }
            if (!isIgnorable) return false // Function only is ignorable if all its propagating functional arguments return ignorable values
        }
        return true
    }

    context(context: CheckerContext)
    private fun FirCallableSymbol<*>.isSubjectToCheck(): Boolean {
        if (this is FirEnumEntrySymbol) return true

        return resolvedStatus.returnValueStatus == ReturnValueStatus.MustUse &&
                // For local functions, annotation resolution happens after status resolution (try crvFull/nestedScopesInsideFile.kt test)
                !context.session.mustUseReturnValueStatusComponent.hasIgnorableLikeAnnotation(resolvedAnnotationClassIds)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter, visitor: UsageVisitor)
    private fun reportForSymbol(
        expression: FirExpression,
        resolvedSymbol: FirCallableSymbol<*>?,
        data: UsageState,
    ) {
        if (resolvedSymbol != null && !resolvedSymbol.isSubjectToCheck()) return
        val functionName = resolvedSymbol?.name
        val targetExpression = if (data is UsageState.UsedInReturn) {
            // Not in map => not inside contracted function call => no need to report
            val unusedFunctionCall = visitor.returnsToCheck[data.returnExpression] ?: return
            visitor.returnsToCheck.values.removeAll { it == unusedFunctionCall } // Remove other entries to avoid multiple UNUSED reports on [unusedFunctionCall]
            unusedFunctionCall
        } else {
            expression
        }
        reporter.reportOn(
            targetExpression.source,
            if (data != UsageState.UnusedFromCoercion) FirErrors.RETURN_VALUE_NOT_USED else FirErrors.RETURN_VALUE_NOT_USED_COERCION,
            functionName
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun createVisitor(): UsageVisitorBase =
        UsageVisitor(context, reporter)

    private class UsageVisitor(
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) : UsageVisitorBase(context, reporter) {
        val returnsToCheck: MutableMap<FirReturnExpression, FirFunctionCall> = hashMapOf()

        override fun checkExpression(expression: FirExpression, data: UsageState) {
            when (data) {
                is UsageState.Used -> return
                is UsageState.UsedInReturn -> if (data.returnExpression !in returnsToCheck) return
                else -> if (!expression.hasSideEffect()) return // Do not report anything FirUnusedExpressionChecker already reported
            }

            context(context, reporter) {
                checkIfExpressionUnused(expression, data)
            }
        }

        override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: UsageState) {
            elvisExpression.lhs.accept(this, data)
            elvisExpression.rhs.accept(this, data)
        }

        override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: UsageState) {
            safeCallExpression.selector.accept(this, data)
        }

        override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: UsageState) {
            checkNotNullCall.argument.accept(this, data)
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: UsageState) {
            typeOperatorCall.arguments.forEach { it.accept(this, data) }
        }

        override fun visitReturnExpression(returnExpression: FirReturnExpression, data: UsageState) {
            returnExpression.acceptChildren(this, UsageState.UsedInReturn(returnExpression))
        }

        override fun visitCallableReferenceAccess(
            callableReferenceAccess: FirCallableReferenceAccess,
            data: UsageState,
        ) {
            if (data is UsageState.UsedInReturn) return
            if (!callableReferenceAccess.resolvedType.isFunctionalTypeThatReturnsUnit(context.session)) return
            val referencedSymbol = callableReferenceAccess.calleeReference.toResolvedCallableSymbol(discardErrorReference = true) ?: return

            context(context, reporter) {
                if (!referencedSymbol.resolvedReturnType.isIgnorable()) // referenceAccess is Unit, referencedSymbol is not => coercion to Unit happened
                    reportForSymbol(callableReferenceAccess, referencedSymbol, UsageState.UnusedFromCoercion)
            }
            // do not visit deeper in any case because there all reference parts are considered used
        }
    }
}
