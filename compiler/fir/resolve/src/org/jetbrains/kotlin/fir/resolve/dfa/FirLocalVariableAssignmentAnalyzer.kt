/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.referredPropertySymbol
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

/**
 *  Helper that checks if an access to a local variable access is stable.
 *
 *  To determine the stability of an access, call [isAccessToUnstableLocalVariable]. Note that the class contains mutable states. So
 *  [isAccessToUnstableLocalVariable] only works for an access during the natural FIR tree traversal. This class will not work if one
 *  queries after the traversal is done.
 **/
internal class FirLocalVariableAssignmentAnalyzer {
    private var rootFunction: FirFunction? = null
    private var assignedLocalVariablesByFunction: Map<FirFunctionSymbol<*>, FunctionFork>? = null

    private val functionScopes: Stack<Pair<FunctionFork?, MutableSet<FirProperty>>> = stackOf()

    // Example of control-flow-postponed lambdas: callBoth({ a.x }, { a = null })
    // Lambdas are called in an unknown order, so control flow edges to both of them go from before the call.
    // However, the assignment in the second lambda should invalidate the smart cast in the first.
    //
    // Example of data-flow-postponed lambdas: genericFunction(run { a = null }, a.x)
    // Although in control flow the first lambda always executes before the second argument, in order to determine
    // the type arguments to `run` - and thus resolve its argument lambda - we may have to resolve `a.x` first.
    // Because smart casts can only get information from statements that have previously been resolved, data flows
    // from the result of `run` to the result of `genericFunction` so smart casts should be prohibited in `a.x`.
    //
    // This mirrors `ControlFlowGraphBuilder.postponedLambdaExits`.
    private val postponedLambdas: Stack<MutableMap<FunctionFork, Boolean /* data-flow only */>> = stackOf()

    fun reset() {
        rootFunction = null
        assignedLocalVariablesByFunction = null
        postponedLambdas.reset()
        functionScopes.reset()
    }

    /** Checks whether the given access is an unstable access to a local variable at this moment. */
    @OptIn(DfaInternals::class)
    fun isAccessToUnstableLocalVariable(fir: FirExpression): Boolean {
        if (assignedLocalVariablesByFunction == null) return false

        val realFir = fir.unwrapElement() as? FirQualifiedAccessExpression ?: return false
        val property = realFir.referredPropertySymbol?.fir ?: return false
        // Have data => have a root function => functionScopes is not empty.
        return property in functionScopes.top().second || postponedLambdas.all().any { lambdas ->
            // Control-flow-postponed lambdas' assignments should be in `functionScopes.top()`.
            // The reason we can't check them here is that one of the entries may be the lambda
            // that is currently being analyzed, and assignments in it are, in fact, totally fine.
            lambdas.any { (lambda, dataFlowOnly) -> dataFlowOnly && property in lambda.assignedInside }
        }
    }

    private fun getInfoForLocalFunction(symbol: FirFunctionSymbol<*>): FunctionFork? {
        val root = rootFunction ?: return null
        if (root.symbol == symbol) return null
        val cachedMap = assignedLocalVariablesByFunction ?: run {
            val data = MiniCfgBuilder.MiniCfgData()
            MiniCfgBuilder().visitElement(root, data)
            data.functionForks.also { assignedLocalVariablesByFunction = it }
        }
        return cachedMap[symbol]
    }

