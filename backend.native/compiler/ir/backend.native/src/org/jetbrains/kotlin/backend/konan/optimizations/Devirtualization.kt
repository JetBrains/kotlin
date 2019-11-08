/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import java.util.*
import kotlin.collections.ArrayList

// Devirtualization analysis is performed using Variable Type Analysis algorithm.
// See http://web.cs.ucla.edu/~palsberg/tba/papers/sundaresan-et-al-oopsla00.pdf for details.
internal object Devirtualization {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private val TAKE_NAMES = false // Take fqNames for all functions and types (for debug purposes).

    private inline fun takeName(block: () -> String) = if (TAKE_NAMES) block() else null

    fun computeRootSet(context: Context, moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG)
            : List<DataFlowIR.FunctionSymbol> {

        fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        val entryPoint = context.ir.symbols.entryPoint?.owner
        val exportedFunctions =
                if (entryPoint != null)
                    listOf(moduleDFG.symbolTable.mapFunction(entryPoint).resolved())
                else
                // In a library every public function and every function accessible via virtual call belongs to the rootset.
                    moduleDFG.symbolTable.functionMap.values.filterIsInstance<DataFlowIR.FunctionSymbol.Public>() +
                    moduleDFG.symbolTable.classMap.values
                            .filterIsInstance<DataFlowIR.Type.Declared>()
                            .flatMap { it.vtable + it.itable.values }
                            .filterIsInstance<DataFlowIR.FunctionSymbol.Declared>()
                            .filter { moduleDFG.functions.containsKey(it) }
        // TODO: Are globals initializers always called whether they are actually reachable from roots or not?
        val globalInitializers =
                moduleDFG.symbolTable.functionMap.values.filter { it.isGlobalInitializer } +
                externalModulesDFG.functionDFGs.keys.filter { it.isGlobalInitializer }

        val explicitlyExportedFunctions =
                moduleDFG.symbolTable.functionMap.values.filter { it.explicitlyExported } +
                externalModulesDFG.functionDFGs.keys.filter { it.explicitlyExported }

        return (exportedFunctions + globalInitializers + explicitlyExportedFunctions).distinct()
    }

    fun BitSet.format(allTypes: List<DataFlowIR.Type.Declared>): String {
        return allTypes.withIndex().filter { this[it.index] }.joinToString { it.value.toString() }
    }

    private val VIRTUAL_TYPE_ID = 0 // Id of [DataFlowIR.Type.Virtual].

    internal class DevirtualizationAnalysis(val context: Context,
                                            val moduleDFG: ModuleDFG,
                                            val externalModulesDFG: ExternalModulesDFG) {

        private val entryPoint = context.ir.symbols.entryPoint?.owner

        private val symbolTable = moduleDFG.symbolTable

        sealed class Node(val id: Int) : DirectedGraphNode<Node> {
            override var directEdges: MutableList<Node>? = null
            override var reversedEdges : MutableList<Node>? = null
            override val key get() = this

            var directCastEdges: MutableList<CastEdge>? = null
            var reversedCastEdges: MutableList<CastEdge>? = null

            val types = BitSet()

            var priority = -1

            var multiNodeStart = -1
            var multiNodeEnd = -1

            val multiNodeSize get() = multiNodeEnd - multiNodeStart

            fun addEdge(node: Node) {
                if (directEdges == null) directEdges = ArrayList(1)
                directEdges!!.add(node)
                if (node.reversedEdges == null) node.reversedEdges = ArrayList(1)
                node.reversedEdges!!.add(this)
            }

            fun addCastEdge(edge: CastEdge) {
                if (directCastEdges == null) directCastEdges = ArrayList(1)
                directCastEdges!!.add(edge)
                if (edge.node.reversedCastEdges == null) edge.node.reversedCastEdges = ArrayList(1)
                edge.node.reversedCastEdges!!.add(CastEdge(this, edge.suitableTypes))
            }

            abstract fun toString(allTypes: List<DataFlowIR.Type.Declared>): String

            class Source(id: Int, typeId: Int, nameBuilder: () -> String): Node(id) {
                val name = takeName(nameBuilder)

                init {
                    types.set(typeId)
                }

                override fun toString(allTypes: List<DataFlowIR.Type.Declared>): String {
                    return "Source(name='$name', types='${types.format(allTypes)}')"
                }
            }

            class Ordinary(id: Int, nameBuilder: () -> String) : Node(id) {
                val name = takeName(nameBuilder)

                override fun toString(allTypes: List<DataFlowIR.Type.Declared>): String {
                    return "Ordinary(name='$name', types='${types.format(allTypes)}')"
                }
            }

            class CastEdge(val node: Node, val suitableTypes: BitSet)
        }

        class Function(val symbol: DataFlowIR.FunctionSymbol, val parameters: Array<Node>, val returns: Node, val throws: Node)

        class VirtualCallSiteReceivers(val receiver: Node, val caller: DataFlowIR.FunctionSymbol,
                                       val devirtualizedCallees: List<DevirtualizedCallee>)

        inner class ConstraintGraph : DirectedGraph<Node, Node> {

            private var nodesCount = 0

            override val nodes = mutableListOf<Node>()
            override fun get(key: Node) = key

            val voidNode = addNode { Node.Ordinary(it, { "Void" }) }
            val virtualNode = addNode { Node.Source(it, VIRTUAL_TYPE_ID, { "Virtual" }) }
            val arrayItemField = DataFlowIR.Field(null,
                    symbolTable.mapClassReferenceType(context.irBuiltIns.anyClass.owner), 1, "Array\$Item")
            val functions = mutableMapOf<DataFlowIR.FunctionSymbol, Function>()
            val concreteClasses = mutableMapOf<DataFlowIR.Type.Declared, Node>()
            val externalFunctions = mutableMapOf<DataFlowIR.FunctionSymbol, Node>()
            val fields = mutableMapOf<DataFlowIR.Field, Node>() // Do not distinguish receivers.
            val virtualCallSiteReceivers = mutableMapOf<DataFlowIR.Node.VirtualCall, VirtualCallSiteReceivers>()

            private fun nextId(): Int = nodesCount++

            fun addNode(nodeBuilder: (Int) -> Node) = nodeBuilder(nextId()).also { nodes.add(it) }
        }

        private val constraintGraph = ConstraintGraph()

        private fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
            if (this is DataFlowIR.Type.Declared) return this
            val hash = (this as DataFlowIR.Type.External).hash
            return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $this")
        }

        private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        inner class TypeHierarchy(types: List<DataFlowIR.Type.Declared>) {
            private val typesSubTypes = mutableMapOf<DataFlowIR.Type.Declared, MutableList<DataFlowIR.Type.Declared>>()

            init {
                val visited = mutableSetOf<DataFlowIR.Type.Declared>()

                fun processType(type: DataFlowIR.Type.Declared) {
                    if (type == DataFlowIR.Type.Virtual) return
                    if (!visited.add(type)) return
                    type.superTypes
                            .map { it.resolved() }
                            .forEach { superType ->
                                val subTypes = typesSubTypes.getOrPut(superType) { mutableListOf() }
                                subTypes += type
                                processType(superType)
                            }
                }

                types.forEach { processType(it) }
            }

            private fun findAllInheritors(type: DataFlowIR.Type.Declared, result: MutableSet<DataFlowIR.Type.Declared>) {
                if (!result.add(type)) return
                typesSubTypes[type]?.forEach { findAllInheritors(it, result) }
            }

            fun inheritorsOf(type: DataFlowIR.Type.Declared): List<DataFlowIR.Type.Declared> {
                val result = mutableSetOf<DataFlowIR.Type.Declared>()
                findAllInheritors(type, result)
                return result.toList()
            }
        }

