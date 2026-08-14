/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.isBasicFunctionType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType

object FirBreaksUsingUnitReturnChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        if (expression.source?.kind is KtFakeSourceElementKind.ImplicitReturn) return
        if (!expression.result.resolvedType.isUnit) return
        val reverseCallStackIterator = context.callsOrAssignments.listIterator(context.callsOrAssignments.size)
        while (reverseCallStackIterator.hasPrevious()) {
            val call = reverseCallStackIterator.previous() as? FirFunctionCall ?: continue
            val calledFunction = call.calleeReference.toResolvedNamedFunctionSymbol(discardErrorReference = true) ?: continue
            return when {
                call.explicitReceiver != null && calledFunction.isCollectionsFunction -> when {
                    expression.target.labeledElement != call.lambdaArgument ->
                        reporter.reportOn(expression.source, FirErrors.UNIT_RETURN_AS_BREAK, calledFunction)
                    else -> break
                }
                else -> continue
            }
        }
    }

    context(sessionHolder: SessionHolder)
    private val FirNamedFunctionSymbol.isCollectionsFunction: Boolean
        get() = callableId.packageName.asString() == "kotlin.collections" && valueParameterSymbols.singleOrNull()
            ?.resolvedReturnType?.isBasicFunctionType(sessionHolder.session) ?: false

    private inline val FirFunctionCall.lambdaArgument: FirAnonymousFunction?
        get() = (arguments.lastOrNull() as? FirAnonymousFunctionExpression)?.anonymousFunction
}
