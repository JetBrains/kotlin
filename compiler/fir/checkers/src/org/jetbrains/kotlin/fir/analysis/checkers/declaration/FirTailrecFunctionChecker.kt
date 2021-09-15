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
        graph.traverse(TraverseDirection.Forward, object : ControlFlowGraphVisitorVoid() {
            override fun visitNode(node: CFGNode<*>) {}

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
                val dispatchReceiverOwner = declaration.dispatchReceiverType?.toSymbol(context.session) as? FirClassSymbol<*>
                val sameReceiver = dispatchReceiver is FirNoReceiverExpression ||
                        (dispatchReceiver is FirThisReceiverExpression && dispatchReceiver.calleeReference.boundSymbol == dispatchReceiverOwner) ||
                        dispatchReceiverOwner?.classKind?.isSingleton == true
                if (!sameReceiver) {
                    // A call on a different receiver might get dispatched to a different method, so it can't be optimized.
                    reporter.reportOn(functionCall.source, FirErrors.NON_TAIL_RECURSIVE_CALL, context)
                } else if (tryScopeCount > 0 || catchScopeCount > 0 || finallyScopeCount > 0) {
                    reporter.reportOn(functionCall.source, FirErrors.TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED, context)
                } else if (node.hasMoreFollowingInstructions(declaration)) {
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
