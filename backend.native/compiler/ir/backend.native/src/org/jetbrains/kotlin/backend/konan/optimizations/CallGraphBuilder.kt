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
                                devirtualizationAnalysisResult: Devirtualization.AnalysisResult?,
                                val gotoExternal: Boolean) {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private val devirtualizedCallSites = devirtualizationAnalysisResult?.devirtualizedCallSites

    private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
        if (this is DataFlowIR.FunctionSymbol.External)
            return externalModulesDFG.publicFunctions[this.hash] ?: this
        return this
    }

    private val visitedFunctions = mutableSetOf<DataFlowIR.FunctionSymbol>()
    private val directEdges = mutableMapOf<DataFlowIR.FunctionSymbol, CallGraphNode>()
    private val reversedEdges = mutableMapOf<DataFlowIR.FunctionSymbol, MutableList<DataFlowIR.FunctionSymbol>>()
    private val callGraph = CallGraph(directEdges, reversedEdges)

    fun build(): CallGraph {
        val rootSet = Devirtualization.computeRootSet(context, moduleDFG, externalModulesDFG)
        @Suppress("LoopToCallChain")
        for (symbol in rootSet) {
            if (!visitedFunctions.contains(symbol))
                dfs(symbol)
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

    private fun dfs(symbol: DataFlowIR.FunctionSymbol) {
        visitedFunctions += symbol
        if (gotoExternal) {
            addNode(symbol)
            val function = moduleDFG.functions[symbol] ?: externalModulesDFG.functionDFGs[symbol]
            val body = function!!.body
            body.nodes.filterIsInstance<DataFlowIR.Node.Call>()
                    .forEach { call ->
                        val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.let { devirtualizedCallSites?.get(it) }
                        if (devirtualizedCallSite == null) {
                            val callee = call.callee.resolved()
                            callGraph.addEdge(symbol, CallGraphNode.CallSite(call, call is DataFlowIR.Node.VirtualCall, callee))
                            if (callee is DataFlowIR.FunctionSymbol.Declared
                                    && call !is DataFlowIR.Node.VirtualCall
                                    && !directEdges.containsKey(callee))
                                dfs(callee)
                        } else {
                            devirtualizedCallSite.possibleCallees.forEach {
                                val callee = it.callee.resolved()
                                callGraph.addEdge(symbol, CallGraphNode.CallSite(call, false, callee))
                                if (callee is DataFlowIR.FunctionSymbol.Declared
                                        && !directEdges.containsKey(callee))
                                    dfs(callee)
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
            body.nodes.filterIsInstance<DataFlowIR.Node.Call>()
                    .forEach { call ->
                        val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.let { devirtualizedCallSites?.get(it) }
                        if (devirtualizedCallSite == null) {
                            val callee = call.callee.resolved()
                            if (moduleDFG.functions.containsKey(callee))
                                addNode(callee)
                            if (local)
                                callGraph.addEdge(symbol, CallGraphNode.CallSite(call, call is DataFlowIR.Node.VirtualCall, callee))
                            if (callee is DataFlowIR.FunctionSymbol.Declared
                                    && call !is DataFlowIR.Node.VirtualCall
                                    && !visitedFunctions.contains(callee))
                                dfs(callee)
                        } else {
                            devirtualizedCallSite.possibleCallees.forEach {
                                val callee = it.callee.resolved()
                                if (moduleDFG.functions.containsKey(callee))
                                    addNode(callee)
                                if (local)
                                    callGraph.addEdge(symbol, CallGraphNode.CallSite(call, false, callee))
                                if (callee is DataFlowIR.FunctionSymbol.Declared
                                        && !visitedFunctions.contains(callee))
                                    dfs(callee)
                            }
                        }
                    }
        }
    }
}