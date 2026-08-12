/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.isBasicFunctionType
import org.jetbrains.kotlin.fir.types.isBasicSuspendFunctionType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirBreaksUsingRunChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {

    private object UsedToBreakUsingRun : FirDeclarationDataKey()

    private var FirAnonymousFunction.isMarkedByBreakUsingRunFlag: Boolean? by FirDeclarationDataRegistry.data(UsedToBreakUsingRun)

    private val FirAnonymousFunction.isMarkedByBreakUsingRun: Boolean get() = isMarkedByBreakUsingRunFlag ?: false

    private lateinit var runFunctions: List<FirCallableSymbol<*>>

    context(sessionHolder: SessionHolder)
    private fun tryCollectingRunFunctions(): Boolean {
        if (!::runFunctions.isInitialized) {
            val foundFunctions = sessionHolder.session.symbolProvider.getTopLevelFunctionSymbols(
                packageFqName = FqName("kotlin"),
                name = Name.identifier("run")
            )
            if (foundFunctions.isEmpty()) return false
            runFunctions = foundFunctions
        }
        return true
    }

    context(sessionHolder: SessionHolder)
    private inline val FirNamedFunctionSymbol.hasForEachLikeSignature: Boolean
        get() = valueParameterSymbols.singleOrNull()?.resolvedReturnType?.let {
            (it.isBasicFunctionType(sessionHolder.session) || it.isBasicSuspendFunctionType(sessionHolder.session)) && it.typeArguments.size > 1
        } ?: false

    private inline val FirFunctionCall.singleLambdaArgument: FirAnonymousFunction?
        get() = (argument as? FirAnonymousFunctionExpression)?.anonymousFunction?.takeIf(FirAnonymousFunction::isLambda)

    private inline val FirFunctionCall.isMarked: Boolean
        get() = singleLambdaArgument?.isMarkedByBreakUsingRun ?: false

    private fun FirFunctionCall.markLambdaArgument() {
        singleLambdaArgument?.isMarkedByBreakUsingRunFlag = true
    }

    private sealed interface BreakPathInfo {

        context(context: CheckerContext, reporter: DiagnosticReporter)
        fun reportDiagnostics()

        data class Complete(val runCall: FirFunctionCall, val capturedForEachLikeCalls: List<FirFunctionCall>) : BreakPathInfo {
            context(context: CheckerContext, reporter: DiagnosticReporter)
            override fun reportDiagnostics() {
                reporter.reportOn(runCall.source, FirErrors.RUN_CALL_USED_TO_BREAK)
                runCall.markLambdaArgument()
                capturedForEachLikeCalls.forEach {
                    reporter.reportOn(it.source, FirErrors.RUN_BROKEN_FOR_EACH_LIKE_CALL)
                    it.markLambdaArgument()
                }
            }
        }

        data class Partial(val capturedForEachLikeCalls: List<FirFunctionCall>) : BreakPathInfo {
            context(context: CheckerContext, reporter: DiagnosticReporter)
            override fun reportDiagnostics() {
                capturedForEachLikeCalls.forEach {
                    reporter.reportOn(it.source, FirErrors.RUN_BROKEN_FOR_EACH_LIKE_CALL)
                    it.markLambdaArgument()
                }
            }
        }
    }

    context(context: CheckerContext)
    private fun findRunCallUsingLambda(lambda: FirAnonymousFunction): BreakPathInfo? {
        val reverseIterator = context.callsOrAssignments.listIterator(context.callsOrAssignments.size)
        val capturedForEachLikeCalls = mutableListOf<FirFunctionCall>()
        while (reverseIterator.hasPrevious()) {
            val call = reverseIterator.previous() as? FirFunctionCall ?: continue
            val function = call.calleeReference.toResolvedNamedFunctionSymbol(discardErrorReference = true) ?: continue
            return when {
                function.hasForEachLikeSignature -> when {
                    call.isMarked -> BreakPathInfo.Partial(capturedForEachLikeCalls)
                    else -> {
                        capturedForEachLikeCalls += call
                        continue
                    }
                }
                // Need to break if we find the function the lambda belongs to BUT it's not the `run` function,
                // otherwise we are traversing the entire call/assignment stack
                call.arguments.any { it is FirAnonymousFunctionExpression && it.anonymousFunction == lambda } -> when {
                    function in runFunctions -> BreakPathInfo.Complete(call, capturedForEachLikeCalls)
                    else -> break
                }
                else -> continue
            }
        }
        return null
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        if (!tryCollectingRunFunctions()) return
        val target = (expression.target.labeledElement as? FirAnonymousFunction)?.takeIf { it.isLambda } ?: return
        val info = findRunCallUsingLambda(target) ?: return
        reporter.reportOn(expression.source, FirErrors.RUN_RETURN_USED_AS_BREAK)
        info.reportDiagnostics()
    }
}