        private inner class InstantiationsSearcher(val rootSet: List<DataFlowIR.FunctionSymbol>,
                                                   val typeHierarchy: TypeHierarchy) {
            private val visited = mutableSetOf<DataFlowIR.FunctionSymbol>()
            private val typesVirtualCallSites = mutableMapOf<DataFlowIR.Type.Declared, MutableList<DataFlowIR.Node.VirtualCall>>()
            private val instantiatingClasses = mutableSetOf<DataFlowIR.Type.Declared>()

            fun search(): Set<DataFlowIR.Type.Declared> {
                // Rapid Type Analysis: find all instantiations and conservatively estimate call graph.

                // Add all final parameters of the roots.
                rootSet.forEach {
                    it.parameters
                            .map { it.type.resolved() }
                            .filter { it.isFinal }
                            .forEach { addInstantiatingClass(it) }
                }
                if (entryPoint == null) {
                    // For library assume all public non-abstract classes could be instantiated.
                    symbolTable.classMap.values
                            .filterIsInstance<DataFlowIR.Type.Public>()
                            .filter { !it.isAbstract }
                            .forEach { addInstantiatingClass(it) }
                } else {
                    // String is implicitly created as argument of <main>.
                    addInstantiatingClass(symbolTable.mapType(context.irBuiltIns.stringType))
                }
                // Traverse call graph from the roots.
                rootSet.forEach { dfs(it, it.returnParameter.type) }
                return instantiatingClasses
            }

            private fun addInstantiatingClass(type: DataFlowIR.Type) {
                val resolvedType = type.resolved()
                if (!instantiatingClasses.add(resolvedType)) return

                DEBUG_OUTPUT(1) { println("Adding instantiating class: $resolvedType") }

                checkSupertypes(resolvedType, resolvedType, mutableSetOf())
            }

            private fun processVirtualCall(virtualCall: DataFlowIR.Node.VirtualCall,
                                           receiverType: DataFlowIR.Type.Declared) {
                DEBUG_OUTPUT(1) {
                    println("Processing virtual call: ${virtualCall.callee}")
                    println("Receiver type: $receiverType")
                }

                val callee = when (virtualCall) {
                    is DataFlowIR.Node.VtableCall ->
                        receiverType.vtable[virtualCall.calleeVtableIndex]

                    is DataFlowIR.Node.ItableCall ->
                        receiverType.itable[virtualCall.calleeHash]!!

                    else -> error("Unreachable")
                }
                dfs(callee, virtualCall.returnType)
            }

            private fun checkSupertypes(type: DataFlowIR.Type.Declared,
                                        inheritor: DataFlowIR.Type.Declared,
                                        seenTypes: MutableSet<DataFlowIR.Type.Declared>) {
                seenTypes += type

                DEBUG_OUTPUT(1) {
                    println("Checking supertype $type of $inheritor")
                    typesVirtualCallSites[type].let {
                        if (it == null)
                            println("None virtual call sites encountered yet")
                        else {
                            println("Virtual call sites:")
                            it.forEach {
                                println("    ${it.callee}")
                            }
                        }
                    }
                }

                typesVirtualCallSites[type]?.let { virtualCallSites ->
                    var index = 0
                    while (index < virtualCallSites.size) {
                        processVirtualCall(virtualCallSites[index], inheritor)
                        ++index
                    }
                }
                type.superTypes
                        .map { it.resolved() }
                        .filterNot { seenTypes.contains(it) }
                        .forEach { checkSupertypes(it, inheritor, seenTypes) }
            }

            private fun dfs(symbol: DataFlowIR.FunctionSymbol, returnType: DataFlowIR.Type) {
                val resolvedFunctionSymbol = symbol.resolved()
                if (resolvedFunctionSymbol is DataFlowIR.FunctionSymbol.External) {

                    DEBUG_OUTPUT(1) { println("Function $resolvedFunctionSymbol is external") }

                    val resolvedReturnType = returnType.resolved()
                    if (resolvedReturnType.isFinal) {

                        DEBUG_OUTPUT(1) { println("Adding return type as it is final") }

                        addInstantiatingClass(resolvedReturnType)
                    }
                    return
                }
                if (!visited.add(resolvedFunctionSymbol)) return

                DEBUG_OUTPUT(1) { println("Visiting $resolvedFunctionSymbol") }

                val function = (moduleDFG.functions[resolvedFunctionSymbol]
                        ?: externalModulesDFG.functionDFGs[resolvedFunctionSymbol])
                        ?: return

                DEBUG_OUTPUT(1) { function.debugOutput() }

                nodeLoop@for (node in function.body.nodes) {
                    when (node) {
                        is DataFlowIR.Node.NewObject -> {
                            addInstantiatingClass(node.constructedType)
                            dfs(node.callee, node.constructedType)
                        }

                        is DataFlowIR.Node.Singleton -> {
                            addInstantiatingClass(node.type)
                            node.constructor?.let { dfs(it, node.type) }
                        }

                        is DataFlowIR.Node.AllocInstance -> {
                            addInstantiatingClass(node.type)
                        }

                        is DataFlowIR.Node.Const -> addInstantiatingClass(node.type)

                        is DataFlowIR.Node.StaticCall ->
                            dfs(node.callee, node.returnType)

                        is DataFlowIR.Node.FunctionReference ->
                            dfs(node.symbol, node.returnType)

                        is DataFlowIR.Node.FieldRead ->
                            if (entryPoint == null && node.field.type.isFinal)
                                addInstantiatingClass(node.field.type)

                        is DataFlowIR.Node.FieldWrite ->
                            if (entryPoint == null && node.field.type.isFinal)
                                addInstantiatingClass(node.field.type)

                        is DataFlowIR.Node.VirtualCall -> {
                            if (node.receiverType == DataFlowIR.Type.Virtual)
                                continue@nodeLoop
                            val receiverType = node.receiverType.resolved()
                            val vCallReturnType = node.callee.returnParameter.type.resolved()

                            DEBUG_OUTPUT(1) {
                                println("Adding virtual callsite:")
                                println("    Receiver: $receiverType")
                                println("    Callee: ${node.callee}")
                                println("    Inheritors:")
                                typeHierarchy.inheritorsOf(receiverType).forEach { println("        $it") }
                                println("    Encountered so far:")
                                typeHierarchy.inheritorsOf(receiverType)
                                        .filter { instantiatingClasses.contains(it) }
                                        .forEach { println("        $it") }
                            }

                            if (entryPoint == null && vCallReturnType.isFinal) {
                                // If we are in a library and facing final return type then
                                // this type can be returned by some user of this library, so propagate it explicitly.
                                addInstantiatingClass(vCallReturnType)
                            }

                            typesVirtualCallSites.getOrPut(receiverType) { mutableListOf() }.add(node)
                            typeHierarchy.inheritorsOf(receiverType)
                                    .filter { instantiatingClasses.contains(it) }
                                    .forEach { processVirtualCall(node, it) }
                        }
                    }
                }
            }
        }

        fun BitSet.copy() = BitSet(this.size()).apply { this.or(this@copy) }

        fun printPathToType(node: Node, type: Int) {
            val visited = mutableSetOf<Node>()
            val prev = mutableMapOf<Node, Node>()
            var front = mutableListOf<Node>()
            front.add(node)
            visited.add(node)
            lateinit var source: Node.Source
            bfs@while (front.isNotEmpty()) {
                val prevFront = front
                front = mutableListOf()
                for (from in prevFront) {
                    val reversedEdges = from.reversedEdges
                    if (reversedEdges != null)
                        for (to in reversedEdges) {
                            if (!visited.contains(to) && to.types[type]) {
                                visited.add(to)
                                prev[to] = from
                                front.add(to)
                                if (to is Node.Source) {
                                    source = to
                                    break@bfs
                                }
                            }
                        }
                    val reversedCastEdges = from.reversedCastEdges
                    if (reversedCastEdges != null)
                        for (castEdge in reversedCastEdges) {
                            val to = castEdge.node
                            if (!visited.contains(to) && castEdge.suitableTypes[type] && to.types[type]) {
                                visited.add(to)
                                prev[to] = from
                                front.add(to)
                                if (to is Node.Source) {
                                    source = to
                                    break@bfs
                                }
                            }
                        }
                }
            }
            var cur: Node = source
            do {
                println("    #${cur.id}")
                cur = prev[cur]!!
            } while (cur != node)
        }

        private inner class Condensation(val multiNodes: IntArray, val topologicalOrder: IntArray) {
            inline fun forEachNode(node: Node, block: (Node) -> Unit) {
                for (i in node.multiNodeStart until node.multiNodeEnd)
                    block(constraintGraph.nodes[multiNodes[i]])
            }
        }