    fun enterFunction(function: FirFunction) {
        val prohibitSmartCasts = functionScopes.topOrNull()?.second?.toMutableSet()
            ?: mutableSetOf<FirProperty>().also { rootFunction = function }

        val info = getInfoForLocalFunction(function.symbol)
        for (concurrentLambdas in postponedLambdas.all()) {
            for ((otherLambda, dataFlowOnly) in concurrentLambdas) {
                if (!dataFlowOnly && otherLambda != info) {
                    prohibitSmartCasts += otherLambda.assignedInside
                }
            }
        }
        if (function !is FirAnonymousFunction || !function.invocationKind.isInPlace) {
            // The function may be stored and then called later =>
            for ((outerInfo, prohibitInOuterScope) in functionScopes.all()) {
                if (info != null) {
                    // => any access of the variables it touches is no longer smartcastable ever.
                    //
                    // TODO: this incorrectly affects separate branches that are visited after this one:
                    //    if (p is Something) {
                    //        if (condition)) {
                    //            foo { p = whatever }
                    //            p.memberOfSomething // Bad
                    //        } else {
                    //            p.memberOfSomething // Marked as an error, but actually OK
                    //        }
                    //        p.memberOfSomething // Bad
                    //    }
                    //   FE1.0 has the same behavior.
                    prohibitInOuterScope.addAll(info.assignedInside)
                }
                if (outerInfo != null) {
                    // => any write to a variable outside the function invalidates smart casts inside it
                    prohibitSmartCasts += outerInfo.assignedLater
                }
            }
            if (info != null) {
                prohibitSmartCasts.addAll(info.assignedLater)
                // => it may be called twice in parallel, so it can't even trust its own assignments
                prohibitSmartCasts.addAll(info.assignedInside)
            }
        }
        functionScopes.push(info to prohibitSmartCasts)
    }

    fun exitFunction() {
        functionScopes.pop()
        if (functionScopes.isEmpty) {
            rootFunction = null
            assignedLocalVariablesByFunction = null
        }
    }

    fun enterFunctionCall(lambdaArgs: Collection<FirAnonymousFunction>) {
        // If not inside a function at all, then there is no concept of a local and nothing to track.
        if (rootFunction == null) return
        postponedLambdas.push(lambdaArgs.mapNotNull { getInfoForLocalFunction(it.symbol) }.associateWithTo(mutableMapOf()) { false })
    }

    fun exitFunctionCall(callCompleted: Boolean) {
        if (rootFunction == null) return
        val lambdasInCall = postponedLambdas.pop()
        if (!callCompleted) {
            // TODO: this has the same problem as above:
            //   if (p is Something) {
            //       foo(
            //           if (condition)
            //               someNotCompletedCall { p = whatever }
            //           else
            //               someNotCompletedCall { p.memberOfSomething }, // Marked as an error, but actually OK
            //           someCall { p.memberOfSomething } // Bad
            //       )
            //   }
            //  And also as above, FE1.0 produces the same error.
            //
            // TODO: this should never return null. Also somehow throwing an exception here leads to weird effects:
            //  apparently the compiler attempts to continue somewhere...
            lambdasInCall.keys.associateWithTo(postponedLambdas.topOrNull() ?: return) { true }
        }
    }

