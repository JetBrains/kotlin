/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.equalTypes
import org.jetbrains.kotlin.fir.types.isBasicFunctionType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType

private fun <T> List<T>.reverseIterator(): ListIterator<T> = listIterator(size)

private val collectionFunctions = setOf(
    "forEach",
    "map",
    "takeWhile",
    "all",
    "any",
    "first",
    "associate",
    "dropWhile",
    "filter",
    "find",
    "flatMap",
    "fold",
    "onEach",
    "reduce"
)

context(sessionHolder: SessionHolder)
private inline val FirNamedFunctionSymbol.isCollectionsLikeFunction: Boolean
    get() = collectionFunctions.any { name.asString().contains(it, ignoreCase = true) }
            && isInline
            && valueParameterSymbols.lastOrNull()?.resolvedReturnType?.isBasicFunctionType(sessionHolder.session) ?: false

private inline val FirNamedFunctionSymbol.isFromCollectionsPackage: Boolean get() = callableId.packageName.asString() == "kotlin.collections"

context(sessionHolder: SessionHolder)
private inline val Pair<FirNamedFunctionSymbol, FirFunctionCall>.collectionsFunctionLambda: FirAnonymousFunction?
    get() = let { [callableFunction, call] ->
        when {
            callableFunction.isCollectionsLikeFunction -> (call.arguments.lastOrNull() as? FirAnonymousFunctionExpression)?.anonymousFunction
            else -> null
        }
    }

context(context: CheckerContext)
private infix fun FirFunction.isDeclaredBeforeLambda(lambda: FirAnonymousFunction): Boolean {
    val reverseContainingDeclarationIterator = context.containingDeclarations.reverseIterator()
    while (reverseContainingDeclarationIterator.hasPrevious()) {
        return when (reverseContainingDeclarationIterator.previous()) {
            symbol -> true
            lambda.symbol -> false
            else -> continue
        }
    }
    return false
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private inline fun FirReturnExpression.checkBreak(
    targetTypeCheck: (ConeKotlinType) -> Boolean,
    callTypeCheck: (targetType: ConeKotlinType, callReturnType: ConeKotlinType) -> Boolean = { _, _ -> true },
    diagnosticFactory: KtDiagnosticFactory1<FirNamedFunctionSymbol>,
    collectionsDiagnosticFactory: KtDiagnosticFactory1<FirNamedFunctionSymbol>,
) {
    if (source?.kind is KtFakeSourceElementKind.ImplicitReturn) return
    val targetType = result.resolvedType
    if (!targetTypeCheck(targetType)) return
    val targetedFunction = target.labeledElement
    val reverseCallStackIterator = context.callsOrAssignments.reverseIterator()
    while (reverseCallStackIterator.hasPrevious()) {
        val call = reverseCallStackIterator.previous() as? FirFunctionCall ?: continue
        val calledFunction = call.calleeReference.toResolvedNamedFunctionSymbol(discardErrorReference = true) ?: continue
        return (calledFunction to call).collectionsFunctionLambda?.let { lambdaArgument ->
            when {
                targetedFunction.isLocal && targetedFunction isDeclaredBeforeLambda lambdaArgument -> break
                targetedFunction != lambdaArgument && callTypeCheck(targetType, call.resolvedType) ->
                    reporter.reportOn(
                        source = source,
                        factory = if (calledFunction.isFromCollectionsPackage) collectionsDiagnosticFactory else diagnosticFactory,
                        a = calledFunction
                    )
                else -> continue
            }
        } ?: when {
            call.arguments.any { it is FirAnonymousFunctionExpression && it.anonymousFunction == targetedFunction } -> break
            else -> continue
        }
    }
}

object FirBreaksUsingUnitReturnChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression): Unit = expression.checkBreak(
        targetTypeCheck = ConeKotlinType::isUnit,
        diagnosticFactory = FirErrors.UNIT_RETURN_AS_BREAK,
        collectionsDiagnosticFactory = FirErrors.UNIT_RETURN_AS_BREAK_IN_STDLIB_FUNCTION
    )
}

object FirBreaksUsingMatchingTypeReturnChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression): Unit = expression.checkBreak(
        targetTypeCheck = { !it.isUnit },
        callTypeCheck = { targetType, callType -> targetType.equalTypes(callType, context.session) },
        diagnosticFactory = FirErrors.MATCHING_TYPE_RETURN_AS_BREAK,
        collectionsDiagnosticFactory = FirErrors.MATCHING_TYPE_RETURN_AS_BREAK_IN_STDLIB_FUNCTION
    )
}

object FirBreaksUsingAnyTypeReturnChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression): Unit = expression.checkBreak(
        targetTypeCheck = { !it.isUnit },
        callTypeCheck = { targetType, callType -> !targetType.equalTypes(callType, context.session) },
        diagnosticFactory = FirErrors.ANY_TYPE_RETURN_AS_BREAK,
        collectionsDiagnosticFactory = FirErrors.ANY_TYPE_RETURN_AS_BREAK_IN_STDLIB_FUNCTION
    )
}