        private inner class CondensationBuilder {
            val nodes = constraintGraph.nodes
            val nodesCount = nodes.size
            val visited = BitSet(nodesCount)
            var index = 0
            val multiNodes = IntArray(nodesCount)

            fun build(): Condensation {
                // First phase.
                val order = IntArray(nodesCount)
                for (node in nodes) {
                    if (!visited[node.id])
                        findOrder(node, order)
                }

                // Second phase.
                visited.clear()
                index = 0
                var multiNodesCount = 0
                val multiNodesRepresentatives = IntArray(nodesCount)
                for (i in order.size - 1 downTo 0) {
                    val nodeIndex = order[i]
                    if (visited[nodeIndex]) continue
                    multiNodesRepresentatives[multiNodesCount++] = nodeIndex
                    val start = index
                    paint(nodes[nodeIndex])
                    val end = index
                    for (multiNodeIndex in start until end) {
                        val node = nodes[multiNodes[multiNodeIndex]]
                        node.multiNodeStart = start
                        node.multiNodeEnd = end
                    }
                }

                // Topsort the built condensation.
                visited.clear()
                index = 0
                val multiNodesOrder = IntArray(multiNodesCount)
                for (v in multiNodesRepresentatives) {
                    if (!visited[v])
                        findMultiNodesOrder(nodes[v], multiNodesOrder)
                }
                multiNodesOrder.reverse()

                return Condensation(multiNodes, multiNodesOrder)
            }

            private fun findOrder(node: Node, order: IntArray) {
                visited.set(node.id)
                node.directEdges?.forEach {
                    if (!visited[it.id])
                        findOrder(it, order)
                }
                order[index++] = node.id
            }

            private fun paint(node: Node) {
                visited.set(node.id)
                multiNodes[index++] = node.id
                node.reversedEdges?.forEach {
                    if (!visited[it.id])
                        paint(it)
                }
            }

            private fun findMultiNodesOrder(multiNode: Node, order: IntArray) {
                visited.set(multiNode.id)
                for (v in multiNode.multiNodeStart until multiNode.multiNodeEnd) {
                    val node = nodes[multiNodes[v]]
                    node.directEdges?.forEach {
                        val nextMultiNode = multiNodes[it.multiNodeStart]
                        if (!visited[nextMultiNode])
                            findMultiNodesOrder(nodes[nextMultiNode], order)
                    }

                }
                order[index++] = multiNode.id
            }
        }

