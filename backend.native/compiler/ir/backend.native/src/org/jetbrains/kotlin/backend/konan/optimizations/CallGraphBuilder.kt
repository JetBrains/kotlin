/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.DirectedGraph
import org.jetbrains.kotlin.backend.konan.DirectedGraphNode
import org.jetbrains.kotlin.backend.konan.Context

internal class CallGraphNode(val graph: CallGraph, val symbol: DataFlowIR.FunctionSymbol)
    : DirectedGraphNode<DataFlowIR.FunctionSymbol> {

    override val key get() = symbol

    override val directEdges: List<DataFlowIR.FunctionSymbol> by lazy {
        graph.directEdges[symbol]!!.callSites
                .map { it.actualCallee }
                .filter { graph.reversedEdges.containsKey(it) }
    }

    override val reversedEdges: List<DataFlowIR.FunctionSymbol> by lazy {
        graph.reversedEdges[symbol]!!
    }

    class CallSite(val call: DataFlowIR.Node.Call, val isVirtual: Boolean, val actualCallee: DataFlowIR.FunctionSymbol)

    val callSites = mutableListOf<CallSite>()
}

internal class CallGraph(val directEdges: Map<DataFlowIR.FunctionSymbol, CallGraphNode>,
                         val reversedEdges: Map<DataFlowIR.FunctionSymbol, MutableList<DataFlowIR.FunctionSymbol>>)
    : DirectedGraph<DataFlowIR.FunctionSymbol, CallGraphNode> {

    override val nodes get() = directEdges.values

    override fun get(key: DataFlowIR.FunctionSymbol) = directEdges[key]!!

    fun addEdge(caller: DataFlowIR.FunctionSymbol, callSite: CallGraphNode.CallSite) {
        directEdges[caller]!!.callSites += callSite
        reversedEdges[callSite.actualCallee]?.add(caller)
    }

}

