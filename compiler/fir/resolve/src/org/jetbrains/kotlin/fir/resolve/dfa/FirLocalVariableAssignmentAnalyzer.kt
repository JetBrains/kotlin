/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
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
import org.jetbrains.kotlin.utils.addToStdlib.popLast

/**
 *  Helper that checks if an access to a local variable access is stable.
 *
 *  To determine the stability of an access, call [isAccessToUnstableLocalVariable]. Note that the class contains mutable states. So
 *  [isAccessToUnstableLocalVariable] only works for an access during the natural FIR tree traversal. This class will not work if one
 *  queries after the traversal is done.
 **/
internal class FirLocalVariableAssignmentAnalyzer(
    private val assignedLocalVariablesByFunction: Map<FirFunctionSymbol<*>, FunctionFork>
) {
    /**
     * Stack storing concurrent lambda arguments for the current visited anonymous function. For example
     * ```
     * callWithMultipleLambdaExactlyOnceEach(
     *   l1 = { x.length },
     *   l2 = { x = null }
     * )
     * ```
     * From the call, it's nondeterministic whether `l1` runs before `l2` or vice versa. So when handling `l1`, we must mark all variables
     * touched in `l2` unstable.
     */
    private val concurrentLambdaArgsStack: MutableList<MutableSet<FirAnonymousFunction>> = mutableListOf()

    /**
     * Stack whose element tracks all concurrently modified variables in execution paths other than this one. It's a stack because after
     * exiting a local function, we must restore the variables to the state before entering this local function. Initially, there is an
     * empty set for the root function when we starts the analysis.
     */
    private val concurrentlyAssignedLocalVariablesStack: MutableList<MutableSet<FirProperty>> = mutableListOf(mutableSetOf())

    /**
     * Temporary storage that tracks concurrently modified variables during function call resolution. For example, consider the following,
     *
     * ```
     * foo(bar { x= null }, x.length)
     * ```
     *
     * Sometimes during resolution, when stability of `x` is retrieved with [isAccessToUnstableLocalVariable], the resolution of `foo` and
     * `bar` is not yet finished. Hence, the lambda arg passed to `bar` is not traversed. In this case, the resolution logic first calls
     * [visitPostponedAnonymousFunction], then [isAccessToUnstableLocalVariable]. Next, it calls [enterLocalFunction] and starts traversing
     * the lambda passed to `bar`.
     */
    private val ephemeralConcurrentlyAssignedLocalVariables: MutableSet<FirProperty> = mutableSetOf()

    private val functionStack = mutableListOf<FunctionFork>()

    /** Checks whether the given access is an unstable access to a local variable at this moment. */
    fun isAccessToUnstableLocalVariable(qualifiedAccessExpression: FirQualifiedAccessExpression): Boolean {
        val property = qualifiedAccessExpression.referredPropertySymbol?.fir ?: return false
        return property in ephemeralConcurrentlyAssignedLocalVariables || property in concurrentlyAssignedLocalVariablesStack.last()
    }

    fun visitPostponedAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        // Postponed anonymous function is visited before the current function call with lambda is resolved. Hence, the invocationKind is
        // always null and hence there is no need to check it. In addition, since multiple lambda can be passed, we accumulate the
        // effects by appending to `ephemeralConcurrentlyAssignedLocalVariables`.  After the function call is resolved,
        // `exitAnonymousFunction` will be invoked at some point to properly set up the `persistentConcurrentlyAssignedLocalVariables`.
        assignedLocalVariablesByFunction[anonymousFunction.symbol]?.assignedInside?.let {
            ephemeralConcurrentlyAssignedLocalVariables.addAll(it)
        }
    }

    fun finishPostponedAnonymousFunction() {
        // Clear the temporarily assigned local variables in `visitPostponedAnonymousFunction`.
        ephemeralConcurrentlyAssignedLocalVariables.clear()
    }

    fun enterLocalFunction(function: FirFunction) {
        val concurrentlyAssignedLocalVariables = concurrentlyAssignedLocalVariablesStack.last().toMutableSet()
        concurrentlyAssignedLocalVariablesStack.add(concurrentlyAssignedLocalVariables)

        // 1. As mentioned in the comment above, we don't know whether other lambda arguments passed to the same call will be
        //    called before or after this lambda, so their assignments might have executed. Unless they're not called at all.
        // 2. While lambdas from outer calls are not concurrent from control flow point of view, they are concurrent in data flow
        //    because the way this lambda resolves may affect the way those lambdas resolve, thus we need to forbid dependencies
        //    from smartcasts in this lambda to statements in these other lambdas.
        for (concurrentLambdas in concurrentLambdaArgsStack) {
            for (otherLambda in concurrentLambdas) {
                if (otherLambda != function && otherLambda.invocationKind != EventOccurrencesRange.ZERO) {
                    assignedLocalVariablesByFunction[otherLambda.symbol]?.assignedInside?.let {
                        concurrentlyAssignedLocalVariables += it
                    }
                }
            }
        }

        assignedLocalVariablesByFunction[function.symbol]?.let {
            functionStack.add(it)
            if (function !is FirAnonymousFunction || !function.invocationKind.isInPlace) {
                // The function may be called twice concurrently in an SMT environment, which means any assignment it executes
                // might in theory happen in between any check it does and a subsequent use of the variable. So if this function
                // does any assignments, it cannot smartcast the target variables.
                concurrentlyAssignedLocalVariables += it.assignedInside
                // The function may also be stored and called later, so assignments done outside its scope after the definition
                // might also have executed.
                for (outerScope in functionStack) {
                    concurrentlyAssignedLocalVariables += outerScope.assignedLater
                }
            }
        }
    }

    fun exitLocalFunction(function: FirFunction) {
        concurrentlyAssignedLocalVariablesStack.removeLast()
        assignedLocalVariablesByFunction[function.symbol]?.let {
            functionStack.popLast()
            if (function !is FirAnonymousFunction || !function.invocationKind.isInPlace) {
                // The function may be stored and then called later, so any access to the variables it touches
                // is no longer smartcastable ever.
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
                for (outerScope in concurrentlyAssignedLocalVariablesStack) {
                    outerScope += it.assignedInside
                }
            }
        }
    }

    fun enterFunctionCall(lambdaArgs: MutableSet<FirAnonymousFunction>, level: Int) {
        while (concurrentLambdaArgsStack.size < level) {
            // This object is only created on first local anonymous function, so we might have missed some
            // `enterFunctionCall`s. None of them have lambda arguments.
            concurrentLambdaArgsStack.add(mutableSetOf())
        }
        concurrentLambdaArgsStack.add(lambdaArgs)
    }

    fun exitFunctionCall(callCompleted: Boolean) {
        // If we had anonymous functions but no calls with lambdas, the stack might have never been initialized.
        if (concurrentLambdaArgsStack.isEmpty()) return

        val lambdasInCall = concurrentLambdaArgsStack.popLast()
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
            concurrentLambdaArgsStack.lastOrNull()?.addAll(lambdasInCall)
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
        fun analyzeFunction(rootFunction: FirFunction): FirLocalVariableAssignmentAnalyzer {
            val data = MiniCfgBuilder.MiniCfgData()
            MiniCfgBuilder().visitElement(rootFunction, data)
            return FirLocalVariableAssignmentAnalyzer(data.functionForks)
        }

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