        fun analyze(): AnalysisResult {
            val functions = moduleDFG.functions + externalModulesDFG.functionDFGs
            val typeHierarchy = TypeHierarchy(symbolTable.classMap.values.filterIsInstance<DataFlowIR.Type.Declared>() +
                                              externalModulesDFG.allTypes)
            val rootSet = computeRootSet(context, moduleDFG, externalModulesDFG)

            val instantiatingClasses =
                    InstantiationsSearcher(rootSet, typeHierarchy).search()
                            .asSequence()
                            .withIndex()
                            .associate { it.value to (it.index + 1 /* 0 is reserved for [DataFlowIR.Type.Virtual] */) }
            val allTypes = listOf(DataFlowIR.Type.Virtual) + instantiatingClasses.asSequence().sortedBy { it.value }.map { it.key }

            val nodesMap = mutableMapOf<DataFlowIR.Node, Node>()
            val constraintGraphBuilder =
                    ConstraintGraphBuilder(nodesMap, functions, typeHierarchy, instantiatingClasses, allTypes, rootSet, true)
            constraintGraphBuilder.build()

            DEBUG_OUTPUT(0) {
                println("FULL CONSTRAINT GRAPH")
                constraintGraph.nodes.forEach {
                    println("    NODE #${it.id}")
                    it.directEdges?.forEach {
                        println("        EDGE: #${it.id}z")
                    }
                    it.directCastEdges?.forEach {
                        println("        CAST EDGE: #${it.node.id}z casted to ${it.suitableTypes.format(allTypes)}")
                    }
                    allTypes.forEachIndexed { index, type ->
                        if (it.types[index])
                            println("        TYPE: $type")
                    }
                }
            }

            constraintGraph.nodes.forEach {
                if (it is Node.Source) {
                    assert(it.reversedEdges?.isEmpty() ?: true) { "A source node #${it.id} has incoming edges" }
                    assert(it.reversedCastEdges?.isEmpty() ?: true) { "A source node #${it.id} has incoming edges" }
                }
            }

            DEBUG_OUTPUT(0) {
                println("CONSTRAINT GRAPH: ${constraintGraph.nodes.size} nodes, " +
                    "${constraintGraph.nodes.sumBy { (it.directEdges?.size ?: 0) + (it.directCastEdges?.size ?: 0) } } edges")
            }

            val condensation = CondensationBuilder().build()
            val topologicalOrder = condensation.topologicalOrder.map { constraintGraph.nodes[it] }

            DEBUG_OUTPUT(0) {
                println("CONDENSATION")
                topologicalOrder.forEachIndexed { index, multiNode ->
                    println("    MULTI-NODE #$index")
                    condensation.forEachNode(multiNode) {
                        println("        #${it.id}: ${it.toString(allTypes)}")
                    }
                }
            }

            topologicalOrder.forEachIndexed { index, multiNode ->
                condensation.forEachNode(multiNode) { node -> node.priority = index }
            }

            val badEdges = mutableListOf<Pair<Node, Node.CastEdge>>()
            for (node in constraintGraph.nodes) {
                node.directCastEdges
                        ?.filter { it.node.priority < node.priority } // Contradicts topological order.
                        ?.forEach { badEdges += node to it }
            }
            badEdges.sortBy { it.second.node.priority } // Heuristic.

            // First phase - greedy phase.
            var iterations = 0
            val maxNumberOfIterations = 2
            do {
                ++iterations
                // Handle all 'right-directed' edges.
                // TODO: this is pessimistic handling of [DataFlowIR.Type.Virtual], think how to do it better.
                for (multiNode in topologicalOrder) {
                    if (multiNode.multiNodeSize == 1 && multiNode is Node.Source)
                        continue // A source has no incoming edges.
                    val types = BitSet()
                    condensation.forEachNode(multiNode) { node ->
                        node.reversedEdges?.forEach { types.or(it.types) }
                        node.reversedCastEdges
                                ?.filter { it.node.priority < node.priority } // Doesn't contradict topological order.
                                ?.forEach {
                                    val sourceTypes = it.node.types.copy()
                                    sourceTypes.and(it.suitableTypes)
                                    types.or(sourceTypes)
                                }
                    }
                    condensation.forEachNode(multiNode) { node -> node.types.or(types) }
                }
                if (iterations >= maxNumberOfIterations) break

                var end = true
                for ((sourceNode, edge) in badEdges) {
                    val distNode = edge.node
                    val missingTypes = sourceNode.types.copy().apply { andNot(distNode.types) }
                    missingTypes.and(edge.suitableTypes)
                    if (!missingTypes.isEmpty) {
                        end = false
                        distNode.types.or(missingTypes)
                    }
                }
            } while (!end)

            // Second phase - do BFS.
            val nodesCount = constraintGraph.nodes.size
            val marked = BitSet(nodesCount)
            var front = IntArray(nodesCount)
            var prevFront = IntArray(nodesCount)
            var frontSize = 0
            val tempBitSet = BitSet()
            for ((sourceNode, edge) in badEdges) {
                val distNode = edge.node
                tempBitSet.clear()
                tempBitSet.or(sourceNode.types)
                tempBitSet.andNot(distNode.types)
                tempBitSet.and(edge.suitableTypes)
                distNode.types.or(tempBitSet)
                if (!marked[distNode.id] && !tempBitSet.isEmpty) {
                    marked.set(distNode.id)
                    front[frontSize++] = distNode.id
                }
            }

            while (frontSize > 0) {
                val prevFrontSize = frontSize
                frontSize = 0
                val temp = front
                front = prevFront
                prevFront = temp
                for (i in 0 until prevFrontSize) {
                    marked[prevFront[i]] = false
                    val node = constraintGraph.nodes[prevFront[i]]
                    node.directEdges?.forEach { distNode ->
                        if (marked[distNode.id])
                            distNode.types.or(node.types)
                        else {
                            tempBitSet.clear()
                            tempBitSet.or(node.types)
                            tempBitSet.andNot(distNode.types)
                            distNode.types.or(node.types)
                            if (!marked[distNode.id] && !tempBitSet.isEmpty) {
                                marked.set(distNode.id)
                                front[frontSize++] = distNode.id
                            }
                        }
                    }
                    node.directCastEdges?.forEach { edge ->
                        val distNode = edge.node
                        tempBitSet.clear()
                        tempBitSet.or(node.types)
                        tempBitSet.andNot(distNode.types)
                        tempBitSet.and(edge.suitableTypes)
                        distNode.types.or(tempBitSet)
                        if (!marked[distNode.id] && !tempBitSet.isEmpty) {
                            marked.set(distNode.id)
                            front[frontSize++] = distNode.id
                        }
                    }
                }
            }

            DEBUG_OUTPUT(0) {
                topologicalOrder.forEachIndexed { index, multiNode ->
                    println("Types of multi-node #$index")
                    condensation.forEachNode(multiNode) { node ->
                        println("    Node #${node.id}")
                        allTypes.asSequence()
                                .withIndex()
                                .filter { node.types[it.index] }.toList()
                                .forEach { println("        ${it.value}") }
                    }
                }
            }

            val result = mutableMapOf<DataFlowIR.Node.VirtualCall, Pair<DevirtualizedCallSite, DataFlowIR.FunctionSymbol>>()
            val nothing = symbolTable.classMap[context.ir.symbols.nothing.owner]
            functions.values
                    .asSequence()
                    .filter { constraintGraph.functions.containsKey(it.symbol) }
                    .flatMap { it.body.nodes.asSequence() }
                    .filterIsInstance<DataFlowIR.Node.VirtualCall>()
                    .forEach { virtualCall ->
                        assert (nodesMap[virtualCall] != null) { "Node for virtual call $virtualCall has not been built" }
                        val virtualCallSiteReceivers = constraintGraph.virtualCallSiteReceivers[virtualCall]
                                ?: error("virtualCallSiteReceivers were not built for virtual call $virtualCall")
                        if (virtualCallSiteReceivers.receiver.types[VIRTUAL_TYPE_ID]) {

                            DEBUG_OUTPUT(0) {
                                println("Unable to devirtualize callsite " +
                                        (virtualCall.irCallSite?.let { ir2stringWhole(it) } ?: virtualCall.callee.toString()))
                                println("    receiver is Virtual")
                                printPathToType(virtualCallSiteReceivers.receiver, VIRTUAL_TYPE_ID)
                            }

                            return@forEach
                        }
                        DEBUG_OUTPUT(0) {
                            println("Devirtualized callsite " +
                                    (virtualCall.irCallSite?.let { ir2stringWhole(it) } ?: virtualCall.callee.toString()))
                        }
                        val inheritorsOfReceiverType = virtualCallSiteReceivers.devirtualizedCallees.map { it.receiverType }.toSet()
                        val possibleReceivers = allTypes.asSequence()
                                .withIndex()
                                .filter { virtualCallSiteReceivers.receiver.types[it.index] }
                                .filter { inheritorsOfReceiverType.contains(it.value) }
                                .filterNot { it.value == nothing }
                                .map {
                                    DEBUG_OUTPUT(0) {
                                        println("Path to type ${it.value}")
                                        printPathToType(virtualCallSiteReceivers.receiver, it.index)
                                    }
                                    it.value
                                }.toList()

                        val map = virtualCallSiteReceivers.devirtualizedCallees.associateBy({ it.receiverType }, { it })
                        result[virtualCall] = DevirtualizedCallSite(virtualCall.callee.resolved(),
                                possibleReceivers.map { receiverType ->
                                    assert(map[receiverType] != null) {
                                        "Non-expected receiver type $receiverType at call site: " +
                                                (virtualCall.irCallSite?.let { ir2stringWhole(it) }
                                                        ?: virtualCall.toString())
                                    }
                                    val devirtualizedCallee = map[receiverType]!!
                                    val callee = devirtualizedCallee.callee
                                    if (callee is DataFlowIR.FunctionSymbol.Declared && callee.symbolTableIndex < 0)
                                        error("Function ${devirtualizedCallee.receiverType}.$callee cannot be called virtually," +
                                                " but actually is at call site: " +
                                                (virtualCall.irCallSite?.let { ir2stringWhole(it) }
                                                        ?: virtualCall.toString()))
                                    devirtualizedCallee
                                }) to virtualCallSiteReceivers.caller
                    }

            DEBUG_OUTPUT(0) {
                println("Devirtualized from current module:")
                result.forEach { virtualCall, devirtualizedCallSite ->
                    if (virtualCall.irCallSite != null) {
                        println("DEVIRTUALIZED")
                        println("FUNCTION: ${devirtualizedCallSite.second}")
                        println("CALL SITE: ${virtualCall.irCallSite?.let { ir2stringWhole(it) } ?: virtualCall.toString()}")
                        println("POSSIBLE RECEIVERS:")
                        devirtualizedCallSite.first.possibleCallees.forEach { println("    TYPE: ${it.receiverType}") }
                        devirtualizedCallSite.first.possibleCallees.forEach { println("    FUN: ${it.callee}") }
                        println()
                    }
                }
                println("Devirtualized from external modules:")
                result.forEach { virtualCall, devirtualizedCallSite ->
                    if (virtualCall.irCallSite == null) {
                        println("DEVIRTUALIZED")
                        println("FUNCTION: ${devirtualizedCallSite.second}")
                        println("CALL SITE: ${virtualCall.irCallSite?.let { ir2stringWhole(it) } ?: virtualCall.toString()}")
                        println("POSSIBLE RECEIVERS:")
                        devirtualizedCallSite.first.possibleCallees.forEach { println("    TYPE: ${it.receiverType}") }
                        devirtualizedCallSite.first.possibleCallees.forEach { println("    FUN: ${it.callee}") }
                        println()
                    }
                }
            }

            return AnalysisResult(result.asSequence().associateBy({ it.key }, { it.value.first }), typeHierarchy, instantiatingClasses.keys)
        }

