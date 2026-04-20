/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.isTailRec
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

object FirTailrecFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
    // !!!
    // Order is important!
    // Must be ordered from least to most severe.
    // !!!
    private enum class TailrecCallKind {
        Valid,
        FollowingInstructions,
        InsideTry,
        WrongReceiver,
        DefaultArguments,
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (!declaration.isTailRec) return
        if (!(declaration.isEffectivelyFinal() || declaration.visibility == Visibilities.Private)) {
            reporter.reportOn(declaration.source, FirErrors.TAILREC_ON_VIRTUAL_MEMBER_ERROR)
        }
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return

        val tailrecCalls = mutableMapOf<FirFunctionCall, TailrecCallKind>()
        graph.traverse(object : ControlFlowGraphVisitorVoid() {
            override fun visitNode(node: CFGNode<*>) {}

            override fun visitFunctionCallExitNode(node: FunctionCallExitNode) {
                val functionCall = node.fir
                val resolvedSymbol = functionCall.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
                if (resolvedSymbol != declaration.symbol) return
                if (functionCall.arguments.size != resolvedSymbol.valueParameterSymbols.size && resolvedSymbol.isOverride) {
                    // Overridden functions using default arguments at tail call are not included: KT-4285
                    tailrecCalls[functionCall] = TailrecCallKind.DefaultArguments
                    return
                }
                val dispatchReceiver = functionCall.dispatchReceiver
                val dispatchReceiverOwner = declaration.dispatchReceiverType?.toClassSymbol()
                val sameReceiver = dispatchReceiver == null ||
                        (dispatchReceiver is FirThisReceiverExpression && dispatchReceiver.calleeReference.boundSymbol == dispatchReceiverOwner) ||
                        dispatchReceiverOwner?.classKind?.isSingleton == true
                if (!sameReceiver) {
                    // A call on a different receiver might get dispatched to a different method, so it can't be optimized.
                    tailrecCalls[functionCall] = TailrecCallKind.WrongReceiver
                } else if (node.hasMoreFollowingInstructions(declaration, context.session)) {
                    tailrecCalls[functionCall] = TailrecCallKind.FollowingInstructions
                } else if (!node.isDead) {
                    tailrecCalls[functionCall] = TailrecCallKind.Valid
                }
            }
        })

        // Check that tailrec calls are not inside try-expressions.
        // DATA(Boolean) = Within try-expression.
        declaration.body?.accept(object : FirVisitor<Unit, Boolean>() {
            override fun visitElement(element: FirElement, data: Boolean) {
                element.acceptChildren(this, data)
            }

            override fun visitTryExpression(tryExpression: FirTryExpression, data: Boolean) {
                tryExpression.acceptChildren(this, data = true)
            }

            override fun visitFunctionCall(functionCall: FirFunctionCall, data: Boolean) {
                functionCall.acceptChildren(this, data)
                if (!data) return // Not within try-expression.

                // Increase severity of tailrec kind to 'InsideTry'.
                val kind = tailrecCalls[functionCall]
                if (kind != null && kind < TailrecCallKind.InsideTry) {
                    tailrecCalls[functionCall] = TailrecCallKind.InsideTry
                }
            }
        }, data = false)

        var tailrecCount = 0
        for ((functionCall, kind) in tailrecCalls) {
            when (kind) {
                TailrecCallKind.Valid -> tailrecCount++
                TailrecCallKind.FollowingInstructions -> reporter.reportOn(functionCall.source, FirErrors.NON_TAIL_RECURSIVE_CALL)
                TailrecCallKind.InsideTry -> reporter.reportOn(functionCall.source, FirErrors.TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED)
                TailrecCallKind.WrongReceiver -> reporter.reportOn(functionCall.source, FirErrors.NON_TAIL_RECURSIVE_CALL)
                TailrecCallKind.DefaultArguments -> reporter.reportOn(functionCall.source, FirErrors.NON_TAIL_RECURSIVE_CALL)
            }
        }

        if (tailrecCount == 0) {
            reporter.reportOn(declaration.source, FirErrors.NO_TAIL_CALLS_FOUND)
        }
    }

    private fun CFGNode<*>.hasMoreFollowingInstructions(tailrecFunction: FirFunction, session: FirSession): Boolean {
        val returnTypeMayBeNullable = tailrecFunction.returnTypeRef.coneType.canBeNull(session)
        for (next in followingNodes) {
            val edge = edgeTo(next)
            if (!edge.kind.usedInCfa || edge.kind.isDead) continue
            if (edge.kind.isBack) return true
            val hasMore = when (next) {
                // If exiting another function, then it means this call is inside a nested local function, in which case, it's not a tailrec call.
                is FunctionExitNode -> return next.fir != tailrecFunction
                is ElvisLhsExitNode if !returnTypeMayBeNullable ->
                    next.correspondingElvisExitNode?.hasMoreFollowingInstructions(tailrecFunction, session) ?: return true
                is TailrecExitNodeMarker ->
                    next.hasMoreFollowingInstructions(tailrecFunction, session)
                else -> return true
            }
            if (hasMore) return true
        }
        return false
    }

    val ElvisLhsExitNode.correspondingElvisExitNode: ElvisExitNode?
        get() = followingNodes.firstIsInstanceOrNull<ElvisLhsIsNotNullNode>()?.followingNodes?.firstIsInstanceOrNull<ElvisExitNode>()
}