    companion object {
        /**
         * Computes assigned local variables in each execution path. This analyzer runs before BODY_RESOLVE. Hence, it works on
         * syntactical information only.
         *
         * # Note on implementation detail
         *
         * The analyzer constructs a mini control flow graph that captures forking of execution path. The only information it cares about,
         * though, is which variables are assigned in a given node or any transitive successor; thus nodes which add no extra information
         * and have only one predecessor are elided from the resulting graph.
         *
         * For example, consider the following code.
         *
         * ```
         * fun test() {
         *   var x: Int = 0
         *   var y: Int = 0
         *   var z: Int = 0
         *   if (true) {
         *     run {
         *       x = 1
         *       var a = 1
         *       a = 2
         *     }
         *   } else {
         *     x = 2
         *     y = 2
         *   }
         *   z = 3
         * }
         * ```
         *
         * The generated mini CFG looks like the following, with assigned local variables annotated after each node in curly brackets.
         *
         *     ┌───────┐
         *     │ entry │ {x y z}
         *     └─┬─┬─┬─┘
         *       │ │ │ fallback
         *       │ │ └─────────────────────────────┐
         *       │ │ false                         │
         *       │ └─────────────────────────┐     │
         *       │ true                      │     │
         *     ┌ ┴ ─ ─ ┐                 ┌ ─ ┴ ─ ┐ │
         *     │ then  │                 │ else  │ │
         *     └ ┬ ─ ┬ ┘                 └ ─ ┬ ─ ┘ │
         *       │   │ normal execution      │     │
         *       │   └─────────────┐         │     │
         *       │ lambda arg      │         │     │
         *     ┌─┴──────┐      ┌───┴───┐     │     │
         *     │ lambda │ {x}  │ empty │ {z} │     │
         *     └────────┘      └───┬───┘     │     │
         *       ┌─────────────────┘         │     │
         *       │ ┌─────────────────────────┘     │
         *       │ │ ┌─────────────────────────────┘
         *     ┌─┴─┴─┴─┐
         *     │ after │ {z}
         *     │  if   │
         *     └───────┘
         *
         * Some notes on why each node contains what it contains:
         * - the nodes with the dashed outline and no associated set, "then" and "else", are elided, as they are neither merge points
         *   nor do they have any useful information - we will never need to know which variables are assigned to after entering the
         *   "else" branch, for example, because that code path will encounter no lambdas. Instead, any assignments done in elided
         *   blocks are immediately propagated to the parents, which is why the "entry" node contains `y`.
         *
         * - the "lambda" and "empty" nodes are not merge points, but they are what the CFG is for: if the lambda is not called-in-place,
         *   then all variables it assigns to cannot be smartcasted in "empty" or its successors and vice versa, as the body of the lambda
         *   can be executed at arbitrary points on that path.
         *
         * - changes to `z` is captured and back-propagated to all earlier nodes as desired.
         *
         * - `a` is not recorded anywhere because it's declared inside the lambda, and no statement needed a new block after
         *   the declaration.
         *
         * Because names are not resolved at this point, we manually track local variable declarations and resolve them along the way
         * so that shadowed names are handled correctly. This works because local variables at any scope have higher priority
         * than members on implicit receivers, even if the implicit receiver is introduced by a later scope.
         */
        class FunctionFork(
            val assignedLater: Set<FirProperty>,
            val assignedInside: Set<FirProperty>,
        )

        private class MiniFlow(val parents: Set<MiniFlow>) {
            val assignedLater: MutableSet<FirProperty> = mutableSetOf()

            fun fork(): MiniFlow = MiniFlow(setOf(this))

            companion object {
                fun start() = MiniFlow(emptySet())
            }
        }

        private class MiniCfgBuilder : FirVisitor<Unit, MiniCfgBuilder.MiniCfgData>() {
            override fun visitElement(element: FirElement, data: MiniCfgData) {
                element.acceptChildren(this, data)
            }

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: MiniCfgData) =
                visitFunction(anonymousFunction, data)

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MiniCfgData) =
                visitFunction(simpleFunction, data)

            override fun visitFunction(function: FirFunction, data: MiniCfgData) {
                val freeVariables = data.variableDeclarations.flatMapTo(mutableSetOf()) { it.values }
                val flow = data.flow
                // Detach the function flow so that variables declared inside don't leak into the outside.
                val flowInto = MiniFlow.start()
                data.flow = flowInto
                function.acceptChildren(this, data)
                flowInto.assignedLater.retainAll(freeVariables)
                // Now that the inner variables have been discarded, the rest can be propagated to prevent smartcasts
                // in lambdas declared before this one.
                flow.recordAssignments(flowInto.assignedLater)
                data.flow = flow.fork()
                data.functionForks[function.symbol] = FunctionFork(data.flow.assignedLater, flowInto.assignedLater)
            }

            override fun visitWhenExpression(whenExpression: FirWhenExpression, data: MiniCfgData) {
                (whenExpression.subjectVariable ?: whenExpression.subject)?.accept(this, data)
                val flow = data.flow
                // Also collect `flow` here for the case when none of the branches execute.
                data.flow = whenExpression.branches.mapTo(mutableSetOf(flow)) {
                    // No need to create a fork - it'll not be observed anywhere anyway.
                    data.flow = flow
                    it.accept(this, data)
                    data.flow
                }.join()
            }

