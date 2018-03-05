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

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.ir.IrPrivateClassReferenceImpl
import org.jetbrains.kotlin.backend.konan.ir.IrPrivateFunctionCallImpl
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.nullConst
import org.jetbrains.kotlin.backend.konan.objcexport.getErasedTypeClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.getValueArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
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
                    moduleDFG.functions.keys.filterIsInstance<DataFlowIR.FunctionSymbol.Public>() +
                    moduleDFG.symbolTable.classMap.values
                            .filterIsInstance<DataFlowIR.Type.Declared>()
                            .flatMap { it.vtable + it.itable.values }
                            .filterIsInstance<DataFlowIR.FunctionSymbol.Declared>()
                            .filter { moduleDFG.functions.containsKey(it) }
        // TODO: Are globals inititalizers always called whether they are actually reachable from roots or not?
        val globalInitializers =
                moduleDFG.functions.keys.filter { it.isGlobalInitializer } +
                externalModulesDFG.functionDFGs.keys.filter { it.isGlobalInitializer }

        return (exportedFunctions + globalInitializers).distinct()

    }

    fun BitSet.format(allTypes: List<DataFlowIR.Type.Declared>): String {
        return allTypes.withIndex().filter { this[it.index] }.joinToString { it.value.toString() }
    }

    private val VIRTUAL_TYPE_ID = 0 // Id of [DataFlowIR.Type.Virtual].

    private class DevirtualizationAnalysis(val context: Context,
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

        class VirtualCallSiteReceivers(val receiver: Node, val caller: DataFlowIR.FunctionSymbol, val devirtualizedCallees: List<DevirtualizedCallee>)

        class ConstraintGraph : DirectedGraph<Node, Node> {

            private var nodesCount = 0

            override val nodes = mutableListOf<Node>()
            override fun get(key: Node) = key

            val voidNode = addNode { Node.Ordinary(it, { "Void" }) }
            val virtualNode = addNode { Node.Source(it, VIRTUAL_TYPE_ID, { "Virtual" }) }
            val arrayItemField = DataFlowIR.Field(null, 1, "Array\$Item")
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

        private inner class TypeHierarchy(types: List<DataFlowIR.Type.Declared>) {
            private val typesSubTypes = mutableMapOf<DataFlowIR.Type.Declared, MutableList<DataFlowIR.Type.Declared>>()

            init {
                val visited = mutableSetOf<DataFlowIR.Type.Declared>()

                fun processType(type: DataFlowIR.Type.Declared) {
                    if (type == DataFlowIR.Type.Virtual) return
                    if (!visited.add(type)) return
                    type.superTypes
                            .map { it.resolved() }
                            .forEach { superType ->
                                val subTypes = typesSubTypes.getOrPut(superType, { mutableListOf() })
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
                    it.parameterTypes
                            .map { it.resolved() }
                            .filter { it.isFinal }
                            .forEach { addInstantiatingClass(it) }
                }
                if (entryPoint == null) {
                    // For library assume all public non-abstract classes could be instantiated.
                    moduleDFG.symbolTable.classMap.values
                            .asSequence()
                            .filterIsInstance<DataFlowIR.Type.Public>()
                            .filter { !it.isAbstract }
                            .forEach { addInstantiatingClass(it) }
                }
                // Traverse call graph from the roots.
                rootSet.forEach { dfs(it) }
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
                dfs(callee)
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

            private fun dfs(symbol: DataFlowIR.FunctionSymbol) {
                val resolvedFunctionSymbol = symbol.resolved()
                if (resolvedFunctionSymbol is DataFlowIR.FunctionSymbol.External) {

                    DEBUG_OUTPUT(1) { println("Function $resolvedFunctionSymbol is external") }

                    val resolvedReturnType = symbol.returnType.resolved()
                    if (resolvedReturnType.isFinal) {

                        DEBUG_OUTPUT(1) { println("Adding return type as it is final") }

                        addInstantiatingClass(resolvedReturnType)
                    }
                    return
                }
                if (!visited.add(resolvedFunctionSymbol)) return

                DEBUG_OUTPUT(1) { println("Visiting $resolvedFunctionSymbol") }

                val function = (moduleDFG.functions[resolvedFunctionSymbol] ?: externalModulesDFG.functionDFGs[resolvedFunctionSymbol])!!

                DEBUG_OUTPUT(1) { function.debugOutput() }

                nodeLoop@for (node in function.body.nodes) {
                    when (node) {
                        is DataFlowIR.Node.NewObject -> {
                            addInstantiatingClass(node.constructedType)
                            dfs(node.callee)
                        }

                        is DataFlowIR.Node.Singleton -> {
                            addInstantiatingClass(node.type)
                            node.constructor?.let { dfs(it) }
                        }

                        is DataFlowIR.Node.Const -> addInstantiatingClass(node.type)

                        is DataFlowIR.Node.StaticCall ->
                            dfs(node.callee)

                        is DataFlowIR.Node.VirtualCall -> {
                            if (node.receiverType == DataFlowIR.Type.Virtual)
                                continue@nodeLoop
                            val receiverType = node.receiverType.resolved()
                            val vCallReturnType = node.callee.returnType.resolved()

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

                            typesVirtualCallSites.getOrPut(receiverType, { mutableListOf() }).add(node)
                            typeHierarchy.inheritorsOf(receiverType)
                                    .filter { instantiatingClasses.contains(it) }
                                    .forEach { processVirtualCall(node, it) }
                        }
                    }
                }
            }
        }

        fun BitSet.copy() = BitSet(this.size()).apply { this.or(this@copy) }

        fun analyze(): Map<DataFlowIR.Node.VirtualCall, DevirtualizedCallSite> {
            val functions = moduleDFG.functions + externalModulesDFG.functionDFGs
            val typeHierarchy = TypeHierarchy(symbolTable.classMap.values.filterIsInstance<DataFlowIR.Type.Declared>() +
                                              externalModulesDFG.allTypes)
            val rootSet = computeRootSet(context, moduleDFG, externalModulesDFG)

            val instantiatingClasses =
                    InstantiationsSearcher(rootSet, typeHierarchy).search()
                            .withIndex()
                            .associate { it.value to (it.index + 1 /* 0 is reserved for [DataFlowIR.Type.Virtual] */) }
            val allTypes = listOf(DataFlowIR.Type.Virtual) + instantiatingClasses.asSequence().sortedBy { it.value }.map { it.key }

            val nodesMap = mutableMapOf<DataFlowIR.Node, Node>()
            val constraintGraphBuilder = ConstraintGraphBuilder(nodesMap, functions, typeHierarchy, instantiatingClasses, allTypes, rootSet)
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
                    assert(it.reversedEdges?.isEmpty() ?: true, { "A source node #${it.id} has incoming edges" })
                    assert(it.reversedCastEdges?.isEmpty() ?: true, { "A source node #${it.id} has incoming edges" })
                }
            }

            DEBUG_OUTPUT(0) {
                println("CONSTRAINT GRAPH: ${constraintGraph.nodes.size} nodes, " +
                    "${constraintGraph.nodes.sumBy { (it.directEdges?.size ?: 0) + (it.directCastEdges?.size ?: 0) } } edges")
            }

            val topologicalOrder = DirectedGraphCondensationBuilder(constraintGraph).build().topologicalOrder

            DEBUG_OUTPUT(0) {
                println("CONDENSATION")
                topologicalOrder.forEachIndexed { index, multiNode ->
                    println("    MULTI-NODE #$index")
                    multiNode.nodes.forEach {
                        println("        #${it.id}: ${it.toString(allTypes)}")
                    }
                }
            }

            topologicalOrder.forEachIndexed { index, multiNode -> multiNode.nodes.forEach { it.priority = index } }

            // Handle all 'right-directed' edges.
            // TODO: this is pessimistic handling of [DataFlowIR.Type.Virtual], think how to do it better.
            for (multiNode in topologicalOrder) {
                if (multiNode.nodes.size == 1 && multiNode.nodes.first() is Node.Source)
                    continue // A source has no incoming edges.
                val types = BitSet()
                for (node in multiNode.nodes) {
                    node.reversedEdges?.forEach { types.or(it.types) }
                    node.reversedCastEdges
                            ?.filter { it.node.priority < node.priority } // Doesn't contradict topological order.
                            ?.forEach {
                                val sourceTypes = it.node.types.copy()
                                sourceTypes.and(it.suitableTypes)
                                types.or(sourceTypes)
                            }
                }
                for (node in multiNode.nodes)
                    node.types.or(types)
            }
            val badEdges = mutableListOf<Pair<Node, Node.CastEdge>>()
            for (node in constraintGraph.nodes) {
                node.directCastEdges
                        ?.filter { it.node.priority < node.priority } // Contradicts topological order.
                        ?.forEach { badEdges += node to it }
            }
            badEdges.sortBy { it.second.node.priority } // Heuristic.

            do {
                fun propagateTypes(node: Node, types: BitSet) {
                    node.types.or(types)
                    node.directEdges?.forEach { edge ->
                        val missingTypes = types.copy().apply { andNot(edge.types) }
                        if (!missingTypes.isEmpty)
                            propagateTypes(edge, missingTypes)
                    }
                    node.directCastEdges?.forEach { castEdge ->
                        val missingTypes = types.copy().apply { andNot(castEdge.node.types) }
                        missingTypes.and(castEdge.suitableTypes)
                        if (!missingTypes.isEmpty)
                            propagateTypes(castEdge.node, missingTypes)
                    }
                }

                var end = true
                for ((sourceNode, edge) in badEdges) {
                    val distNode = edge.node
                    val missingTypes = sourceNode.types.copy().apply { andNot(distNode.types) }
                    missingTypes.and(edge.suitableTypes)
                    if (!missingTypes.isEmpty) {
                        end = false
                        propagateTypes(distNode, missingTypes)
                    }
                }
            } while (!end)

            DEBUG_OUTPUT(0) {
                topologicalOrder.forEachIndexed { index, multiNode ->
                    println("Types of multi-node #$index")
                    for (node in multiNode.nodes) {
                        println("    Node #${node.id}")
                        allTypes.withIndex()
                                .filter { node.types[it.index] }
                                .forEach { println("        ${it.value}") }
                    }
                }
            }

            val result = mutableMapOf<DataFlowIR.Node.VirtualCall, Pair<DevirtualizedCallSite, DataFlowIR.FunctionSymbol>>()
            val nothing = symbolTable.mapClass(context.ir.symbols.nothing.owner)
            functions.values
                    .asSequence()
                    .filter { constraintGraph.functions.containsKey(it.symbol) }
                    .flatMap { it.body.nodes.asSequence() }
                    .filterIsInstance<DataFlowIR.Node.VirtualCall>()
                    .forEach { virtualCall ->
                        assert (nodesMap[virtualCall] != null, { "Node for virtual call $virtualCall has not been built" })
                        val virtualCallSiteReceivers = constraintGraph.virtualCallSiteReceivers[virtualCall]
                        if (virtualCallSiteReceivers == null || virtualCallSiteReceivers.receiver.types[VIRTUAL_TYPE_ID]) {

                            DEBUG_OUTPUT(0) {
                                println("Unable to devirtualize callsite ${virtualCall.irCallSite?.let { ir2stringWhole(it) } ?: virtualCall.toString() }")
                                println("    receiver is Virtual")
                            }

                            return@forEach
                        }
                        val possibleReceivers = allTypes.withIndex()
                                .filter { virtualCallSiteReceivers.receiver.types[it.index] }
                                .map { it.value }
                                .filter { it != nothing }
                        val map = virtualCallSiteReceivers.devirtualizedCallees.associateBy({ it.receiverType }, { it })
                        result[virtualCall] = DevirtualizedCallSite(virtualCall.callee.resolved(), possibleReceivers.map { receiverType ->
                            assert (map[receiverType] != null) {
                                "Non-expected receiver type $receiverType at call site: " +
                                        (virtualCall.irCallSite?.let { ir2stringWhole(it) } ?: virtualCall.toString())
                            }
                            val devirtualizedCallee = map[receiverType]!!
                            val callee = devirtualizedCallee.callee
                            if (callee is DataFlowIR.FunctionSymbol.Declared && callee.symbolTableIndex < 0)
                                error("Function ${devirtualizedCallee.receiverType}.$callee cannot be called virtually," +
                                        " but actually is at call site: ${virtualCall.irCallSite?.let { ir2stringWhole(it) } ?: virtualCall.toString() }")
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

            return result.asSequence().associateBy({ it.key }, { it.value.first })
        }

        private inner class ConstraintGraphBuilder(val functionNodesMap: MutableMap<DataFlowIR.Node, Node>,
                                                   val functions: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>,
                                                   val typeHierarchy: TypeHierarchy,
                                                   val instantiatingClasses: Map<DataFlowIR.Type.Declared, Int>,
                                                   val allTypes: List<DataFlowIR.Type.Declared>,
                                                   val rootSet: List<DataFlowIR.FunctionSymbol>) {

            private val variables = mutableMapOf<DataFlowIR.Node.Variable, Node>()

            private fun concreteType(type: DataFlowIR.Type.Declared): Int {
                assert(!(type.isAbstract && type.isFinal)) { "Incorrect type: $type" }
                return if (type.isAbstract) VIRTUAL_TYPE_ID else { instantiatingClasses[type] ?: error("Type $type is not instantiated") }
            }

            private fun ordinaryNode(nameBuilder: () -> String) =
                    constraintGraph.addNode { Node.Ordinary(it, nameBuilder) }

            private fun sourceNode(typeId: Int, nameBuilder: () -> String) =
                    constraintGraph.addNode { Node.Source(it, typeId, nameBuilder) }

            private fun concreteClass(type: DataFlowIR.Type.Declared) =
                    constraintGraph.concreteClasses.getOrPut(type) { sourceNode(concreteType(type)) { "Class\$$type" } }

            private fun fieldNode(field: DataFlowIR.Field) =
                    constraintGraph.fields.getOrPut(field) { ordinaryNode { "Field\$$field" } }

            private var stack = mutableListOf<DataFlowIR.FunctionSymbol>()

            fun build() {
                rootSet.forEach { createFunctionConstraintGraph(it, true)!! }
                while (stack.isNotEmpty()) {
                    val symbol = stack.pop()
                    val function = functions[symbol] ?: error("Unknown function: $symbol")
                    val body = function.body
                    val functionConstraintGraph = constraintGraph.functions[symbol]!!

                    body.nodes.forEach { dfgNodeToConstraintNode(functionConstraintGraph, it) }
                    functionNodesMap[body.returns]!!.addEdge(functionConstraintGraph.returns)
                    functionNodesMap[body.throws]!!.addEdge(functionConstraintGraph.throws)


                    DEBUG_OUTPUT(0) {
                        println("CONSTRAINT GRAPH FOR $symbol")
                        val ids = function.body.nodes.withIndex().associateBy({ it.value }, { it.index })
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

                val parameters = Array(symbol.parameterTypes.size) { ordinaryNode { "Param#$it\$$symbol" } }
                if (isRoot) {
                    // Exported function from the current module.
                    symbol.parameterTypes.forEachIndexed { index, type ->
                        val resolvedType = type.resolved()
                        val node = if (!resolvedType.isFinal)
                                       constraintGraph.virtualNode
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

            private fun edgeToConstraintNode(function: Function,
                                             edge: DataFlowIR.Edge): Node {
                val result = dfgNodeToConstraintNode(function, edge.node)
                val castToType = edge.castToType?.resolved() ?: return result
                val castNode = ordinaryNode { "Cast\$${function.symbol}" }
                val castEdge = createCastEdge(castNode, castToType)
                result.addCastEdge(castEdge)
                return castNode
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

                fun doCall(callee: Function, arguments: List<Any>): Node {
                    assert(callee.parameters.size == arguments.size) {
                        "Function ${callee.symbol} takes ${callee.parameters.size} but caller ${function.symbol} provided ${arguments.size}"
                    }
                    callee.parameters.forEachIndexed { index, parameter ->
                        val argument = argumentToConstraintNode(arguments[index])
                        argument.addEdge(parameter)
                    }
                    return callee.returns
                }

                fun doCall(callee: DataFlowIR.FunctionSymbol,
                           arguments: List<Any>,
                           returnType: DataFlowIR.Type.Declared,
                           receiverType: DataFlowIR.Type.Declared?): Node {
                    val resolvedCallee = callee.resolved()
                    val calleeConstraintGraph = createFunctionConstraintGraph(resolvedCallee, false)
                    return if (calleeConstraintGraph == null) {
                        constraintGraph.externalFunctions.getOrPut(resolvedCallee) {
                            val fictitiousReturnNode = ordinaryNode { "External$resolvedCallee" }
                            if (returnType.isFinal)
                                concreteClass(returnType).addEdge(fictitiousReturnNode)
                            else
                                constraintGraph.virtualNode.addEdge(fictitiousReturnNode)
                            fictitiousReturnNode
                        }
                    } else {
                        calleeConstraintGraph.throws.addEdge(function.throws)
                        if (receiverType == null)
                            doCall(calleeConstraintGraph, arguments)
                        else {
                            val receiverNode = argumentToConstraintNode(arguments[0])
                            val castedReceiver = ordinaryNode { "CastedReceiver\$${function.symbol}" }
                            val castedEdge = createCastEdge(castedReceiver, receiverType)
                            receiverNode.addCastEdge(castedEdge)
                            doCall(calleeConstraintGraph, listOf(castedReceiver) + arguments.drop(1))
                        }
                    }
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

                        is DataFlowIR.Node.Parameter ->
                            function.parameters[node.index]

                        is DataFlowIR.Node.StaticCall ->
                            doCall(node.callee, node.arguments, node.callee.returnType.resolved(), node.receiverType?.resolved())

                        is DataFlowIR.Node.NewObject -> {
                            val returnType = node.constructedType.resolved()
                            val instanceNode = concreteClass(returnType)
                            doCall(node.callee, listOf(instanceNode) + node.arguments, returnType, null)
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

                            val returnType = node.callee.returnType.resolved()
                            val receiverNode = edgeToConstraintNode(node.arguments[0])
                            if (receiverType == DataFlowIR.Type.Virtual)
                                constraintGraph.virtualNode.addEdge(receiverNode)
                            val castedReceiver = ordinaryNode { "CastedReceiver\$${function.symbol}" }
                            val castedEdge = createCastEdge(castedReceiver, receiverType)
                            receiverNode.addCastEdge(castedEdge)
                            val arguments = listOf(castedReceiver) + node.arguments.drop(1)

                            val returnsNode = ordinaryNode { "VirtualCallReturns\$${function.symbol}" }
                            callees.forEachIndexed { index, actualCallee ->
                                doCall(actualCallee, arguments, returnType, possibleReceiverTypes[index]).addEdge(returnsNode)
                            }
                            // Add cast to [Virtual] edge from receiver to returns, if return type is not final.
                            // With this we're reflecting the fact that unknown function can return anything.
                            val virtualTypeFilter = BitSet().apply { set(VIRTUAL_TYPE_ID) }
                            if (!returnType.isFinal) {
                                receiverNode.addCastEdge(Node.CastEdge(returnsNode, virtualTypeFilter))
                            }
                            // And throw anything.
                            receiverNode.addCastEdge(Node.CastEdge(function.throws, virtualTypeFilter))
                            // And write to some array anything. TODO: This is conservative.
                            receiverNode.addCastEdge(Node.CastEdge(fieldNode(constraintGraph.arrayItemField), virtualTypeFilter))

                            if (callees.isEmpty() && returnType.isFinal && entryPoint == null) {
                                // If we are in a library and facing final return type with no possible callees -
                                // this type still can be returned by some user of this library, so propagate it explicitly.
                                concreteClass(returnType).addEdge(returnsNode)
                            }

                            val devirtualizedCallees = possibleReceiverTypes.mapIndexed { index, possibleReceiverType ->
                                DevirtualizedCallee(possibleReceiverType, callees[index])
                            }
                            constraintGraph.virtualCallSiteReceivers[node] = VirtualCallSiteReceivers(castedReceiver, function.symbol, devirtualizedCallees)
                            returnsNode
                        }

                        is DataFlowIR.Node.Singleton -> {
                            val type = node.type.resolved()
                            val instanceNode = concreteClass(type)
                            node.constructor?.let { doCall(it, listOf(instanceNode), type, null) }
                            instanceNode
                        }

                        is DataFlowIR.Node.FieldRead ->
                            fieldNode(node.field)

                        is DataFlowIR.Node.FieldWrite -> {
                            val fieldNode = fieldNode(node.field)
                            edgeToConstraintNode(node.value).addEdge(fieldNode)
                            constraintGraph.voidNode
                        }

                        is DataFlowIR.Node.ArrayRead ->
                            fieldNode(constraintGraph.arrayItemField)

                        is DataFlowIR.Node.ArrayWrite -> {
                            val fieldNode = fieldNode(constraintGraph.arrayItemField)
                            edgeToConstraintNode(node.value).addEdge(fieldNode)
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

    class AnalysisResult(val devirtualizedCallSites: Map<DataFlowIR.Node.VirtualCall, DevirtualizedCallSite>)

    fun run(irModule: IrModuleFragment, context: Context, moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG)
            : AnalysisResult {
        val devirtualizationAnalysisResult = DevirtualizationAnalysis(context, moduleDFG, externalModulesDFG).analyze()
        val devirtualizedCallSites =
                devirtualizationAnalysisResult
                        .asSequence()
                        .filter { it.key.irCallSite != null }
                        .associate { it.key.irCallSite!! to it.value }
        Devirtualization.devirtualize(irModule, context, devirtualizedCallSites)
        return AnalysisResult(devirtualizationAnalysisResult)
    }

    private fun devirtualize(irModule: IrModuleFragment, context: Context,
                             devirtualizedCallSites: Map<IrCall, DevirtualizedCallSite>) {
        val nativePtrType = context.builtIns.nativePtr.defaultType
        val nativePtrEqualityOperatorSymbol = context.ir.symbols.areEqualByValue.single { it.descriptor.valueParameters[0].type == nativePtrType }
        val optimize = context.shouldOptimize()

        irModule.transformChildrenVoid(object: IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val devirtualizedCallSite = devirtualizedCallSites[expression]
                val possibleCallees = devirtualizedCallSite?.possibleCallees
                if (possibleCallees == null
                        || possibleCallees.any { it.callee is DataFlowIR.FunctionSymbol.External }
                        || possibleCallees.any { it.receiverType is DataFlowIR.Type.External })
                    return expression

                val descriptor = expression.descriptor
                val owner = (descriptor.containingDeclaration as ClassDescriptor)
                val maxUnfoldFactor = if (owner.isInterface) 3 else 1
                if (possibleCallees.size > maxUnfoldFactor) {
                    // Callsite too complicated to devirtualize.
                    return expression
                }

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val type = if (descriptor.isSuspend)
                               context.builtIns.nullableAnyType
                           else descriptor.original.returnType!!
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val dispatchReceiver = expression.dispatchReceiver!!
                    return when {
                        possibleCallees.isEmpty() -> irBlock(expression) {
                            +irCall(context.ir.symbols.throwInvalidReceiverTypeException).apply {
                                putValueArgument(0, irCall(context.ir.symbols.kClassImplConstructor, listOf(dispatchReceiver.type)).apply {
                                    putValueArgument(0, irCall(context.ir.symbols.getObjectTypeInfo).apply {
                                        putValueArgument(0, dispatchReceiver)
                                    })
                                })
                            }
                            +nullConst(expression, type)
                        }

                        optimize && possibleCallees.size == 1 -> { // Monomorphic callsite.
                            val actualCallee = possibleCallees[0].callee as DataFlowIR.FunctionSymbol.Declared
                            irDevirtualizedCall(expression, type, actualCallee).apply {
                                this.dispatchReceiver = dispatchReceiver
                                this.extensionReceiver = expression.extensionReceiver
                                expression.descriptor.valueParameters.forEach {
                                    this.putValueArgument(it.index, expression.getValueArgument(it))
                                }
                            }
                        }

                        else -> irBlock(expression) {
                            val receiver = irTemporary(dispatchReceiver, "receiver")
                            val extensionReceiver = expression.extensionReceiver?.let { irTemporary(it, "extensionReceiver") }
                            val parameters = expression.descriptor.valueParameters.associate { it to irTemporary(expression.getValueArgument(it)!!) }
                            val typeInfo = irTemporary(irCall(context.ir.symbols.getObjectTypeInfo).apply {
                                putValueArgument(0, irGet(receiver.symbol))
                            })

                            val branches = mutableListOf<IrBranchImpl>()
                            possibleCallees.mapIndexedTo(branches) { index, devirtualizedCallee ->
                                val actualCallee = devirtualizedCallee.callee as DataFlowIR.FunctionSymbol.Declared
                                val actualReceiverType = devirtualizedCallee.receiverType as DataFlowIR.Type.Declared
                                val expectedTypeInfo = IrPrivateClassReferenceImpl(
                                        startOffset      = startOffset,
                                        endOffset        = endOffset,
                                        type             = nativePtrType,
                                        symbol           = IrClassSymbolImpl(dispatchReceiver.type.getErasedTypeClass()),
                                        classType        = receiver.type,
                                        moduleDescriptor = actualReceiverType.module!!.descriptor,
                                        totalClasses     = actualReceiverType.module.numberOfClasses,
                                        classIndex       = actualReceiverType.symbolTableIndex)
                                val condition =
                                        if (optimize && index == possibleCallees.size - 1)
                                            irTrue() // Don't check last type in optimize mode.
                                        else
                                            irCall(nativePtrEqualityOperatorSymbol).apply {
                                                putValueArgument(0, irGet(typeInfo.symbol))
                                                putValueArgument(1, expectedTypeInfo)
                                            }
                                IrBranchImpl(
                                        startOffset = startOffset,
                                        endOffset   = endOffset,
                                        condition   = condition,
                                        result      = irDevirtualizedCall(expression, type, actualCallee).apply {
                                            this.dispatchReceiver  = irGet(receiver.symbol)
                                            this.extensionReceiver = extensionReceiver?.let { irGet(it.symbol) }
                                            expression.descriptor.valueParameters.forEach {
                                                putValueArgument(it.index, irGet(parameters[it]!!.symbol))
                                            }
                                        }
                                )
                            }
                            if (!optimize) { // Add else branch throwing exception for debug purposes.
                                branches.add(IrBranchImpl(
                                        startOffset = startOffset,
                                        endOffset   = endOffset,
                                        condition   = irTrue(),
                                        result      = irCall(context.ir.symbols.throwInvalidReceiverTypeException).apply {
                                            putValueArgument(0,
                                                    irCall(context.ir.symbols.kClassImplConstructor,
                                                            listOf(dispatchReceiver.type)
                                                    ).apply {
                                                        putValueArgument(0, irGet(typeInfo.symbol))
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

            fun IrBuilderWithScope.irDevirtualizedCall(callee: IrCall, actualType: KotlinType,
                                                       devirtualizedCallee: DataFlowIR.FunctionSymbol.Declared) =
                    IrPrivateFunctionCallImpl(
                            startOffset      = startOffset,
                            endOffset        = endOffset,
                            type             = actualType,
                            symbol           = callee.symbol,
                            descriptor       = callee.descriptor,
                            typeArgumentsCount = callee.typeArgumentsCount,
                            moduleDescriptor = devirtualizedCallee.module.descriptor,
                            totalFunctions   = devirtualizedCallee.module.numberOfFunctions,
                            functionIndex    = devirtualizedCallee.symbolTableIndex
                    ).apply {
                        copyTypeArgumentsFrom(callee)
                    }

        })
    }
}