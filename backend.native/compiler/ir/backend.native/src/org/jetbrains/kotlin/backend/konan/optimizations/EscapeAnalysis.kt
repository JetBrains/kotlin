/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.DirectedGraphCondensationBuilder
import org.jetbrains.kotlin.backend.konan.DirectedGraphMultiNode
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.ir.IrElement

internal object EscapeAnalysis {

    /*
     * The goal of escape analysis is to estimate lifetimes of all expressions in a program.
     * Possible lifetimes are:
     * 1. Local        - an object is used only within a function.
     * 2. Return value - an object is either returned or set to a field of an object being returned.
     * 3. Parameter    - an object is set to a field of exactly one parameter of a function.
     * 4. Global       - otherwise.
     *
     * The analysis is performed in two main steps - intraprocedural and interprocedural.
     * During intraprocedural analysis we remove all control flow related expressions and compute all possible
     * values of all variables within a function.
     * The goal of interprocedural analysis is to build points-to graph (an edge is created from A to B iff A holds
     * a reference to B). This is done by building call graph (using devirtualization for more precise result).
     *
     * How do exactly we build the points-to graph out of the call graph?
     * 1. Build condensation of the call graph.
     * 2. Handle vertices of the resulting DAG in topological order (ensuring that all functions being called
     *    are already handled).
     * 3. For a strongly connected component build the points-to graph iteratively starting with empty graph
     *    (since edges can only be added the process will end eventually).
     *
     * When we have the points-to graph it is easy to compute lifetimes - using DFS compute the graph's closure.
     */

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    // Roles in which particular object reference is being used. Lifetime is computed from all roles reference.
    private enum class Role {
        // If reference is being returned.
        RETURN_VALUE,
        // If reference is being thrown.
        THROW_VALUE,
        // If reference's field is being written to.
        FIELD_WRITTEN,
        // If reference is being written to the global.
        WRITTEN_TO_GLOBAL
    }

    private class RoleInfoEntry(val data: DataFlowIR.Node? = null)

    private open class RoleInfo {
        val entries = mutableListOf<RoleInfoEntry>()

        open fun add(entry: RoleInfoEntry) = entries.add(entry)
    }

    private class Roles {
        val data = HashMap<Role, RoleInfo>()

        fun add(role: Role, info: RoleInfoEntry?) {
            val entry = data.getOrPut(role, { RoleInfo() })
            if (info != null) entry.add(info)
        }

        fun has(role: Role): Boolean = data[role] != null

        fun escapes() = has(Role.WRITTEN_TO_GLOBAL) || has(Role.THROW_VALUE)

        override fun toString() =
                data.keys.joinToString(separator = "; ", prefix = "Roles: ") { it.toString() }
    }

    private class FunctionAnalysisResult(val function: DataFlowIR.Function,
                                         val nodesRoles: Map<DataFlowIR.Node, Roles>)