internal class CallGraphBuilder(val context: Context,
                                val moduleDFG: ModuleDFG,
                                val externalModulesDFG: ExternalModulesDFG,
                                val devirtualizationAnalysisResult: Devirtualization.AnalysisResult,
                                val gotoExternal: Boolean) {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private val devirtualizedCallSites = devirtualizationAnalysisResult.devirtualizedCallSites

    private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
        if (this is DataFlowIR.FunctionSymbol.External)
            return externalModulesDFG.publicFunctions[this.hash] ?: this
        return this
    }

    private val visitedFunctions = mutableSetOf<DataFlowIR.FunctionSymbol>()
    private val directEdges = mutableMapOf<DataFlowIR.FunctionSymbol, CallGraphNode>()
    private val reversedEdges = mutableMapOf<DataFlowIR.FunctionSymbol, MutableList<DataFlowIR.FunctionSymbol>>()
    private val callGraph = CallGraph(directEdges, reversedEdges)
    private val functionStack = mutableListOf<DataFlowIR.FunctionSymbol>()

    fun build(): CallGraph {
        val rootSet = Devirtualization.computeRootSet(context, moduleDFG, externalModulesDFG)
        @Suppress("LoopToCallChain")
        for (symbol in rootSet)
            functionStack.push(symbol)

        while (functionStack.isNotEmpty()) {
            val symbol = functionStack.pop()
            if (symbol !in visitedFunctions)
                handleFunction(symbol)
        }

        DEBUG_OUTPUT(0) {
            println("DirectEdges: ${directEdges.size}")
            println("ReversedEdges: ${reversedEdges.size}")
            println("Sum: ${directEdges.values.sumBy { it.callSites.size }}")
        }

        return callGraph
    }

    private fun addNode(symbol: DataFlowIR.FunctionSymbol) {
        if (directEdges.containsKey(symbol))
            return
        val node = CallGraphNode(callGraph, symbol)
        directEdges.put(symbol, node)
        val list = mutableListOf<DataFlowIR.FunctionSymbol>()
        reversedEdges.put(symbol, list)
    }

    private val symbols = context.ir.symbols
    private val arrayGet = symbols.arrayGet[symbols.array]!!.owner
    private val arraySet = symbols.arraySet[symbols.array]!!.owner

    private inline fun DataFlowIR.FunctionBody.forEachCallSite(block: (DataFlowIR.Node.Call) -> Unit) =
            nodes.forEach { node ->
                when (node) {
                    is DataFlowIR.Node.Call -> block(node)

                    is DataFlowIR.Node.Singleton ->
                        node.constructor?.let { block(DataFlowIR.Node.Call(it, emptyList(), node.type, null)) }

                    is DataFlowIR.Node.ArrayRead ->
                        block(DataFlowIR.Node.Call(
                                callee = node.callee,
                                arguments = listOf(node.array, node.index),
                                returnType = node.type,
                                irCallSite = null)
                        )

                    is DataFlowIR.Node.ArrayWrite ->
                        block(DataFlowIR.Node.Call(
                                callee = node.callee,
                                arguments = listOf(node.array, node.index, node.value),
                                returnType = moduleDFG.symbolTable.mapType(context.irBuiltIns.unitType),
                                irCallSite = null)
                        )

                    is DataFlowIR.Node.FunctionReference ->
                        block(DataFlowIR.Node.Call(
                                callee = node.symbol,
                                arguments = emptyList(),
                                returnType = node.symbol.returnParameter.type,
                                irCallSite = null
                        ))
                }
            }

    private fun staticCall(caller: DataFlowIR.FunctionSymbol, call: DataFlowIR.Node.Call, callee: DataFlowIR.FunctionSymbol) {
        callGraph.addEdge(caller, CallGraphNode.CallSite(call, false, callee))
        if (callee is DataFlowIR.FunctionSymbol.Declared
                && !directEdges.containsKey(callee))
            functionStack.push(callee)
    }

    private fun handleFunction(symbol: DataFlowIR.FunctionSymbol) {
        visitedFunctions += symbol
        if (gotoExternal) {
            addNode(symbol)
            val function = moduleDFG.functions[symbol] ?: externalModulesDFG.functionDFGs[symbol] ?: return
            val body = function.body
            body.forEachCallSite { call ->
                val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.let { devirtualizedCallSites[it] }
                when {
                    call !is DataFlowIR.Node.VirtualCall -> staticCall(symbol, call, call.callee.resolved())

                    devirtualizedCallSite != null -> {
                        devirtualizedCallSite.possibleCallees.forEach {
                            staticCall(symbol, call, it.callee.resolved())
                        }
                    }

                    call.receiverType == DataFlowIR.Type.Virtual -> {
                        // Skip callsite. This can only be for invocations Any's methods on instances of ObjC classes.
                    }

                    else -> {
                        // Callsite has not been devirtualized - conservatively assume the worst:
                        // any inheritor of the receiver type is possible here.
                        val typeHierarchy = devirtualizationAnalysisResult.typeHierarchy
                        typeHierarchy.inheritorsOf(call.receiverType as DataFlowIR.Type.Declared).forEachBit {
                            val receiverType = typeHierarchy.allTypes[it]
                            if (receiverType.isAbstract) return@forEachBit
                            // TODO: Unconservative way - when we can use it?
                            //.filter { devirtualizationAnalysisResult.instantiatingClasses.contains(it) }
                            val actualCallee = when (call) {
                                is DataFlowIR.Node.VtableCall ->
                                    receiverType.vtable[call.calleeVtableIndex]

                                is DataFlowIR.Node.ItableCall ->
                                    receiverType.itable[call.calleeHash]!!

                                else -> error("Unreachable")
                            }
                            staticCall(symbol, call, actualCallee.resolved())
                        }
                    }
                }
            }
        } else {
            var function = moduleDFG.functions[symbol]
            var local = true
            if (function != null)
                addNode(symbol)
            else {
                function = externalModulesDFG.functionDFGs[symbol]!!
                local = false
            }
            val body = function.body
            body.forEachCallSite { call ->
                val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.let { devirtualizedCallSites.get(it) }
                if (devirtualizedCallSite == null) {
                    val callee = call.callee.resolved()
                    if (moduleDFG.functions.containsKey(callee))
                        addNode(callee)
                    if (local)
                        callGraph.addEdge(symbol, CallGraphNode.CallSite(call, call is DataFlowIR.Node.VirtualCall, callee))
                    if (callee is DataFlowIR.FunctionSymbol.Declared
                            && call !is DataFlowIR.Node.VirtualCall
                            && !visitedFunctions.contains(callee))
                        functionStack.push(callee)
                } else {
                    devirtualizedCallSite.possibleCallees.forEach {
                        val callee = it.callee.resolved()
                        if (moduleDFG.functions.containsKey(callee))
                            addNode(callee)
                        if (local)
                            callGraph.addEdge(symbol, CallGraphNode.CallSite(call, false, callee))
                        if (callee is DataFlowIR.FunctionSymbol.Declared
                                && !visitedFunctions.contains(callee))
                            functionStack.push(callee)
                    }
                }
            }
        }
    }
}