        private inner class ConstraintGraphBuilder(val functionNodesMap: MutableMap<DataFlowIR.Node, Node>,
                                                   val functions: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>,
                                                   val typeHierarchy: TypeHierarchy,
                                                   val instantiatingClasses: Map<DataFlowIR.Type.Declared, Int>,
                                                   val allTypes: List<DataFlowIR.Type.Declared>,
                                                   val rootSet: List<DataFlowIR.FunctionSymbol>,
                                                   val useTypes: Boolean) {

            private val variables = mutableMapOf<DataFlowIR.Node.Variable, Node>()

            private fun concreteType(type: DataFlowIR.Type.Declared): Int {
                assert(!(type.isAbstract && type.isFinal)) { "Incorrect type: $type" }
                return if (type.isAbstract)
                    VIRTUAL_TYPE_ID
                else {
                    instantiatingClasses[type] ?: error("Type $type is not instantiated")
                }
            }

            private fun ordinaryNode(nameBuilder: () -> String) =
                    constraintGraph.addNode { Node.Ordinary(it, nameBuilder) }

            private fun sourceNode(typeId: Int, nameBuilder: () -> String) =
                    constraintGraph.addNode { Node.Source(it, typeId, nameBuilder) }

            private fun concreteClass(type: DataFlowIR.Type.Declared) =
                    constraintGraph.concreteClasses.getOrPut(type) { sourceNode(concreteType(type)) { "Class\$$type" } }

            private fun fieldNode(field: DataFlowIR.Field) =
                    constraintGraph.fields.getOrPut(field) {
                        val fieldNode = ordinaryNode { "Field\$$field" }
                        if (entryPoint == null) {
                            // TODO: This is conservative.
                            val fieldType = field.type.resolved()
                            // Some user of our library might place some value into the field.
                            if (fieldType.isFinal)
                                concreteClass(fieldType).addEdge(fieldNode)
                            else
                                constraintGraph.virtualNode.addEdge(fieldNode)
                        }
                        fieldNode
                    }

            private var stack = mutableListOf<DataFlowIR.FunctionSymbol>()

            fun build() {
                if (entryPoint != null) {
                    // String arguments are implicitly put into the <args> array parameter of <main>.
                    concreteClass(symbolTable.mapType(context.irBuiltIns.stringType).resolved()).addEdge(
                            fieldNode(constraintGraph.arrayItemField)
                    )
                }
                rootSet.forEach { createFunctionConstraintGraph(it, true) }
                while (stack.isNotEmpty()) {
                    val symbol = stack.pop()
                    val function = functions[symbol] ?: continue
                    val body = function.body
                    val functionConstraintGraph = constraintGraph.functions[symbol]!!

                    body.nodes.forEach { dfgNodeToConstraintNode(functionConstraintGraph, it) }
                    functionNodesMap[body.returns]!!.addEdge(functionConstraintGraph.returns)
                    functionNodesMap[body.throws]!!.addEdge(functionConstraintGraph.throws)


                    DEBUG_OUTPUT(0) {
                        println("CONSTRAINT GRAPH FOR $symbol")
                        val ids = function.body.nodes.asSequence().withIndex().associateBy({ it.value }, { it.index })
                        for (node in function.body.nodes) {
                            println("FT NODE #${ids[node]}")
                            DataFlowIR.Function.printNode(node, ids)
                            val constraintNode = functionNodesMap[node] ?: variables[node] ?: break
                            println("       CG NODE #${constraintNode.id}: ${constraintNode.toString(allTypes)}")
                            println()
                        }
                        println("Returns: #${ids[function.body.returns]}")
                        println()
                    }
                }
            }

            private fun createFunctionConstraintGraph(symbol: DataFlowIR.FunctionSymbol, isRoot: Boolean): Function? {
                if (symbol is DataFlowIR.FunctionSymbol.External) return null
                constraintGraph.functions[symbol]?.let { return it }

                val parameters = Array(symbol.parameters.size) { ordinaryNode { "Param#$it\$$symbol" } }
                if (isRoot) {
                    // Exported function from the current module.
                    symbol.parameters.forEachIndexed { index, type ->
                        val resolvedType = type.type.resolved()
                        val node = if (!resolvedType.isFinal)
                                       constraintGraph.virtualNode // TODO: OBJC-INTEROP-GENERATED-CLASSES
                                   else
                                       concreteClass(resolvedType)
                        node.addEdge(parameters[index])
                    }
                }

                val returnsNode = ordinaryNode { "Returns\$$symbol" }
                val throwsNode = ordinaryNode { "Throws\$$symbol" }
                val functionConstraintGraph = Function(symbol, parameters, returnsNode, throwsNode)
                constraintGraph.functions[symbol] = functionConstraintGraph

                stack.push(symbol)

                return functionConstraintGraph
            }

            private fun createCastEdge(node: Node, type: DataFlowIR.Type.Declared): Node.CastEdge {
                val suitableTypes = BitSet()
                suitableTypes.set(VIRTUAL_TYPE_ID)
                for (inheritor in typeHierarchy.inheritorsOf(type)) {
                    instantiatingClasses[inheritor]?.let { suitableTypes.set(it) }
                }
                return Node.CastEdge(node, suitableTypes)
            }

            private fun doCast(function: Function, node: Node, type: DataFlowIR.Type.Declared): Node {
                val castNode = ordinaryNode { "Cast\$${function.symbol}" }
                val castEdge = createCastEdge(castNode, type)
                node.addCastEdge(castEdge)
                return castNode
            }

            private fun edgeToConstraintNode(function: Function,
                                             edge: DataFlowIR.Edge): Node {
                val result = dfgNodeToConstraintNode(function, edge.node)
                val castToType = edge.castToType?.resolved() ?: return result
                return doCast(function, result, castToType)
            }

            /**
             * Takes a function DFG's node and creates a constraint graph node corresponding to it.
             * Also creates all necessary edges.
             */
            private fun dfgNodeToConstraintNode(function: Function, node: DataFlowIR.Node): Node {

                fun edgeToConstraintNode(edge: DataFlowIR.Edge): Node =
                        edgeToConstraintNode(function, edge)

                fun argumentToConstraintNode(argument: Any): Node =
                        when (argument) {
                            is Node -> argument
                            is DataFlowIR.Edge -> edgeToConstraintNode(argument)
                            else -> error("Unexpected argument: $argument")
                        }

                fun doCall(callee: Function, arguments: List<Any>, returnType: DataFlowIR.Type.Declared): Node {
                    assert(callee.parameters.size == arguments.size) {
                        "Function ${callee.symbol} takes ${callee.parameters.size} but caller ${function.symbol}" +
                                " provided ${arguments.size}"
                    }
                    callee.parameters.forEachIndexed { index, parameter ->
                        val argument = argumentToConstraintNode(arguments[index])
                        argument.addEdge(parameter)
                    }
                    return if (!useTypes || returnType == callee.symbol.returnParameter.type.resolved())
                        callee.returns
                    else
                        doCast(function, callee.returns, returnType)
                }

                fun doCall(callee: DataFlowIR.FunctionSymbol,
                           arguments: List<Any>,
                           returnType: DataFlowIR.Type.Declared): Node {
                    val resolvedCallee = callee.resolved()
                    val calleeConstraintGraph = createFunctionConstraintGraph(resolvedCallee, false)
                    return if (calleeConstraintGraph == null) {
                        constraintGraph.externalFunctions.getOrPut(resolvedCallee) {
                            val fictitiousReturnNode = ordinaryNode { "External$resolvedCallee" }
                            if (returnType.isFinal)
                                concreteClass(returnType).addEdge(fictitiousReturnNode)
                            else {
                                constraintGraph.virtualNode.addEdge(fictitiousReturnNode)
                                // TODO: Unconservative way - when we can use it?
                                // TODO: OBJC-INTEROP-GENERATED-CLASSES
//                                typeHierarchy.inheritorsOf(returnType)
//                                        .filterNot { it.isAbstract }
//                                        .filter { instantiatingClasses.containsKey(it) }
//                                        .forEach { concreteClass(it).addEdge(fictitiousReturnNode) }
                            }
                            fictitiousReturnNode
                        }
                    } else {
                        calleeConstraintGraph.throws.addEdge(function.throws)
                        doCall(calleeConstraintGraph, arguments, returnType)
                    }
                }

                fun readField(field: DataFlowIR.Field, actualType: DataFlowIR.Type.Declared): Node {
                    val fieldNode = fieldNode(field)
                    val expectedType = field.type.resolved()
                    return if (!useTypes || actualType == expectedType)
                        fieldNode
                    else
                        doCast(function, fieldNode, actualType)
                }

                fun writeField(field: DataFlowIR.Field, actualType: DataFlowIR.Type.Declared, value: Node) {
                    val fieldNode = fieldNode(field)
                    val expectedType = field.type.resolved()
                    val castedValue = if (!useTypes || actualType == expectedType)
                        value
                    else
                        doCast(function, value, actualType)
                    castedValue.addEdge(fieldNode)
                }

                if (node is DataFlowIR.Node.Variable && node.kind != DataFlowIR.VariableKind.Temporary) {
                    var variableNode = variables[node]
                    if (variableNode == null) {
                        variableNode = ordinaryNode { "Variable\$${function.symbol}" }
                        variables[node] = variableNode
                        for (value in node.values) {
                            edgeToConstraintNode(value).addEdge(variableNode)
                        }
                        if (node.kind == DataFlowIR.VariableKind.CatchParameter)
                            function.throws.addCastEdge(createCastEdge(variableNode, node.type.resolved()))
                    }
                    return variableNode
                }

                return functionNodesMap.getOrPut(node) {
                    when (node) {
                        is DataFlowIR.Node.Const ->
                            sourceNode(concreteType(node.type.resolved())) { "Const\$${function.symbol}" }

                        DataFlowIR.Node.Null -> constraintGraph.voidNode

                        is DataFlowIR.Node.Parameter ->
                            function.parameters[node.index]

                        is DataFlowIR.Node.StaticCall ->
                            doCall(node.callee, node.arguments, node.returnType.resolved())

                        is DataFlowIR.Node.NewObject -> {
                            val returnType = node.constructedType.resolved()
                            val instanceNode = concreteClass(returnType)
                            doCall(node.callee, listOf(instanceNode) + node.arguments, returnType)
                            instanceNode
                        }

                        is DataFlowIR.Node.VirtualCall -> {
                            val callee = node.callee
                            val receiverType = node.receiverType.resolved()

                            DEBUG_OUTPUT(0) {
                                println("Virtual call")
                                println("Caller: ${function.symbol}")
                                println("Callee: $callee")
                                println("Receiver type: $receiverType")
                            }

                            val possibleReceiverTypes =
                                    if (receiverType == DataFlowIR.Type.Virtual)
                                        emptyList()
                                    else
                                        typeHierarchy.inheritorsOf(receiverType).filter { instantiatingClasses.containsKey(it) }
                            val callees = possibleReceiverTypes.map {
                                when (node) {
                                    is DataFlowIR.Node.VtableCall ->
                                        it.vtable[node.calleeVtableIndex]

                                    is DataFlowIR.Node.ItableCall ->
                                        it.itable[node.calleeHash]!!

                                    else -> error("Unreachable")
                                }
                            }

                            DEBUG_OUTPUT(0) {
                                println("Possible callees:")
                                callees.forEach { println("$it") }
                                println()
                            }

                            val returnType = node.returnType.resolved()
                            val receiverNode = edgeToConstraintNode(node.arguments[0])
                            if (receiverType == DataFlowIR.Type.Virtual)
                                constraintGraph.virtualNode.addEdge(receiverNode)
                            val castedReceiver = ordinaryNode { "CastedReceiver\$${function.symbol}" }
                            receiverNode.addEdge(castedReceiver)
                            val arguments = listOf(castedReceiver) + node.arguments.drop(1)

                            val returnsNode = ordinaryNode { "VirtualCallReturns\$${function.symbol}" }
                            callees.forEachIndexed { index, actualCallee ->
                                doCall(actualCallee, arguments, returnType).addEdge(returnsNode)
                            }
                            // Add cast to [Virtual] edge from receiver to returns, if return type is not final.
                            // With this we're reflecting the fact that unknown function can return anything.
                            val virtualTypeFilter = BitSet().apply { set(VIRTUAL_TYPE_ID) }
                            if (!returnType.isFinal && entryPoint == null) {
                                receiverNode.addCastEdge(Node.CastEdge(returnsNode, virtualTypeFilter))
                            }
                            // And throw anything.
                            receiverNode.addCastEdge(Node.CastEdge(function.throws, virtualTypeFilter))

                            if (callees.isEmpty() && returnType.isFinal && entryPoint == null) {
                                // If we are in a library and facing final return type with no possible callees -
                                // this type still can be returned by some user of this library, so propagate it explicitly.
                                concreteClass(returnType).addEdge(returnsNode)
                            }

                            val devirtualizedCallees = possibleReceiverTypes.mapIndexed { index, possibleReceiverType ->
                                DevirtualizedCallee(possibleReceiverType, callees[index])
                            }
                            constraintGraph.virtualCallSiteReceivers[node] =
                                    VirtualCallSiteReceivers(castedReceiver, function.symbol, devirtualizedCallees)
                            returnsNode
                        }

                        is DataFlowIR.Node.Singleton -> {
                            val type = node.type.resolved()
                            val instanceNode = concreteClass(type)
                            node.constructor?.let { doCall(it, listOf(instanceNode), type) }
                            instanceNode
                        }

                        is DataFlowIR.Node.AllocInstance -> {
                            concreteClass(node.type.resolved())
                        }

                        is DataFlowIR.Node.FunctionReference -> {
                            concreteClass(node.type.resolved())
                        }

                        is DataFlowIR.Node.FieldRead ->
                            readField(node.field, node.field.type.resolved())

                        is DataFlowIR.Node.FieldWrite -> {
                            writeField(node.field, node.field.type.resolved(), edgeToConstraintNode(node.value))
                            constraintGraph.voidNode
                        }

                        is DataFlowIR.Node.ArrayRead ->
                            readField(constraintGraph.arrayItemField, node.type.resolved())

                        is DataFlowIR.Node.ArrayWrite -> {
                            writeField(constraintGraph.arrayItemField, node.type.resolved(), edgeToConstraintNode(node.value))
                            constraintGraph.voidNode
                        }

                        is DataFlowIR.Node.Variable ->
                            node.values.map { edgeToConstraintNode(it) }.let { values ->
                                ordinaryNode { "TempVar\$${function.symbol}" }.also { node ->
                                    values.forEach { it.addEdge(node) }
                                }
                            }

                        else -> error("Unreachable")
                    }
                }
            }
        }

    }

    class DevirtualizedCallee(val receiverType: DataFlowIR.Type, val callee: DataFlowIR.FunctionSymbol)

    class DevirtualizedCallSite(val callee: DataFlowIR.FunctionSymbol, val possibleCallees: List<DevirtualizedCallee>)

    class AnalysisResult(val devirtualizedCallSites: Map<DataFlowIR.Node.VirtualCall, DevirtualizedCallSite>,
                         val typeHierarchy: DevirtualizationAnalysis.TypeHierarchy,
                         val instantiatingClasses: Set<DataFlowIR.Type.Declared>)

    fun run(irModule: IrModuleFragment, context: Context, moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG)
            : AnalysisResult {
        val devirtualizationAnalysisResult = DevirtualizationAnalysis(context, moduleDFG, externalModulesDFG).analyze()
        val devirtualizedCallSites =
                devirtualizationAnalysisResult.devirtualizedCallSites
                        .asSequence()
                        .filter { it.key.irCallSite != null }
                        .associate { it.key.irCallSite!! to it.value }
        devirtualize(irModule, context, externalModulesDFG, devirtualizedCallSites)
        removeRedundantCoercions(irModule, context)
        return devirtualizationAnalysisResult
    }

    /**
     * TODO: JVM inliner crashed on attempt inline this function from transform.kt with:
     *  j.l.IllegalStateException: Couldn't obtain compiled function body for
     *  public inline fun <reified T : org.jetbrains.kotlin.ir.IrElement> kotlin.collections.MutableList<T>.transform...
     */
    private inline fun <reified T : IrElement> MutableList<T>.transform(transformation: (T) -> IrElement) {
        forEachIndexed { i, item ->
            set(i, transformation(item) as T)
        }
    }

    private fun IrExpression.isBoxOrUnboxCall() =
            (this is IrCall && symbol.owner.origin == DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION)

    private fun devirtualize(irModule: IrModuleFragment, context: Context, externalModulesDFG: ExternalModulesDFG,
                             devirtualizedCallSites: Map<IrCall, DevirtualizedCallSite>) {
        val symbols = context.ir.symbols
        val nativePtrEqualityOperatorSymbol = symbols.areEqualByValue[PrimitiveBinaryType.POINTER]!!
        val optimize = context.shouldOptimize()

        fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
            if (this is DataFlowIR.Type.Declared) return this
            val hash = (this as DataFlowIR.Type.External).hash
            return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $hash")
        }

        fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        fun IrBuilderWithScope.irCoerce(value: IrExpression, coercion: IrFunctionSymbol?) =
                if (coercion == null)
                    value
                else irCall(coercion).apply {
                    addArguments(listOf(coercion.descriptor.explicitParameters.single() to value))
                }

        fun IrBuilderWithScope.irCoerce(value: IrExpression, coercion: DataFlowIR.FunctionSymbol.Declared?) =
                if (coercion == null)
                    value
                else irCall(coercion.irFunction!!).apply {
                    putValueArgument(0, value)
                }

        class PossiblyCoercedValue(val value: IrVariable, val coercion: IrFunctionSymbol?) {
            fun getFullValue(irBuilder: IrBuilderWithScope) = irBuilder.run {
                irCoerce(irGet(value), coercion)
            }
        }

        fun <T : IrElement> IrStatementsBuilder<T>.irTemporary(value: IrExpression, tempName: String, type: IrType): IrVariable {
            val originalKotlinType = type.originalKotlinType ?: type.toKotlinType()
            val descriptor = IrTemporaryVariableDescriptorImpl(
                    scope.scopeOwner, Name.identifier(tempName), originalKotlinType, false)

            val temporary = IrVariableImpl(
                    value.startOffset, value.endOffset, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                    descriptor,
                    type,
                    value
            )

            +temporary
            return temporary
        }

        fun <T : IrElement> IrStatementsBuilder<T>.irSplitCoercion(expression: IrExpression, tempName: String, actualType: IrType) =
                if (!expression.isBoxOrUnboxCall())
                    PossiblyCoercedValue(irTemporary(expression, tempName, actualType), null)
                else {
                    val coercion = expression as IrCall
                    PossiblyCoercedValue(
                            irTemporary(coercion.getValueArgument(0)!!, tempName,
                                    coercion.symbol.owner.explicitParameters.single().type)
                            , coercion.symbol)
                }

        fun getTypeConversion(actualType: DataFlowIR.FunctionParameter,
                              targetType: DataFlowIR.FunctionParameter): DataFlowIR.FunctionSymbol.Declared? {
            if (actualType.boxFunction == null && targetType.boxFunction == null) return null
            if (actualType.boxFunction != null && targetType.boxFunction != null) {
                assert (actualType.type.resolved() == targetType.type.resolved())
                        { "Inconsistent types: ${actualType.type} and ${targetType.type}" }
                return null
            }
            if (actualType.boxFunction == null)
                return targetType.unboxFunction!!.resolved() as DataFlowIR.FunctionSymbol.Declared
            return actualType.boxFunction.resolved() as DataFlowIR.FunctionSymbol.Declared
        }

        fun IrCallImpl.putArgument(index: Int, value: IrExpression) {
            var receiversCount = 0
            val callee = symbol.owner
            if (callee.dispatchReceiverParameter != null)
                ++receiversCount
            if (callee.extensionReceiverParameter != null)
                ++receiversCount
            if (index >= receiversCount)
                putValueArgument(index - receiversCount, value)
            else {
                if (callee.dispatchReceiverParameter != null && index == 0)
                    dispatchReceiver = value
                else
                    extensionReceiver = value
            }
        }

        fun IrBuilderWithScope.irDevirtualizedCall(callSite: IrCall,
                                                   actualType: IrType,
                                                   devirtualizedCallee: DevirtualizedCallee,
                                                   arguments: List<IrExpression>): IrCall {
            val actualCallee = devirtualizedCallee.callee.irFunction!!
            val call = IrCallImpl(
                    callSite.startOffset, callSite.endOffset,
                    actualType,
                    actualCallee.symbol,
                    actualCallee.typeParameters.size,
                    actualCallee.valueParameters.size,
                    callSite.origin,
                    actualCallee.parentAsClass.symbol
            )
            if (actualCallee.explicitParameters.size == arguments.size) {
                arguments.forEachIndexed { index, argument -> call.putArgument(index, argument) }
                return call
            }
            assert(actualCallee.isSuspend && actualCallee.explicitParameters.size == arguments.size - 1) {
                "Incorrect number of arguments: expected [${actualCallee.explicitParameters.size}] but was [${arguments.size - 1}]\n" +
                        actualCallee.dump()
            }
            val continuation = arguments.last()
            for (index in 0..arguments.size - 2)
                call.putArgument(index, arguments[index])
            return irCall(context.ir.symbols.coroutineLaunchpad, actualType).apply {
                putValueArgument(0, call)
                putValueArgument(1, continuation)
            }
        }

        fun IrBuilderWithScope.irDevirtualizedCall(callee: IrCall, actualType: IrType,
                                                   devirtualizedCallee: DevirtualizedCallee,
                                                   arguments: List<PossiblyCoercedValue>): IrExpression {
            val actualCallee = devirtualizedCallee.callee as DataFlowIR.FunctionSymbol.Declared
            return actualCallee.bridgeTarget.let { bridgeTarget ->
                if (bridgeTarget == null)
                    irDevirtualizedCall(callee, actualType,
                            devirtualizedCallee,
                            arguments.map { it.getFullValue(this@irDevirtualizedCall) })
                else {
                    val callResult = irDevirtualizedCall(callee, actualType,
                            DevirtualizedCallee(devirtualizedCallee.receiverType, bridgeTarget),
                            arguments.mapIndexed { index, value ->
                                val coercion = getTypeConversion(actualCallee.parameters[index], bridgeTarget.parameters[index])
                                val fullValue = value.getFullValue(this@irDevirtualizedCall)
                                coercion?.let { irCoerce(fullValue, coercion) } ?: fullValue
                            })
                    val returnCoercion = getTypeConversion(bridgeTarget.returnParameter, actualCallee.returnParameter)
                    irCoerce(callResult, returnCoercion)
                }
            }
        }

        irModule.transformChildrenVoid(object: IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val devirtualizedCallSite = devirtualizedCallSites[expression]
                val possibleCallees = devirtualizedCallSite?.possibleCallees
                if (possibleCallees == null
                        || possibleCallees.any { it.callee is DataFlowIR.FunctionSymbol.External }
                        || possibleCallees.any { it.receiverType is DataFlowIR.Type.External })
                    return expression

                val callee = expression.symbol.owner
                val owner = callee.parentAsClass
                // TODO: Think how to evaluate different unfold factors (in terms of both execution speed and code size).
                val classMaxUnfoldFactor = 3
                val interfaceMaxUnfoldFactor = 3
                val maxUnfoldFactor = if (owner.isInterface) interfaceMaxUnfoldFactor else classMaxUnfoldFactor
                if (possibleCallees.size > maxUnfoldFactor) {
                    // Callsite too complicated to devirtualize.
                    return expression
                }

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val function = expression.symbol.owner
                val type = if (callee.isSuspend)
                               context.irBuiltIns.anyNType
                           else function.returnType
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val dispatchReceiver = expression.dispatchReceiver!!
                    return when {
                        possibleCallees.isEmpty() -> irBlock(expression) {
                            val throwExpr = irCall(symbols.throwInvalidReceiverTypeException.owner).apply {
                                putValueArgument(0,
                                        irCall(symbols.kClassImplConstructor.owner, listOf(dispatchReceiver.type)).apply {
                                            putValueArgument(0,
                                                    irCall(symbols.getObjectTypeInfo.owner).apply {
                                                        putValueArgument(0, dispatchReceiver)
                                                    })
                                        })
                            }
                            // Insert proper unboxing (unreachable code):
                            +irCoerce(throwExpr, symbols.getTypeConversion(throwExpr.type, type))
                        }

                        optimize && possibleCallees.size == 1 -> { // Monomorphic callsite.
                            irBlock(expression) {
                                val parameters = expression.getArgumentsWithSymbols().mapIndexed { index, arg ->
                                    irSplitCoercion(arg.second, "arg$index", arg.first.owner.type)
                                }
                                +irDevirtualizedCall(expression, type, possibleCallees[0], parameters)
                            }
                        }

                        else -> irBlock(expression) {
                            val arguments = expression.getArgumentsWithSymbols().mapIndexed { index, arg ->
                                irSplitCoercion(arg.second, "arg$index", arg.first.owner.type)
                            }
                            val typeInfo = irTemporary(irCall(symbols.getObjectTypeInfo).apply {
                                putValueArgument(0, arguments[0].getFullValue(this@irBlock))
                            })

                            val branches = mutableListOf<IrBranchImpl>()
                            possibleCallees.mapIndexedTo(branches) { index, devirtualizedCallee ->
                                val actualReceiverType = devirtualizedCallee.receiverType as DataFlowIR.Type.Declared
                                val expectedTypeInfo = IrClassReferenceImpl(
                                        startOffset, endOffset,
                                        symbols.nativePtrType,
                                        actualReceiverType.irClass!!.symbol,
                                        actualReceiverType.irClass.defaultType
                                )
                                val condition =
                                        if (optimize && index == possibleCallees.size - 1)
                                            irTrue() // Don't check last type in optimize mode.
                                        else
                                            irCall(nativePtrEqualityOperatorSymbol).apply {
                                                putValueArgument(0, irGet(typeInfo))
                                                putValueArgument(1, expectedTypeInfo)
                                            }
                                IrBranchImpl(
                                        startOffset = startOffset,
                                        endOffset   = endOffset,
                                        condition   = condition,
                                        result      = irDevirtualizedCall(expression, type, devirtualizedCallee, arguments)
                                )
                            }
                            if (!optimize) { // Add else branch throwing exception for debug purposes.
                                branches.add(IrBranchImpl(
                                        startOffset = startOffset,
                                        endOffset   = endOffset,
                                        condition   = irTrue(),
                                        result      = irCall(symbols.throwInvalidReceiverTypeException).apply {
                                            putValueArgument(0,
                                                    irCall(symbols.kClassImplConstructor,
                                                            listOf(dispatchReceiver.type)
                                                    ).apply {
                                                        putValueArgument(0, irGet(typeInfo))
                                                    }
                                            )
                                        })
                                )
                            }

                            +IrWhenImpl(
                                    startOffset = startOffset,
                                    endOffset   = endOffset,
                                    type        = type,
                                    origin      = expression.origin,
                                    branches    = branches
                            )
                        }
                    }
                }
            }
        })
    }

    private fun removeRedundantCoercions(irModule: IrModuleFragment, context: Context) {

        class PossiblyFoldedExpression(val expression: IrExpression, val folded: Boolean) {
            fun getFullExpression(coercion: IrCall, cast: IrTypeOperatorCall?): IrExpression {
                if (folded) return expression
                assert (coercion.dispatchReceiver == null && coercion.extensionReceiver == null) {
                    "Expected either <box> or <unbox> function without any receivers"
                }
                val castedExpression =
                        if (cast == null)
                            expression
                        else with (cast) {
                            IrTypeOperatorCallImpl(startOffset, endOffset, type, operator,
                                    typeOperand, typeOperandClassifier, expression)
                        }
                with (coercion) {
                    return IrCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, origin).apply {
                        putValueArgument(0, castedExpression)
                    }
                }
            }
        }

        // Possible values of a returnable block.
        val returnableBlockValues = mutableMapOf<IrReturnableBlock, MutableList<IrExpression>>()

        irModule.acceptChildrenVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitContainerExpression(expression: IrContainerExpression) {
                if (expression is IrReturnableBlock)
                    returnableBlockValues[expression] = mutableListOf()

                super.visitContainerExpression(expression)
            }

            override fun visitReturn(expression: IrReturn) {
                val returnableBlock = expression.returnTargetSymbol.owner as? IrReturnableBlock
                if (returnableBlock != null)
                    returnableBlockValues[returnableBlock]!!.add(expression.value)

                super.visitReturn(expression)
            }

        })

        irModule.transformChildrenVoid(object: IrElementTransformerVoid() {

            fun IrFunction.getCoercedClass(): IrClass {
                if (name.asString().endsWith("-box>"))
                    return valueParameters[0].type.classifierOrFail.owner as IrClass
                if (name.asString().endsWith("-unbox>"))
                    return returnType.classifierOrFail.owner as IrClass
                error("Unexpected coercion: ${this.dump()}")
            }

            fun fold(expression: IrExpression, coercion: IrCall, cast: IrTypeOperatorCall?,
                     transformRecursively: Boolean): PossiblyFoldedExpression {

                val transformer = this

                fun IrExpression.transformIfAsked() =
                        if (transformRecursively) this.transform(transformer, data = null) else this

                fun IrElement.transformIfAsked() =
                        if (transformRecursively) this.transform(transformer, data = null) else this

                val coercionDeclaringClass = coercion.symbol.owner.getCoercedClass()
                if (expression.isBoxOrUnboxCall()) {
                    expression as IrCall
                    val result =
                            if (coercionDeclaringClass == expression.symbol.owner.getCoercedClass())
                                expression.getArguments().single().second
                            else expression

                    return PossiblyFoldedExpression(result.transformIfAsked(), result != expression)
                }
                return when (expression) {
                    is IrReturnableBlock -> {
                        val foldedReturnableBlockValues = returnableBlockValues[expression]!!.associate {
                            it to fold(it, coercion, cast, false)
                        }
                        val someoneFolded = foldedReturnableBlockValues.any { it.value.folded }
                        val transformedReturnableBlock =
                                if (!someoneFolded)
                                    expression
                                else {
                                    val oldSymbol = expression.symbol
                                    val newSymbol = IrReturnableBlockSymbolImpl(expression.descriptor)
                                    val transformedReturnableBlock = with(expression) {
                                        IrReturnableBlockImpl(
                                                startOffset          = startOffset,
                                                endOffset            = endOffset,
                                                type                 = coercion.type,
                                                symbol               = newSymbol,
                                                origin               = origin,
                                                statements           = statements,
                                                inlineFunctionSymbol = inlineFunctionSymbol)
                                    }
                                    transformedReturnableBlock.transformChildrenVoid(object: IrElementTransformerVoid() {
                                        override fun visitExpression(expression: IrExpression): IrExpression {
                                            foldedReturnableBlockValues[expression]?.let {
                                                return it.getFullExpression(coercion, cast)
                                            }
                                            return super.visitExpression(expression)
                                        }

                                        override fun visitReturn(expression: IrReturn): IrExpression {
                                            expression.transformChildrenVoid(this)
                                            return if (expression.returnTargetSymbol != oldSymbol)
                                                expression
                                            else with(expression) {
                                                IrReturnImpl(
                                                        startOffset        = startOffset,
                                                        endOffset          = endOffset,
                                                        type               = context.irBuiltIns.nothingType,
                                                        returnTargetSymbol = newSymbol,
                                                        value              = value)
                                            }
                                        }
                                    })
                                    transformedReturnableBlock
                                }
                        if (transformRecursively)
                            transformedReturnableBlock.transformChildrenVoid(this)
                        PossiblyFoldedExpression(transformedReturnableBlock, someoneFolded)
                    }

                    is IrBlock -> {
                        val statements = expression.statements
                        val lastStatement = statements.last() as IrExpression
                        val foldedLastStatement = fold(lastStatement, coercion, cast, transformRecursively)
                        statements.transform {
                            if (it == lastStatement)
                                foldedLastStatement.expression
                            else
                                it.transformIfAsked()
                        }
                        val transformedBlock =
                                if (!foldedLastStatement.folded)
                                    expression
                                else with(expression) {
                                    IrBlockImpl(
                                            startOffset = startOffset,
                                            endOffset   = endOffset,
                                            type        = coercion.type,
                                            origin      = origin,
                                            statements  = statements)
                                }
                        PossiblyFoldedExpression(transformedBlock, foldedLastStatement.folded)
                    }

                    is IrWhen -> {
                        val foldedBranches = expression.branches.map { fold(it.result, coercion, cast, transformRecursively) }
                        val someoneFolded = foldedBranches.any { it.folded }
                        val transformedWhen = with(expression) {
                            IrWhenImpl(startOffset, endOffset, if (someoneFolded) coercion.type else type, origin,
                                    branches.asSequence().withIndex().map { (index, branch) ->
                                        IrBranchImpl(
                                                startOffset = branch.startOffset,
                                                endOffset   = branch.endOffset,
                                                condition   = branch.condition.transformIfAsked(),
                                                result      = if (someoneFolded)
                                                                  foldedBranches[index].getFullExpression(coercion, cast)
                                                              else foldedBranches[index].expression)
                                    }.toList())
                        }
                        return PossiblyFoldedExpression(transformedWhen, someoneFolded)
                    }

                    is IrTypeOperatorCall ->
                        if (expression.operator != IrTypeOperator.CAST
                                && expression.operator != IrTypeOperator.IMPLICIT_CAST
                                && expression.operator != IrTypeOperator.SAFE_CAST)
                            PossiblyFoldedExpression(expression.transformIfAsked(), false)
                        else {
                            if (expression.typeOperand.getInlinedClassNative() != coercionDeclaringClass)
                                PossiblyFoldedExpression(expression.transformIfAsked(), false)
                            else {
                                val foldedArgument = fold(expression.argument, coercion, expression, transformRecursively)
                                if (foldedArgument.folded)
                                    foldedArgument
                                else
                                    PossiblyFoldedExpression(expression.apply { argument = foldedArgument.expression }, false)
                            }
                        }

                    else -> PossiblyFoldedExpression(expression.transformIfAsked(), false)
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (!expression.isBoxOrUnboxCall())
                    return super.visitCall(expression)

                val argument = expression.getArguments().single().second
                val foldedArgument = fold(
                        expression           = argument,
                        coercion             = expression,
                        cast                 = null,
                        transformRecursively = true)
                return foldedArgument.getFullExpression(expression, null)
            }
        })
    }
}
