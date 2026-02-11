/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration.crv

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirUnusedCheckerBase
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.mustUseReturnValueStatusComponent
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.resolve.ReturnValueStatus

object FirUnusedReturnValueChecker : FirUnusedCheckerBase() {
    context(context: CheckerContext)
    override fun isEnabled(): Boolean =
        context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) != ReturnValueCheckerMode.DISABLED

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun reportUnusedExpressionIfNeeded(
        expression: FirExpression,
        hasSideEffects: Boolean,
        data: UsageState,
        source: KtSourceElement?,
    ): Boolean {
        if (!hasSideEffects) return false // Do not report anything FirUnusedExpressionChecker already reported

        if (expression.resolvedType.isIgnorable()) return false

        val resolvedSymbol = expression.toResolvedCallableSymbol(context.session)?.originalOrSelf()

        if (expression is FirFunctionCall) {
            // Special case for `x[y] = z` assigment:
            if (expression.origin == FirFunctionCallOrigin.Operator && resolvedSymbol?.name?.asString() == "set") return false

            // returnsResultOf contracts:
            if (resolvedSymbol != null && hasContractAndPropagatesIgnorable(expression, resolvedSymbol)) return false
            // TODO(KT-84198): technically, this whole shouldUse thing should be recursive, because we may have x?.let { a[b] = c } or x?.let { y?.let { ... }}
        }

        // Special case for `condition() || throw/return` or `condition() && throw/return`:
        if (expression is FirBooleanOperatorExpression && expression.rightOperand.resolvedType.isIgnorable()) return false

        return reportForSymbol(expression, resolvedSymbol, data)
    }

    // TODO(KT-84196): analyze all return points inside the lambda, not just the last statement
    context(context: CheckerContext)
    private fun FirAnonymousFunction.lastStatementIsIgnorable(): Boolean {
        val lastStatement = body?.statements?.lastOrNull() as? FirReturnExpression ?: return false

        val result = lastStatement.result
        if (result.resolvedType.isIgnorable()) return true
        if (result.toResolvedCallableSymbol(context.session)?.isSubjectToCheck() == false) return true
        return false
    }

    context(context: CheckerContext)
    private fun hasContractAndPropagatesIgnorable(functionCall: FirFunctionCall, resolvedSymbol: FirCallableSymbol<*>): Boolean {
        val parameterIndex = resolvedSymbol.getReturnsResultOfParameterIndex() ?: return false
        val functionalArgument = functionCall.arguments.getOrNull(parameterIndex) ?: return false
        return when (functionalArgument) {
            is FirAnonymousFunctionExpression -> functionalArgument.anonymousFunction.lastStatementIsIgnorable()
            is FirCallableReferenceAccess -> functionalArgument.calleeReference.toResolvedCallableSymbol(discardErrorReference = true)
                ?.let { refSymbol ->
                    refSymbol.resolvedReturnType.isIgnorable() || !refSymbol.isSubjectToCheck()
                } ?: false
            else -> false
        }
    }

    context(context: CheckerContext)
    private fun FirCallableSymbol<*>.isSubjectToCheck(): Boolean {
        if (this is FirEnumEntrySymbol) return true

        return resolvedStatus.returnValueStatus == ReturnValueStatus.MustUse &&
                // For local functions, annotation resolution happens after status resolution (try crvFull/nestedScopesInsideFile.kt test)
                !context.session.mustUseReturnValueStatusComponent.hasIgnorableLikeAnnotation(resolvedAnnotationClassIds)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportForSymbol(
        expression: FirExpression,
        resolvedSymbol: FirCallableSymbol<*>?,
        data: UsageState,
    ): Boolean {
        if (resolvedSymbol != null && !resolvedSymbol.isSubjectToCheck()) return false
        val functionName = resolvedSymbol?.name
        reporter.reportOn(
            expression.source,
            if (data == UsageState.Unused) FirErrors.RETURN_VALUE_NOT_USED else FirErrors.RETURN_VALUE_NOT_USED_COERCION,
            functionName
        )
        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun createVisitor(): UsageVisitorBase =
        UsageVisitor(context, reporter)

    private class UsageVisitor(
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) : UsageVisitorBase(context, reporter) {
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

        override fun visitCallableReferenceAccess(
            callableReferenceAccess: FirCallableReferenceAccess,
            data: UsageState,
        ) {
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