    private class IntraproceduralAnalysis(val functions: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>,
                                          val callGraph: CallGraph) {

        fun analyze(): Map<DataFlowIR.FunctionSymbol, FunctionAnalysisResult> {
            return callGraph.nodes.associateBy({ it.symbol }) {
                val function = functions[it.symbol]!!
                val body = function.body
                val nodesRoles = body.nodes.associate { it to Roles() }

                fun assignRole(node: DataFlowIR.Node, role: Role, infoEntry: RoleInfoEntry?) {
                    nodesRoles[node]!!.add(role, infoEntry)
                }

                body.returns.values.forEach { assignRole(it.node, Role.RETURN_VALUE, null /* TODO */) }
                body.throws.values.forEach  { assignRole(it.node, Role.THROW_VALUE,  null /* TODO */) }
                for (node in body.nodes) {
                    when (node) {
                        is DataFlowIR.Node.FieldWrite -> {
                            val receiver = node.receiver
                            if (receiver == null) {
                                // Global field.
                                assignRole(node.value.node, Role.WRITTEN_TO_GLOBAL, null /* TODO */)
                            } else {
                                assignRole(receiver.node, Role.FIELD_WRITTEN, RoleInfoEntry(node.value.node))

                                // TODO: make more precise analysis and differentiate fields from receivers.
                                // See test escape2.kt, why we need these edges.
                                assignRole(node.value.node, Role.FIELD_WRITTEN, RoleInfoEntry(receiver.node))
                            }
                        }

                        is DataFlowIR.Node.Singleton -> {
                            assignRole(node, Role.WRITTEN_TO_GLOBAL, null /* TODO */)
                        }

                        is DataFlowIR.Node.FieldRead -> {
                            val receiver = node.receiver
                            if (receiver == null) {
                                // Global field.
                                assignRole(node, Role.WRITTEN_TO_GLOBAL, null /* TODO */)
                            } else {
                                // Receiver holds reference to all its fields.
                                assignRole(receiver.node, Role.FIELD_WRITTEN, RoleInfoEntry(node))

                                /*
                                 * The opposite (a field points to its receiver) is also kind of true.
                                 * Here is an example why we need these edges:
                                 *
                                 * class B
                                 * class A { val b = B() }
                                 * fun foo(): B {
                                 *     val a = A() <- here [a] is created and so does [a.b], therefore they have the same lifetime.
                                 *     return a.b  <- a.b escapes to return value. If there were no edge from [a.b] to [a],
                                 *                    then [a] would've been considered local and since [a.b] has the same lifetime as [a],
                                 *                    [a.b] would be local as well.
                                 * }
                                 *
                                 */
                                assignRole(node, Role.FIELD_WRITTEN, RoleInfoEntry(receiver.node))
                            }
                        }

                        is DataFlowIR.Node.Variable -> {
                            for (value in node.values) {
                                assignRole(node, Role.FIELD_WRITTEN, RoleInfoEntry(value.node))
                                assignRole(value.node, Role.FIELD_WRITTEN, RoleInfoEntry(node))
                            }
                        }
                        else -> TODO()
                    }
                }
                FunctionAnalysisResult(function, nodesRoles)
            }
        }
    }