            override fun visitTryExpression(tryExpression: FirTryExpression, data: MiniCfgData) {
                tryExpression.tryBlock.accept(this, data)
                val flow = data.flow
                data.flow = tryExpression.catches.mapTo(mutableSetOf(flow)) {
                    data.flow = flow
                    it.accept(this, data)
                    data.flow
                }.join()
                tryExpression.finallyBlock?.accept(this, data)
            }

            private fun Set<MiniFlow>.join(): MiniFlow =
                singleOrNull() ?: MiniFlow(this)

            override fun visitWhileLoop(whileLoop: FirWhileLoop, data: MiniCfgData) {
                // Loop entry is a merge point, so need a new node.
                val start = data.flow.fork()
                data.flow = start
                whileLoop.condition.accept(this, data)
                whileLoop.block.accept(this, data)
                // All forks in the loop should have the same set of variables assigned later, equal to the set
                // at the start of the loop.
                data.flow.recordAssignments(start.assignedLater)
            }

            override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: MiniCfgData) {
                val start = data.flow.fork()
                data.flow = start
                doWhileLoop.block.accept(this, data)
                doWhileLoop.condition.accept(this, data)
                data.flow.recordAssignments(start.assignedLater)
            }

            // TODO: liveness analysis - return/throw/break/continue terminate the flow.
            //   This is somewhat problematic though because try-catch and loops can restore it.
            //   It is not possible to implement liveness analysis partially - otherwise combinations of
            //   control flow structures can cause incorrect smartcasts.

            override fun visitFunctionCall(functionCall: FirFunctionCall, data: MiniCfgData) {
                val visitor = this
                with(functionCall) {
                    setOfNotNull(explicitReceiver, dispatchReceiver, extensionReceiver).forEach { it.accept(visitor, data) }
                    // Delay processing of lambda args because lambda body are evaluated after all arguments have been evaluated.
                    // TODO: this is not entirely correct (the lambda might be nested deep inside an expression), but also this
                    //  entire override should be unnecessary as long as the full CFG builder visits everything in the right order
                    val (postponedFunctionArgs, normalArgs) = argumentList.arguments.partition { it is FirAnonymousFunctionExpression }
                    normalArgs.forEach { it.accept(visitor, data) }
                    postponedFunctionArgs.forEach { it.accept(visitor, data) }
                    calleeReference.accept(visitor, data)
                }
            }

            override fun visitBlock(block: FirBlock, data: MiniCfgData) {
                data.variableDeclarations.addLast(mutableMapOf())
                visitElement(block, data)
                data.variableDeclarations.removeLast()
            }

            override fun visitProperty(property: FirProperty, data: MiniCfgData) {
                visitElement(property, data)
                if (property.isLocal) {
                    data.variableDeclarations.last()[property.name] = property
                }
            }

            override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: MiniCfgData) {
                visitElement(variableAssignment, data)
                if (variableAssignment.explicitReceiver != null) return
                data.recordAssignment(variableAssignment.lValue)
            }

            override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: MiniCfgData) {
                visitElement(assignmentOperatorStatement, data)
                val lhs = assignmentOperatorStatement.leftArgument as? FirQualifiedAccessExpression ?: return
                if (lhs.explicitReceiver != null) return
                data.recordAssignment(lhs.calleeReference)
            }

            private fun MiniCfgData.recordAssignment(reference: FirReference) {
                val name = (reference as? FirNamedReference)?.name ?: return
                val property = variableDeclarations.lastOrNull { name in it }?.get(name) ?: return
                flow.recordAssignments(setOf(property))
            }

            private fun MiniFlow.recordAssignments(properties: Set<FirProperty>) {
                // All assignments already recorded here should also have been recorded in all parents,
                // so if (properties - assignedLater) is empty, no point in continuing.
                if (!assignedLater.addAll(properties)) return
                parents.forEach { it.recordAssignments(properties) }
            }

            class MiniCfgData {
                var flow: MiniFlow = MiniFlow.start()
                val variableDeclarations: ArrayDeque<MutableMap<Name, FirProperty>> = ArrayDeque(listOf(mutableMapOf()))
                val functionForks: MutableMap<FirFunctionSymbol<*>, FunctionFork> = mutableMapOf()
            }
        }
    }
}
