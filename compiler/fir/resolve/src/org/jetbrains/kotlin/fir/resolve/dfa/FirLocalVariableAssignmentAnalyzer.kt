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
    private val assignedLocalVariablesByFunction: Map<FirFunctionSymbol<*>, AssignedLocalVariables>
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
        assignedLocalVariablesByFunction[anonymousFunction.symbol]?.insideLocalFunction?.let {
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
                    assignedLocalVariablesByFunction[otherLambda.symbol]?.insideLocalFunction?.let {
                        concurrentlyAssignedLocalVariables += it
                    }
                }
            }
        }

        when ((function as? FirAnonymousFunction)?.invocationKind) {
            EventOccurrencesRange.AT_LEAST_ONCE,
            EventOccurrencesRange.MORE_THAN_ONCE ->
                // The function may be called repeatedly so the assignments may have already executed before we enter it again.
                assignedLocalVariablesByFunction[function.symbol]?.insideLocalFunction?.let { concurrentlyAssignedLocalVariables += it }
            EventOccurrencesRange.UNKNOWN, null ->
                // The function may not only be called repeatedly, but also stored and called later, so assignments done outside
                // its scope after the definition might also have executed.
                assignedLocalVariablesByFunction[function.symbol]?.all?.let { concurrentlyAssignedLocalVariables += it }
            else -> {} // The function is called at most once so its assignments have not executed yet.
        }
    }

    fun exitLocalFunction(function: FirFunction) {
        concurrentlyAssignedLocalVariablesStack.removeLast()
        when ((function as? FirAnonymousFunction)?.invocationKind) {
            EventOccurrencesRange.UNKNOWN, null ->
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
                assignedLocalVariablesByFunction[function.symbol]?.insideLocalFunction?.let {
                    for (outerScope in concurrentlyAssignedLocalVariablesStack) {
                        outerScope += it
                    }
                }
            else -> {} // The function is only called inline; this is handled by CFG construction by visiting the function body.
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
        private fun computeAssignedLocalVariables(firFunction: FirFunction): Map<FirFunctionSymbol<*>, AssignedLocalVariables> {
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
                data.localFunctionToAssignedLocalVariables[function.symbol] =
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
                super.visitReturnExpression(returnExpression, data)
                // TODO: consider to also handle `throw`, which would require keeping track of all `try`, `catch` and `finally` constructs.
                data.flow = null
            }

            override fun visitFunctionCall(functionCall: FirFunctionCall, data: MiniCfgData) {
                val visitor = this
                with(functionCall) {
                    setOfNotNull(explicitReceiver, dispatchReceiver, extensionReceiver).forEach { it.accept(visitor, data) }
                    // Delay processing of lambda args because lambda body are evaluated after all arguments have been evaluated.
                    val (postponedFunctionArgs, normalArgs) = argumentList.arguments.partition { it is FirAnonymousFunctionExpression }
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
                super.visitProperty(property, data)
                if (property.isLocal) {
                    data.variableDeclarations.last()[property.name] = property
                }
            }

            override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: MiniCfgData) {
                super.visitVariableAssignment(variableAssignment, data)
                val flow = data.flow ?: return
                val name = (variableAssignment.lValue as? FirNamedReference)?.name ?: return
                flow.recordAssignment(name, data)
            }

            override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: MiniCfgData) {
                super.visitAssignmentOperatorStatement(assignmentOperatorStatement, data)
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
                val localFunctionToAssignedLocalVariables: MutableMap<FirFunctionSymbol<*>, AssignedLocalVariables> = mutableMapOf()

                fun resolveLocalVariable(name: Name): FirProperty? {
                    return variableDeclarations.lastOrNull { name in it }?.getValue(name)
                }
            }
        }
    }
}