    private class ParameterEscapeAnalysisResult(val escapes: Boolean, val pointsTo: IntArray) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ParameterEscapeAnalysisResult) return false

            if (escapes != other.escapes) return false
            if (pointsTo.size != other.pointsTo.size) return false
            return pointsTo.indices.all { pointsTo[it] == other.pointsTo[it] }
        }

        override fun hashCode(): Int {
            var result = escapes.hashCode()
            pointsTo.forEach { result = 31 * result + it.hashCode() }
            return result
        }

        override fun toString() = "${if (escapes) "ESCAPES" else "LOCAL"}, POINTS TO: ${pointsTo.contentToString()}"
    }

    private class FunctionEscapeAnalysisResult(val parameters: Array<ParameterEscapeAnalysisResult>) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FunctionEscapeAnalysisResult) return false

            if (parameters.size != other.parameters.size) return false
            return parameters.indices.all { parameters[it] == other.parameters[it] }
        }

        override fun hashCode(): Int {
            var result = 0
            parameters.forEach { result = 31 * result + it.hashCode() }
            return result
        }

        override fun toString(): String {
            return parameters.withIndex().joinToString("\n") {
                if (it.index < parameters.size - 1)
                    "PARAM#${it.index}: ${it.value}"
                else "RETURN: ${it.value}"
            }
        }

        val isTrivial get() = parameters.all { !it.escapes && it.pointsTo.isEmpty() }

        companion object {
            fun fromBits(escapesMask: Int, pointsToMasks: List<Int>) = FunctionEscapeAnalysisResult(
                    pointsToMasks.indices.map { parameterIndex ->
                        val escapes = escapesMask and (1 shl parameterIndex) != 0
                        val curPointsToMask = pointsToMasks[parameterIndex]
                        val pointsTo = (0..31).filter { curPointsToMask and (1 shl it) != 0 }.toIntArray()
                        ParameterEscapeAnalysisResult(escapes, pointsTo)
                    }.toTypedArray()
            )
        }
    }

    private class InterproceduralAnalysis(val callGraph: CallGraph,
                                          val intraproceduralAnalysisResult: Map<DataFlowIR.FunctionSymbol, FunctionAnalysisResult>,
                                          val lifetimes: MutableMap<IrElement, Lifetime>) {

        val escapeAnalysisResults = mutableMapOf<DataFlowIR.FunctionSymbol, FunctionEscapeAnalysisResult>()

        fun analyze() {
            DEBUG_OUTPUT(0) {
                println("CALL GRAPH")
                callGraph.directEdges.forEach { t, u ->
                    println("    FUN $t")
                    u.callSites.forEach {
                        val local = callGraph.directEdges.containsKey(it.actualCallee)
                        println("        CALLS ${if (local) "LOCAL" else "EXTERNAL"} ${it.actualCallee}")
                    }
                    callGraph.reversedEdges[t]!!.forEach {
                        println("        CALLED BY $it")
                    }
                }
            }

            val condensation = DirectedGraphCondensationBuilder(callGraph).build()

            DEBUG_OUTPUT(0) {
                println("CONDENSATION")
                condensation.topologicalOrder.forEach { multiNode ->
                    println("    MULTI-NODE")
                    multiNode.nodes.forEach {
                        println("        $it")
                        callGraph.directEdges[it]!!.callSites
                                .filter { callGraph.directEdges.containsKey(it.actualCallee) }
                                .forEach { println("            CALLS ${it.actualCallee}") }
                        callGraph.reversedEdges[it]!!.forEach {
                            println("            CALLED BY $it")
                        }
                    }
                }
            }

            for (functionSymbol in callGraph.directEdges.keys) {
                val numberOfParameters = functionSymbol.parameterTypes.size
                escapeAnalysisResults[functionSymbol] = FunctionEscapeAnalysisResult(
                        // Assume no edges at the beginning.
                        // Then iteratively add needed.
                        Array(numberOfParameters + 1) { ParameterEscapeAnalysisResult(false, IntArray(0)) }
                )
            }

            for (multiNode in condensation.topologicalOrder.reversed())
                analyze(callGraph, multiNode)
        }

        private fun analyze(callGraph: CallGraph, multiNode: DirectedGraphMultiNode<DataFlowIR.FunctionSymbol>) {
            DEBUG_OUTPUT(0) {
                println("Analyzing multiNode:\n    ${multiNode.nodes.joinToString("\n   ") { it.toString() }}")
                multiNode.nodes.forEach { from ->
                    println("DataFlowIR")
                    intraproceduralAnalysisResult[from]!!.function.debugOutput()
                    callGraph.directEdges[from]!!.callSites.forEach { to ->
                        println("CALL")
                        println("   from $from")
                        println("   to ${to.actualCallee}")
                    }
                }
            }

            val pointsToGraphs = multiNode.nodes.associateBy({ it }, { PointsToGraph(it) })
            val toAnalyze = mutableSetOf<DataFlowIR.FunctionSymbol>()
            toAnalyze.addAll(multiNode.nodes)
            while (toAnalyze.isNotEmpty()) {
                val function = toAnalyze.first()
                toAnalyze.remove(function)

                DEBUG_OUTPUT(0) { println("Processing function $function") }

                val startResult = escapeAnalysisResults[callGraph.directEdges[function]!!.symbol]!!

                DEBUG_OUTPUT(0) { println("Start escape analysis result:\n$startResult") }

                analyze(callGraph, pointsToGraphs[function]!!, function)
                val endResult = escapeAnalysisResults[callGraph.directEdges[function]!!.symbol]!!
                if (startResult == endResult) {
                    DEBUG_OUTPUT(0) { println("Escape analysis is not changed") }
                } else {
                    DEBUG_OUTPUT(0) { println("Escape analysis was refined:\n$endResult") }

                    callGraph.reversedEdges[function]?.forEach {
                        if (multiNode.nodes.contains(it))
                            toAnalyze.add(it)
                    }
                }
            }
            for (graph in pointsToGraphs.values) {
                graph.nodes.keys
                        .filterIsInstance<DataFlowIR.Node.Call>()
                        .forEach { call -> call.irCallSite?.let { lifetimes.put(it, graph.lifetimeOf(call)) } }
            }
        }

        private fun analyze(callGraph: CallGraph, pointsToGraph: PointsToGraph, function: DataFlowIR.FunctionSymbol) {
            DEBUG_OUTPUT(0) {
                println("Before calls analysis")
                pointsToGraph.print()
            }

            callGraph.directEdges[function]!!.callSites.forEach {
                val callee = it.actualCallee
                val calleeEAResult = callGraph.directEdges[callee]?.let { escapeAnalysisResults[it.symbol]!! }
                        ?: getExternalFunctionEAResult(it)
                pointsToGraph.processCall(it.call, calleeEAResult)
            }

            DEBUG_OUTPUT(0) {
                println("After calls analysis")
                pointsToGraph.print()
            }

            // Build transitive closure.
            val eaResult = pointsToGraph.buildClosure()

            DEBUG_OUTPUT(0) {
                println("After closure building")
                pointsToGraph.print()
            }

            escapeAnalysisResults[callGraph.directEdges[function]!!.symbol] = eaResult
        }

        private fun getConservativeFunctionEAResult(symbol: DataFlowIR.FunctionSymbol): FunctionEscapeAnalysisResult {
            val numberOfParameters = symbol.parameterTypes.size
            return FunctionEscapeAnalysisResult((0..numberOfParameters).map {
                ParameterEscapeAnalysisResult(
                        escapes = true,
                        pointsTo = IntArray(0)
                )
            }.toTypedArray())
        }

        private fun getExternalFunctionEAResult(callSite: CallGraphNode.CallSite): FunctionEscapeAnalysisResult {
            val callee = callSite.actualCallee

            val calleeEAResult = if (callSite.call is DataFlowIR.Node.VirtualCall) {

                DEBUG_OUTPUT(0) { println("A virtual call: $callee") }

                getConservativeFunctionEAResult(callee)
            } else {
                callSite.call as DataFlowIR.Node.StaticCall
                callee as DataFlowIR.FunctionSymbol.External

                FunctionEscapeAnalysisResult.fromBits(
                        callee.escapes ?: 0,
                        (0..callee.parameterTypes.size).map { callee.pointsTo?.elementAtOrNull(it) ?: 0 }
                )
            }

            DEBUG_OUTPUT(0) {
                println("Escape analysis result")
                println(calleeEAResult.toString())
                println()
            }

            return calleeEAResult
        }

        private enum class PointsToGraphNodeKind(val weight: Int) {
            LOCAL(0),
            RETURN_VALUE(1),
            ESCAPES(2)
        }

        private class PointsToGraphNode(roles: Roles) {
            val edges = mutableSetOf<DataFlowIR.Node>()

            var kind = when {
                roles.escapes() -> PointsToGraphNodeKind.ESCAPES
                roles.has(Role.RETURN_VALUE) -> PointsToGraphNodeKind.RETURN_VALUE
                else -> PointsToGraphNodeKind.LOCAL
            }

            val beingReturned = roles.has(Role.RETURN_VALUE)

            var parameterPointingOnUs: Int? = null
            var pointsMoreThanOneParameter = false

            fun addIncomingParameter(parameter: Int) {
                if (pointsMoreThanOneParameter) return
                if (parameterPointingOnUs == null)
                    parameterPointingOnUs = parameter
                else pointsMoreThanOneParameter = true
            }
        }

        private inner class PointsToGraph(val functionSymbol: DataFlowIR.FunctionSymbol) {

            val functionAnalysisResult = intraproceduralAnalysisResult[functionSymbol]!!
            val nodes = mutableMapOf<DataFlowIR.Node, PointsToGraphNode>()

            val ids = if (DEBUG > 0) functionAnalysisResult.function.body.nodes.withIndex().associateBy({ it.value }, { it.index }) else null

            fun lifetimeOf(node: DataFlowIR.Node) = nodes[node]!!.let {
                when (it.kind) {
                    PointsToGraphNodeKind.ESCAPES -> Lifetime.GLOBAL

                    PointsToGraphNodeKind.LOCAL -> {
                        if (it.pointsMoreThanOneParameter)
                            Lifetime.GLOBAL
                        else {
                            val parameterPointingOnUs = it.parameterPointingOnUs
                            if (parameterPointingOnUs != null)
                                // A value is stored into a parameter field.
                                Lifetime.PARAMETER_FIELD(parameterPointingOnUs)
                            else
                                // A value is neither stored into a global nor into any parameter nor into the return value -
                                // it can be allocated locally.
                                Lifetime.LOCAL
                        }
                    }

                    PointsToGraphNodeKind.RETURN_VALUE -> {
                        when {
                            it.parameterPointingOnUs != null -> Lifetime.GLOBAL
                            // If a value is explicitly returned.
                            returnValues.contains(node) -> Lifetime.RETURN_VALUE
                            // A value is stored into a field of the return value.
                            else -> Lifetime.INDIRECT_RETURN_VALUE
                        }
                    }
                }
            }

            init {
                DEBUG_OUTPUT(0) {
                    println("Building points-to graph for function $functionSymbol")
                    println("Results of preliminary function analysis")
                }

                functionAnalysisResult.nodesRoles.forEach { node, roles ->
                    DEBUG_OUTPUT(0) { println("NODE ${nodeToString(node)}: $roles") }

                    nodes.put(node, PointsToGraphNode(roles))
                }

                functionAnalysisResult.nodesRoles.forEach { node, roles ->
                    addEdges(node, roles)
                }
            }

            private val returnValues = nodes.filter { it.value.beingReturned }
                                            .map { it.key }
                                            .toSet()

            private fun addEdges(from: DataFlowIR.Node, roles: Roles) {
                val pointsToEdge = roles.data[Role.FIELD_WRITTEN]
                        ?: return
                pointsToEdge.entries.forEach {
                    val to = it.data!!
                    if (nodes.containsKey(to)) {
                        nodes[from]!!.edges.add(to)

                        DEBUG_OUTPUT(0) {
                            println("EDGE: ")
                            println("    FROM: ${nodeToString(from)}")
                            println("    TO: ${nodeToString(to)}")
                        }
                    }
                }
            }

            private fun nodeToStringWhole(node: DataFlowIR.Node) = DataFlowIR.Function.nodeToString(node, ids!!)

            private fun nodeToString(node: DataFlowIR.Node) = ids!![node]

            fun print() {
                println("POINTS-TO GRAPH")
                println("NODES")
                nodes.forEach { t, _ ->
                    println("    ${lifetimeOf(t)} ${nodeToString(t)}")
                    print(nodeToStringWhole(t))
                }
                println("EDGES")
                nodes.forEach { t, u ->
                    u.edges.forEach {
                        println("    FROM ${nodeToString(t)}")
                        println("    TO ${nodeToString(it)}")
                    }
                }
            }

            fun processCall(callSite: DataFlowIR.Node.Call, calleeEscapeAnalysisResult: FunctionEscapeAnalysisResult) {
                DEBUG_OUTPUT(0) {
                    println("Processing callSite")
                    println(nodeToStringWhole(callSite))
                    println("Callee escape analysis result:")
                    println(calleeEscapeAnalysisResult.toString())
                }

                val arguments = if (callSite is DataFlowIR.Node.NewObject) {
                                    (0..callSite.arguments.size).map {
                                        if (it == 0) callSite else callSite.arguments[it - 1].node
                                    }
                                } else {
                                    (0..callSite.arguments.size).map {
                                        if (it < callSite.arguments.size) callSite.arguments[it].node else callSite
                                    }
                                }

                for (index in 0..callSite.arguments.size) {
                    val parameterEAResult = calleeEscapeAnalysisResult.parameters[index]
                    val from = arguments[index]
                    if (parameterEAResult.escapes) {
                        nodes[from]!!.kind = PointsToGraphNodeKind.ESCAPES

                        DEBUG_OUTPUT(0) { println("Node ${nodeToString(from)} escapes") }
                    }
                    parameterEAResult.pointsTo.forEach { toIndex ->
                        val nodeFrom = nodes[from]
                        if (nodeFrom == null) {
                            DEBUG_OUTPUT(0) {
                                println("WARNING: There is no node")
                                println("    FROM ${nodeToString(from)}")
                            }
                        } else {
                            val to = arguments[toIndex]
                            val nodeTo = nodes[to]
                            if (nodeTo == null) {
                                DEBUG_OUTPUT(0) {
                                    println("WARNING: There is no node")
                                    println("    TO ${nodeToString(to)}")
                                }
                            } else {
                                DEBUG_OUTPUT(0) {
                                    println("Adding edge")
                                    println("    FROM ${nodeToString(from)}")
                                    println("    TO ${nodeToString(to)}")
                                }

                                nodeFrom.edges.add(to)
                            }
                        }
                    }
                }
            }

            fun buildClosure(): FunctionEscapeAnalysisResult {
                val parameters = functionAnalysisResult.function.body.nodes.filterIsInstance<DataFlowIR.Node.Parameter>()
                val reachabilities = mutableListOf<IntArray>()

                DEBUG_OUTPUT(0) {
                    println("BUILDING CLOSURE")
                    println("Return values:")
                    returnValues.forEach {
                        println("    ${nodeToString(it)}")
                    }
                }

                parameters.forEach {
                    val visited = mutableSetOf<DataFlowIR.Node>()
                    if (nodes[it] != null)
                        findReachable(it, visited)
                    visited -= it

                    DEBUG_OUTPUT(0) {
                        println("Reachable from ${nodeToString(it)}")
                        visited.forEach {
                            println("    ${nodeToString(it)}")
                        }
                    }

                    val reachable = mutableListOf<Int>()
                    parameters.forEach {
                        if (visited.contains(it))
                            reachable += it.index
                    }
                    if (returnValues.any { visited.contains(it) })
                        reachable += parameters.size
                    reachabilities.add(reachable.toIntArray())
                    visited.forEach { node ->
                        if (node !is DataFlowIR.Node.Parameter)
                            nodes[node]!!.addIncomingParameter(it.index)
                    }
                }
                val visitedFromReturnValues = mutableSetOf<DataFlowIR.Node>()
                returnValues.forEach {
                    if (!visitedFromReturnValues.contains(it)) {
                        findReachable(it, visitedFromReturnValues)
                    }
                }
                reachabilities.add(
                        parameters.filter { visitedFromReturnValues.contains(it) }
                                .map { it.index }.toIntArray()
                )

                propagate(PointsToGraphNodeKind.ESCAPES)
                propagate(PointsToGraphNodeKind.RETURN_VALUE)

                return FunctionEscapeAnalysisResult(reachabilities.withIndex().map { (index, reachability) ->
                    val escapes =
                            if (index == parameters.size) // Return value.
                                returnValues.any { nodes[it]!!.kind == PointsToGraphNodeKind.ESCAPES }
                            else {
                                /*runtimeAware.isInteresting(parameters[index].value.type) TODO: is it really needed?
                                        && */nodes[parameters[index]]!!.kind == PointsToGraphNodeKind.ESCAPES
                            }
                    ParameterEscapeAnalysisResult(escapes, reachability)
                }.toTypedArray())
            }

            private fun findReachable(node: DataFlowIR.Node, visited: MutableSet<DataFlowIR.Node>) {
                visited += node
                nodes[node]!!.edges.forEach {
                    if (!visited.contains(it)) {
                        findReachable(it, visited)
                    }
                }
            }

            private fun propagate(kind: PointsToGraphNodeKind) {
                val visited = mutableSetOf<DataFlowIR.Node>()
                nodes.filter { it.value.kind == kind }
                        .forEach { node, _ -> propagate(node, kind, visited) }
            }

            private fun propagate(node: DataFlowIR.Node, kind: PointsToGraphNodeKind, visited: MutableSet<DataFlowIR.Node>) {
                if (visited.contains(node)) return
                visited.add(node)
                val nodeInfo = nodes[node]!!
                if (nodeInfo.kind.weight < kind.weight)
                    nodeInfo.kind = kind
                nodeInfo.edges.forEach { propagate(it, kind, visited) }
            }
        }
    }

    fun computeLifetimes(moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG,
                         callGraph: CallGraph, lifetimes: MutableMap<IrElement, Lifetime>) {
        assert(lifetimes.isEmpty())

        val intraproceduralAnalysisResult =
                IntraproceduralAnalysis(moduleDFG.functions + externalModulesDFG.functionDFGs, callGraph).analyze()
        InterproceduralAnalysis(callGraph, intraproceduralAnalysisResult, lifetimes).analyze()
    }
}