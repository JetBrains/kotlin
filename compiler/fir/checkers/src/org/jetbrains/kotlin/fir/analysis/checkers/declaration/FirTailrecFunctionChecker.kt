/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.cfa.util.TraverseDirection
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverse
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.isTailRec
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.toSymbol

object FirTailrecFunctionChecker : FirSimpleFunctionChecker() {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isTailRec) return
        if (!declaration.isEffectivelyFinal(context)) {
            reporter.reportOn(declaration.source, FirErrors.TAILREC_ON_VIRTUAL_MEMBER_ERROR, context)
        }
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return

        var tryScopeCount = 0
        var catchScopeCount = 0
        var finallyScopeCount = 0
        var tailrecCount = 0
        var lambdaScopeCount = 0
        graph.traverse(TraverseDirection.Forward, object : ControlFlowGraphVisitorVoid() {
            override fun visitNode(node: CFGNode<*>) {}
            override fun visitPostponedLambdaEnterNode(node: PostponedLambdaEnterNode) {
                lambdaScopeCount++
            }

            override fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode) {
                lambdaScopeCount--
            }

            override fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode) {
                tryScopeCount++
            }

            override fun visitTryMainBlockExitNode(node: TryMainBlockExitNode) {
                tryScopeCount--
            }

            override fun visitCatchClauseEnterNode(node: CatchClauseEnterNode) {
                catchScopeCount++
            }

            override fun visitCatchClauseExitNode(node: CatchClauseExitNode) {
                catchScopeCount--
            }

            override fun visitFinallyBlockEnterNode(node: FinallyBlockEnterNode) {
                finallyScopeCount++
            }

            override fun visitFinallyBlockExitNode(node: FinallyBlockExitNode) {
                finallyScopeCount--
            }

            override fun visitFunctionCallNode(node: FunctionCallNode) {
                val functionCall = node.fir
                val resolvedSymbol = functionCall.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
                if (resolvedSymbol != declaration.symbol) return
                if (functionCall.arguments.size != resolvedSymbol.valueParameterSymbols.size && resolvedSymbol.isOverride) {
                    // Overridden functions using default arguments at tail call are not included: KT-4285
                    reporter.reportOn(functionCall.source, FirErrors.NON_TAIL_RECURSIVE_CALL, context)
                    return
                }
                val dispatchReceiver = functionCall.dispatchReceiver
                // A tailrec call does not support changing dispatchers. Here we report changing dispatch receiver if the dispatch receiver
                // is present and not a `this` or a singleton. For the `this` check, we don't need to actually compare if the dispatch
                // receiver `this` references the same `this` made available from `declaration`. This is because
                // 1. if `this` is not labeled, then it references the innermost `this` receiver. If the innermost scope is not the
                //    `declaration` body, then follow-up checks on following nodes would report there to be more instructions, which would
                //    then make this call non-tailrec.
                // 2. If `this` is labeled, then one of the following is possible.
                //    a. the call is in some context that has additional implicit `this` declared. But this can only happen if the call is
                //       placed inside some extension lambda, which would be covered by the later check on exiting node.
                //    b. `declaration` is a member function in a local class and the receiver is a labeled `this` pointing to the outer
                //       non-local class. In this case, the resolved symbol cannot be the same as the symbol of `declaration`, and this case
                //       is already bailed out earlier. So there is no need to report anything.
                //    c. `declaration` is a member function of an inner class and the receiver is a labeled `this` pointing to the outer
                //       class. The reasoning is the same with b.
                if (dispatchReceiver !is FirThisReceiverExpression &&
                    dispatchReceiver !is FirNoReceiverExpression &&
                    (declaration.dispatchReceiverType?.toSymbol(context.session) as? FirClassSymbol<*>)?.classKind?.isSingleton != true
                ) {
                    reporter.reportOn(functionCall.source, FirErrors.NON_TAIL_RECURSIVE_CALL, context)
                    return
                }
                if (tryScopeCount > 0 || catchScopeCount > 0 || finallyScopeCount > 0) {
                    reporter.reportOn(functionCall.source, FirErrors.TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED, context)
                } else if (lambdaScopeCount > 0 || node.hasMoreFollowingInstructions(declaration)) {
                    reporter.reportOn(functionCall.source, FirErrors.NON_TAIL_RECURSIVE_CALL, context)
                } else if (!node.isDead) {
                    tailrecCount++
                }
            }
        })
        if (tailrecCount == 0) {
            reporter.reportOn(declaration.source, FirErrors.NO_TAIL_CALLS_FOUND, context)
        }
    }

    private fun CFGNode<*>.hasMoreFollowingInstructions(tailrecFunction: FirSimpleFunction): Boolean {
        for (next in followingNodes) {
            val edge = outgoingEdges.getValue(next)
            if (!edge.kind.usedInCfa || edge.kind.isDead) continue
            if (edge.kind.isBack) return true
            val hasMore = when (next) {
                // If exiting another function, then it means this call is inside a nested local function, in which case, it's not a tailrec call.
                is FunctionExitNode -> return next.fir != tailrecFunction
                is JumpNode, is BinaryAndExitNode, is BinaryOrExitNode, is WhenBranchResultExitNode, is WhenExitNode, is BlockExitNode ->
                    next.hasMoreFollowingInstructions(tailrecFunction)
                else -> return true
            }
            if (hasMore) return hasMore
        }
        return false
    }
}
