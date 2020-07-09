/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.DirectedGraphCondensationBuilder
import org.jetbrains.kotlin.backend.konan.DirectedGraphMultiNode
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.backend.konan.Context

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

    private class IntraproceduralAnalysis(val context: Context,
                                          val moduleDFG: ModuleDFG, val externalModulesDFG: ExternalModulesDFG,
                                          val callGraph: CallGraph) {

        val functions = moduleDFG.functions// TODO: use for cross-module analysis: + externalModulesDFG.functionDFGs

        private fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
            if (this is DataFlowIR.Type.Declared) return this
            val hash = (this as DataFlowIR.Type.External).hash
            return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $hash")
        }

        fun analyze(): Map<DataFlowIR.FunctionSymbol, FunctionAnalysisResult> {
            val nothing = moduleDFG.symbolTable.mapClassReferenceType(context.ir.symbols.nothing.owner).resolved()
            return callGraph.nodes.associateBy({ it.symbol }) {
                val function = functions[it.symbol]!!
                val body = function.body
                val nodesRoles = mutableMapOf<DataFlowIR.Node, Roles>()
                body.forEachNonScopeNode { nodesRoles[it] = Roles() }

                fun assignRole(node: DataFlowIR.Node, role: Role, infoEntry: RoleInfoEntry?) {
                    nodesRoles[node]!!.add(role, infoEntry)
                }

                body.returns.values.forEach { assignRole(it.node, Role.RETURN_VALUE, null /* TODO */) }
                body.throws.values.forEach  { assignRole(it.node, Role.THROW_VALUE,  null /* TODO */) }
                body.forEachNonScopeNode { node ->
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
                            val type = node.type.resolved()
                            if (type != nothing)
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

                        is DataFlowIR.Node.ArrayWrite -> {
                            assignRole(node.array.node, Role.FIELD_WRITTEN, RoleInfoEntry(node.value.node))
                            assignRole(node.value.node, Role.FIELD_WRITTEN, RoleInfoEntry(node.array.node))
                        }

                        is DataFlowIR.Node.ArrayRead -> {
                            assignRole(node.array.node, Role.FIELD_WRITTEN, RoleInfoEntry(node))
                            assignRole(node, Role.FIELD_WRITTEN, RoleInfoEntry(node.array.node))
                        }

                        is DataFlowIR.Node.Variable -> {
                            for (value in node.values) {
                                assignRole(node, Role.FIELD_WRITTEN, RoleInfoEntry(value.node))
                                assignRole(value.node, Role.FIELD_WRITTEN, RoleInfoEntry(node))
                            }
                        }
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
                                          val externalModulesDFG: ExternalModulesDFG,
                                          val lifetimes: MutableMap<IrElement, Lifetime>) {

        val escapeAnalysisResults = mutableMapOf<DataFlowIR.FunctionSymbol, FunctionEscapeAnalysisResult>()

        fun analyze() {
            DEBUG_OUTPUT(0) {
                println("CALL GRAPH")
                callGraph.directEdges.forEach { t, u ->
                    println("    FUN $t")
                    u.callSites.forEach {
                        val label = when {
                            it.isVirtual -> "VIRTUAL"
                            callGraph.directEdges.containsKey(it.actualCallee) -> "LOCAL"
                            else -> "EXTERNAL"
                        }
                        println("        CALLS $label ${it.actualCallee}")
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
                val numberOfParameters = functionSymbol.parameters.size
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
            multiNode.nodes.forEach {
                val escapeAnalysisResult = escapeAnalysisResults[it]!!
                var escapes = 0
                val pointsTo = escapeAnalysisResult.parameters.withIndex().map { (index, parameterEAResult) ->
                    if (parameterEAResult.escapes)
                        escapes = escapes or (1 shl index)
                    var pointsToMask = 0
                    parameterEAResult.pointsTo.forEach {
                        pointsToMask = pointsToMask or (1 shl it)
                    }
                    pointsToMask
                }.toIntArray()
                it.escapes = escapes
                it.pointsTo = pointsTo
            }
            for (graph in pointsToGraphs.values) {
                for (node in graph.nodes.keys) {
                    val ir = when (node) {
                        is DataFlowIR.Node.Call -> node.irCallSite
                        is DataFlowIR.Node.ArrayRead -> node.irCallSite
                        is DataFlowIR.Node.FieldRead -> node.ir
                        else -> null
                    }
                    ir?.let { lifetimes.put(it, graph.lifetimeOf(node)) }
                }
            }
        }

        private fun analyze(callGraph: CallGraph, pointsToGraph: PointsToGraph, function: DataFlowIR.FunctionSymbol) {
            DEBUG_OUTPUT(0) {
                println("Before calls analysis")
                pointsToGraph.print()
            }

            callGraph.directEdges[function]!!.callSites.forEach {
                val callee = it.actualCallee
                val calleeEAResult = if (it.isVirtual)
                                         getExternalFunctionEAResult(it)
                                     else
                                         callGraph.directEdges[callee]?.let { escapeAnalysisResults[it.symbol]!! }
                                             ?: getExternalFunctionEAResult(it)
                pointsToGraph.processCall(it, calleeEAResult)
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
            val numberOfParameters = symbol.parameters.size
            return FunctionEscapeAnalysisResult((0..numberOfParameters).map {
                ParameterEscapeAnalysisResult(
                        escapes  = true,
                        pointsTo = IntArray(0)
                )
            }.toTypedArray())
        }

        private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        private fun getExternalFunctionEAResult(callSite: CallGraphNode.CallSite): FunctionEscapeAnalysisResult {
            val callee = callSite.actualCallee.resolved()

            val calleeEAResult = if (callSite.isVirtual) {

                DEBUG_OUTPUT(0) { println("A virtual call: $callee") }

                getConservativeFunctionEAResult(callee)
            } else {

                DEBUG_OUTPUT(0) { println("An external call: $callee") }

                FunctionEscapeAnalysisResult.fromBits(
                        callee.escapes ?: 0,
                        (0..callee.parameters.size).map { callee.pointsTo?.elementAtOrNull(it) ?: 0 }
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

            val parametersPointingOnUs = mutableSetOf<Int>()

            fun addIncomingParameter(parameter: Int) {
                if (kind == PointsToGraphNodeKind.ESCAPES) return
                parametersPointingOnUs += parameter
            }
        }

        private inner class PointsToGraph(val functionSymbol: DataFlowIR.FunctionSymbol) {

            val functionAnalysisResult = intraproceduralAnalysisResult[functionSymbol]!!
            val nodes = mutableMapOf<DataFlowIR.Node, PointsToGraphNode>()

            val ids = if (DEBUG > 0)
                (listOf(functionAnalysisResult.function.body.rootScope)
                        + functionAnalysisResult.function.body.allScopes.flatMap { it.nodes }
                        )
                        .withIndex().associateBy({ it.value }, { it.index })
            else null

            fun lifetimeOf(node: DataFlowIR.Node) = nodes[node]!!.let {
                when (it.kind) {
                    PointsToGraphNodeKind.ESCAPES -> Lifetime.GLOBAL

                    PointsToGraphNodeKind.LOCAL -> {
                        if (it.parametersPointingOnUs.isEmpty()) {
                            // A value is neither stored into a global nor into any parameter nor into the return value -
                            // it can be allocated locally.
                            Lifetime.LOCAL
                        } else {
                            if (it.parametersPointingOnUs.size == 1) { // TODO: remove.
                                // A value is stored into a parameter field.
                                Lifetime.PARAMETER_FIELD(it.parametersPointingOnUs.first())
                            } else {
                                // A value is stored into several parameters fields.
                                Lifetime.PARAMETERS_FIELD(it.parametersPointingOnUs.toIntArray(), false)
                            }
                        }
                    }

                    PointsToGraphNodeKind.RETURN_VALUE -> {
                        when {
                            // If a value is explicitly returned.
                            returnValues.contains(node) -> Lifetime.RETURN_VALUE

                            it.parametersPointingOnUs.isNotEmpty() -> Lifetime.PARAMETERS_FIELD(it.parametersPointingOnUs.toIntArray(), true)

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

            fun print_digraph() {
                println("digraph {")
                val ids = ids!!
                nodes.forEach { t, u ->
                    u.edges.forEach {
                        println("    ${ids[t]} -> ${ids[it]};")
                    }
                }
                println("}")
            }

            fun processCall(callSite: CallGraphNode.CallSite, calleeEscapeAnalysisResult: FunctionEscapeAnalysisResult) {
                val call = callSite.call
                DEBUG_OUTPUT(0) {
                    println("Processing callSite")
                    println(nodeToStringWhole(call))
                    println("Actual callee: ${callSite.actualCallee}")
                    println("Callee escape analysis result:")
                    println(calleeEscapeAnalysisResult.toString())
                }

                val arguments = if (call is DataFlowIR.Node.NewObject) {
                                    (0..call.arguments.size).map {
                                        if (it == 0) call else call.arguments[it - 1].node
                                    }
                                } else {
                                    (0..call.arguments.size).map {
                                        if (it < call.arguments.size) call.arguments[it].node else call
                                    }
                                }

                for (index in 0..call.arguments.size) {
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
                // Parameters are declared in the root scope.
                val parameters = functionAnalysisResult.function.body.rootScope.nodes
                        .filterIsInstance<DataFlowIR.Node.Parameter>()
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

    fun computeLifetimes(context: Context, moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG,
                         callGraph: CallGraph, lifetimes: MutableMap<IrElement, Lifetime>) {
        assert(lifetimes.isEmpty())

        val intraproceduralAnalysisResult =
                IntraproceduralAnalysis(context, moduleDFG, externalModulesDFG, callGraph).analyze()
        InterproceduralAnalysis(callGraph, intraproceduralAnalysisResult, externalModulesDFG, lifetimes).analyze()
    }
}