/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.isNothingOrNullableNothing
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId

private fun FirAnnotation.isMustUseReturnValue(session: FirSession): Boolean =
    toAnnotationClassId(session)?.let { it.relativeClassName.toString() == "MustUseReturnValue" && it.packageFqName.asString() == "kotlin" } == true

private fun FirAnnotation.isIgnorableValue(session: FirSession): Boolean =
    toAnnotationClassId(session)?.let { it.relativeClassName.toString() == "IgnorableReturnValue" && it.packageFqName.asString() == "kotlin" } == true

object FirReturnValueAnnotationsChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) != ReturnValueCheckerMode.DISABLED) return

        val session = context.session
        val annotations = declaration.annotations
        // @MustUseValue and @Ignorable have targets sets without intersection, so it is safe to query them both at once.
        val annotation = annotations.find { it.isMustUseReturnValue(session) } ?: annotations.find { it.isIgnorableValue(session) } ?: return

        reporter.reportOn(
            annotation.source,
            FirErrors.IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED,
            context
        )
    }
}

object FirUnusedReturnValueChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) == ReturnValueCheckerMode.DISABLED) return
        if (expression !is FirExpression) return
        if (expression is FirBlock) return

        if (expression.isLhsOfAssignment(context)) return
        if (expression is FirAnnotation) return

        if (expression.isLocalPropertyOrParameterOrThis()) return

        // Do not check everything marked as 'propagating' in walkUp and still `is FirExpression`:
        when (expression) {
            is FirSmartCastExpression,
            is FirTypeOperatorCall,
            is FirCheckNotNullCall,
            is FirTryExpression,
            is FirWhenExpression,
            is FirSafeCallExpression,
            is FirElvisExpression,
                -> return
        }

        // Try resolve reference to see if it is excluded
        val calleeReference = expression.toReference(context.session)
        val resolvedReference = calleeReference?.resolved

        // Exclusions
        val resolvedSymbol = resolvedReference?.toResolvedCallableSymbol()

        if (resolvedSymbol != null && !resolvedSymbol.isSubjectToCheck(context.session)) return

        if (resolvedSymbol?.isExcluded(context.session) == true) return

        // Ignore Unit or Nothing
        if (expression.resolvedType.run { isNothingOrNullableNothing || isUnit }) return

        // If not the outermost call, then it is used as an argument
        if (context.callsOrAssignments.lastOrNull { it != expression } != null) return

        if (hasUsages(context, expression)) return

        reporter.reportOn(
            expression.source,
            FirErrors.RETURN_VALUE_NOT_USED,
            resolvedSymbol?.callableId?.toString() ?: "<${expression.render()}>",
            context
        )
    }

    private fun hasUsages(context: CheckerContext, thisExpression: FirExpression): Boolean {
        val stack = context.containingElements.asReversed()
        var lastPropagating: FirElement = thisExpression

        for (e in stack) {
            if (e == thisExpression) continue
            when (e) {
                // Propagate further:
                is FirSmartCastExpression,
                is FirArgumentList,
                is FirTypeOperatorCall,
                is FirCheckNotNullCall,
                is FirCatch,
                is FirElvisExpression, // both lhs and rhs are propagating (for now)
                    -> {
                    lastPropagating = e
                    continue
                }

                // Conditional propagation:

                is FirTryExpression -> {
                    // Only try and catch do propagation, finally does not use its last statement
                    if (e.tryBlock == lastPropagating || lastPropagating in e.catches) {
                        lastPropagating = e
                        continue
                    }
                    return false
                }

                is FirWhenBranch -> {
                    // If it is condition, it is used, otherwise it is result and we propagate up:
                    if (e.condition == lastPropagating) return true
                    lastPropagating = e
                    continue
                }

                is FirWhenExpression -> {
                    // If it is subject, it is used, otherwise it is branch and we propagate up:
                    if (e.subject == lastPropagating) return true
                    lastPropagating = e
                    continue
                }

                is FirSafeCallExpression -> {
                    // Receiver is always used, selector is propagating:
                    if (e.receiver == lastPropagating) return true
                    lastPropagating = e
                    continue
                }

                // Expressions that always use what's down the stack:

                is FirReturnExpression -> return true // result == given
                is FirThrowExpression -> return true // exception == given
                is FirComparisonExpression -> return true // compareToCall == given
                is FirBooleanOperatorExpression -> return true // leftOperand == given || rightOperand == given

                is FirEqualityOperatorCall -> return true // given in argumentList.arguments
                is FirStringConcatenationCall -> return true // given in argumentList.arguments
                is FirGetClassCall -> return true // given in argumentList.arguments
                is FirArrayLiteral -> return true // given in argumentList.arguments

                // Initializers
                // FirField can occur in `by` interface delegation
                is FirProperty, is FirValueParameter, is FirField -> return true

                // Conditional usage:

                is FirBlock -> {
                    // Special case: ++x is desugared to FirBlock, we consider result of pre/post increment as discardable.
                    if (e.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) return true

                    // FirBlock result is the last statement, other statements are not used
                    if (e.statements.lastOrNull() == lastPropagating) {
                        lastPropagating = e
                        continue
                    }
                    return false
                }

                is FirLoop -> return e.condition == lastPropagating

                else -> return false
            }
        }
        return false
    }


    private fun FirExpression.isLocalPropertyOrParameterOrThis(): Boolean {
        if (this is FirThisReceiverExpression) return true
        if (this !is FirPropertyAccessExpression) return false
        return when (calleeReference.symbol) {
            is FirValueParameterSymbol -> true
            is FirPropertySymbol -> calleeReference.toResolvedPropertySymbol()?.isLocal == true
            else -> false
        }
    }

    private fun FirCallableSymbol<*>.isExcluded(session: FirSession): Boolean = annotations.any { it.isIgnorableValue(session) }

    private fun FirCallableSymbol<*>.isSubjectToCheck(session: FirSession): Boolean {
        // TODO: treating everything in kotlin. seems to be the easiest way to handle builtins, FunctionN, etc..
        // This should be removed after bootstrapping and recompiling stdlib in FULL mode
        if (this.callableId.packageName.asString() == "kotlin") return true
        callableId.ifMappedTypeCollection { return it }

        val classOrFile = getContainingSymbol(session) ?: return false
        return classOrFile.annotations.any { it.isMustUseReturnValue(session) }
    }

    // TODO: after kotlin.collections package will be bootstrapped and @MustUseReturnValue-annotated,
    // this list should contain only typealiased Java types (HashSet, StringBuilder, etc.)
    private inline fun CallableId.ifMappedTypeCollection(nonIgnorableCollectionMethod: (Boolean) -> Unit) {
        val packageName = packageName.asString()
        if (packageName != "kotlin.collections" && packageName != "java.util") return
        val className = className?.asString() ?: return
        if (className !in setOf(
                "Collection",
                "MutableCollection",
                "List",
                "MutableList",
                "ArrayList",
                "Set",
                "MutableSet",
                "HashSet",
                "LinkedHashSet",
                "Map",
                "MutableMap",
                "HashMap",
                "LinkedHashMap",
                "ArrayDeque"
            )
        ) return
        nonIgnorableCollectionMethod(
            callableName.asString() !in setOf(
                "add",
                "addAll",
                "remove",
                "removeAt",
                "set",
                "put",
                "retainAll",
                "removeLast"
            )
        )
    }
}
