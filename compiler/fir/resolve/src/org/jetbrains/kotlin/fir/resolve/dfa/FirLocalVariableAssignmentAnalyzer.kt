/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.referredPropertySymbol
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirLocalVariableAssignmentAnalyzer.Companion.MiniFlow.Companion.join
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

/**
 *  Helper that checks if an access to a local variable access is stable.
 *
 *  To determine the stability of an access, call [isAccessToUnstableLocalVariable]. Note that the class contains mutable states. So
 *  [isAccessToUnstableLocalVariable] only works for an access during the natural FIR tree traversal. This class will not work if one
 *  queries after the traversal is done.
 **/
internal class FirLocalVariableAssignmentAnalyzer(
    private val assignedLocalVariablesByFunction: Map<FirFunction, AssignedLocalVariables>
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
    private val concurrentLambdaArgsStack: MutableList<Set<FirAnonymousFunction>> = mutableListOf()

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
        assignedLocalVariablesByFunction[anonymousFunction]?.insideLocalFunction?.let {
            ephemeralConcurrentlyAssignedLocalVariables.addAll(it)
        }
    }

    fun finishPostponedAnonymousFunction() {
        // Clear the temporarily assigned local variables in `visitPostponedAnonymousFunction`.
        ephemeralConcurrentlyAssignedLocalVariables.clear()
    }

    fun enterLocalFunction(function: FirFunction) {
        val eventOccurrencesRange: EventOccurrencesRange? = (function as? FirAnonymousFunction)?.invocationKind
        // carry on concurrently modified variables from the current scope
        val concurrentlyAssignedLocalVariables = concurrentlyAssignedLocalVariablesStack.last().toMutableSet()
        concurrentlyAssignedLocalVariablesStack.add(concurrentlyAssignedLocalVariables)
        val concurrentLambdasInCurrentCall = concurrentLambdaArgsStack.lastOrNull()
        if (concurrentLambdasInCurrentCall != null && function in concurrentLambdasInCurrentCall) {
            concurrentLambdasInCurrentCall.filter { it != function }.forEach { otherLambda ->
                assignedLocalVariablesByFunction[otherLambda]?.insideLocalFunction?.let {
                    concurrentlyAssignedLocalVariables += it
                }
            }
        }
        when (eventOccurrencesRange) {
            EventOccurrencesRange.AT_LEAST_ONCE,
            EventOccurrencesRange.MORE_THAN_ONCE -> assignedLocalVariablesByFunction[function]?.insideLocalFunction?.let {
                concurrentlyAssignedLocalVariables += it
            }
            // Add both inside and outside since this local function may be invoked multiple times concurrently.
            EventOccurrencesRange.UNKNOWN, null -> assignedLocalVariablesByFunction[function]?.all?.let {
                concurrentlyAssignedLocalVariables += it
            }
            else -> {
                // no additional stuff to do for other cases
            }
        }
    }

    fun exitLocalFunction(function: FirFunction) {
        val eventOccurrencesRange: EventOccurrencesRange? = (function as? FirAnonymousFunction)?.invocationKind
        concurrentlyAssignedLocalVariablesStack.removeLast()
        when (eventOccurrencesRange) {
            EventOccurrencesRange.UNKNOWN, null -> assignedLocalVariablesByFunction[function]?.insideLocalFunction?.let {
                concurrentlyAssignedLocalVariablesStack.last() += it
            }
            else -> {
                // no additional stuff to do for other cases
            }
        }
    }

    fun enterFunctionCallWithMultipleLambdaArgs(lambdaArgs: List<FirAnonymousFunction>) {
        concurrentLambdaArgsStack.add(lambdaArgs.toSet())
    }

    fun exitFunctionCallWithMultipleLambdaArgs() {
        concurrentLambdaArgsStack.removeLast()
    }

    companion object {
        /**
         * Computes assigned local variables in each execution path. This analyzer runs before BODY_RESOLVE. Hence, it works on
         * syntactical information only.
         *
         * # Note on implementation detail
         *
         * The analyzer constructs a mini control flow graph that captures forking of execution path. Any conditional branches, declaration of
         * lambda and local functions are forks. The mini CFG does not care about loop structures and effectively treats it as a linear sequence of
         * statements. This is sufficient for the purpose of collecting unstable local variables. Similarly for try/catch/finally constructs.
         *
         * Also, for simplicity, all conditionals are treated as non-exhaustive. Hence, a fallback edge is always added along a conditional
         * structure.
         *
         * While building the mini CFG, inside each node, we collect local variables that are assigned later in the execution path.
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
         * ┌───────┐
         * │  if   │ {x y z a}
         * └─┬─┬─┬─┘
         *   │ │ │ fallback
         *   │ │ └──────────────────────────────────────┐
         *   │ │ false                                  │
         *   │ └─────────────────────────┐              │
         *   │ true                      │              │
         * ┌─┴─────┐                 ┌───┴────┐         │
         * │  run  │ {x z a}         │  else  │ {x y z} │
         * │       │                 │ branch │         │
         * └─┬───┬─┘                 └───┬────┘         │
         *   │   │ normal execution      │              │
         *   │   └─────────────┐         │              │
         *   │ lambda arg      │         │              │
         * ┌─┴──────┐      ┌───┴───┐     │              │
         * │ lambda │ {x}  │ empty │ {z} │              │
         * │  body  │      │       │     │              │
         * └────────┘      └───┬───┘     │              │
         *   ┌─────────────────┘         │              │
         *   │ ┌─────────────────────────┘              │
         *   │ │ ┌──────────────────────────────────────┘
         * ┌─┴─┴─┴─┐
         * │ after │ {z}
         * │  if   │
         * └───────┘
         *
         * Some notes on why each node contains what it contains:
         *
         * - changes to `z` is captured and back-propagated to all earlier nodes as desired.
         *
         * - "lambda body" node does not contain `a` because `a` is declared inside the function. Such declarations are removed in
         *   [MiniCfgBuilder.handleFunctionFork] after the lambda function is processed. However, the parent nodes contain `a` because
         *   [MiniCfgBuilder.recordAssignment] propagates `a` during traversal. The extra `a` won't do any harm since `a` can never be
         *   referenced outside the lambda. It's possible to track the scope at each node and remove the unneeded `a` in "if" and "run"
         *   nodes. But doing that seems to be more expensive than simply letting it propagate.
         *
         * - "run" node does not contain `y` as desired since the if true and false branches are mutually exclusive.
         *
         * By the way, since local variables are not resolved at this point, we manually track local variable declarations and resolve them along
         * the way so that shadowed names are handled correctly.
         */
        fun analyzeFunction(rootFunction: FirFunction): FirLocalVariableAssignmentAnalyzer {
            return FirLocalVariableAssignmentAnalyzer(computeAssignedLocalVariables(rootFunction))
        }

        /**
         * Computes a mini CFG and returns the map tracking assigned local variables at each potentially concurrent local/lambda function.
         */
        private fun computeAssignedLocalVariables(firFunction: FirFunction): Map<FirFunction, AssignedLocalVariables> {
            val startFlow = MiniFlow.start()
            val data = MiniCfgBuilder.MiniCfgData(startFlow)
            MiniCfgBuilder().visitElement(firFunction, data)
            return data.localFunctionToAssignedLocalVariables
        }

        class AssignedLocalVariables(val outsideLocalFunction: Set<FirProperty>, val insideLocalFunction: Set<FirProperty>) {
            val all get() = outsideLocalFunction + insideLocalFunction
        }

        private class MiniFlow(val parents: Set<MiniFlow>) {
            val assignedLocalVariables: MutableSet<FirProperty> = mutableSetOf()

            fun fork(): MiniFlow = MiniFlow(setOf(this))

            companion object {
                fun start() = MiniFlow(emptySet())
                fun Set<MiniFlow>.join(): MiniFlow = MiniFlow(this)
            }
        }


        private class MiniCfgBuilder : FirVisitor<Unit, MiniCfgBuilder.MiniCfgData>() {
            override fun visitElement(element: FirElement, data: MiniCfgData) {
                element.acceptChildren(this, data)
            }

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: MiniCfgData) {
                handleFunctionFork(anonymousFunction, data)
            }

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MiniCfgData) {
                handleFunctionFork(simpleFunction, data)
            }

            private fun handleFunctionFork(function: FirFunction, data: MiniCfgData) {
                val currentFlow = data.flow ?: return
                val functionFork = currentFlow.fork()
                data.flow = functionFork
                function.acceptChildren(this, data)

                // Only retain local variables declared above the current scope. This way, any local variables declared inside the
                // function will effectively be treated as distinct variables and, hence, stable (Of course, for nested lambda, things would
                // just work because inside the lambda assigned local variables are tracked by different nodes).
                functionFork.assignedLocalVariables.retainAll(data.variableDeclarations.flatMap { it.values })
                // Create another fork for the normal execution
                val normalExecution = currentFlow.fork()
                data.localFunctionToAssignedLocalVariables[function] =
                    AssignedLocalVariables(normalExecution.assignedLocalVariables, functionFork.assignedLocalVariables)
                data.flow = normalExecution
            }

            override fun visitWhenExpression(whenExpression: FirWhenExpression, data: MiniCfgData) {
                val flow = data.flow ?: return
                val visitor = this
                with(whenExpression) {
                    calleeReference.accept(visitor, data)
                    val subjectVariable = this.subjectVariable
                    if (subjectVariable != null) {
                        subjectVariable.accept(visitor, data)
                    } else {
                        subject?.accept(visitor, data)
                    }
                    val childFlows = branches.mapNotNull {
                        data.flow = flow.fork()
                        it.accept(visitor, data)
                        data.flow
                    }
                    // Also collect `flow` here for the synthetic fallback flow when none of the branch executes.
                    data.flow = (childFlows + flow).toSet().join()
                }
            }

            override fun visitReturnExpression(returnExpression: FirReturnExpression, data: MiniCfgData) {
                // TODO: consider to also handle `throw`, which would require keeping track of all `try`, `catch` and `finally` constructs.
                data.flow = null
            }

            @OptIn(ExperimentalStdlibApi::class)
            override fun visitFunctionCall(functionCall: FirFunctionCall, data: MiniCfgData) {
                val visitor = this
                with(functionCall) {
                    setOfNotNull(explicitReceiver, dispatchReceiver, extensionReceiver).forEach { it.accept(visitor, data) }
                    // Delay processing of lambda args because lambda body are evaluated after all arguments have been evaluated.
                    val (postponedFunctionArgs, normalArgs) = argumentList.arguments.partition { it is FirAnonymousFunction }
                    normalArgs.forEach { it.accept(visitor, data) }
                    postponedFunctionArgs.forEach { it.accept(visitor, data) }
                    calleeReference.accept(visitor, data)
                }
            }

            override fun visitBlock(block: FirBlock, data: MiniCfgData) {
                data.variableDeclarations.addLast(mutableMapOf())
                super.visitBlock(block, data)
                data.variableDeclarations.removeLast()
            }

            override fun visitProperty(property: FirProperty, data: MiniCfgData) {
                if (property.isLocal) {
                    data.variableDeclarations.last()[property.name] = property
                }
            }

            override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: MiniCfgData) {
                val flow = data.flow ?: return
                val name = (variableAssignment.lValue as? FirNamedReference)?.name ?: return
                flow.recordAssignment(name, data)
            }

            override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: MiniCfgData) {
                val flow = data.flow ?: return
                val lhs = assignmentOperatorStatement.leftArgument as? FirQualifiedAccessExpression ?: return
                if (lhs.explicitReceiver != null) return
                val name = (lhs.calleeReference as? FirNamedReference)?.name ?: return
                flow.recordAssignment(name, data)
            }

            fun MiniFlow.recordAssignment(name: Name, data: MiniCfgData) {
                val property = data.resolveLocalVariable(name) ?: return
                recordAssignment(property, mutableSetOf())
            }

            private fun MiniFlow.recordAssignment(property: FirProperty, visited: MutableSet<MiniFlow>) {
                if (this in visited) return
                visited += this
                assignedLocalVariables += property
                // Back-propagate the assignment to all parent flows.
                parents.forEach { it.recordAssignment(property, visited) }
            }

            class MiniCfgData(var flow: MiniFlow?) {
                val variableDeclarations: ArrayDeque<MutableMap<Name, FirProperty>> = ArrayDeque(listOf(mutableMapOf()))
                val localFunctionToAssignedLocalVariables: MutableMap<FirFunction, AssignedLocalVariables> = mutableMapOf()
                fun resolveLocalVariable(name: Name): FirProperty? {
                    return variableDeclarations.asReversed().firstNotNullOfOrNull { it[name] }
                }
            }
        }
    }
}
