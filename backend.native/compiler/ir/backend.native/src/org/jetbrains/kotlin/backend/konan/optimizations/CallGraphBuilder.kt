package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.DirectedGraph
import org.jetbrains.kotlin.backend.konan.DirectedGraphNode
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.llvm.findMainEntryPoint
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

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

    class CallSite(val call: DataFlowIR.Node.Call, val actualCallee: DataFlowIR.FunctionSymbol)

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
                                val externalModulesDFG: ExternalModulesDFG) {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private val hasMain = context.config.configuration.get(KonanConfigKeys.PRODUCE) == CompilerOutputKind.PROGRAM

    private val symbolTable = moduleDFG.symbolTable

    private fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
        if (this is DataFlowIR.Type.Declared) return this
        val hash = (this as DataFlowIR.Type.External).hash
        return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $hash")
    }

    private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
        if (this is DataFlowIR.FunctionSymbol.External)
            return externalModulesDFG.publicFunctions[this.hash] ?: this
        return this
    }

    private fun DataFlowIR.Type.Declared.isSubtypeOf(other: DataFlowIR.Type.Declared): Boolean {
        return this == other || this.superTypes.any { it.resolved().isSubtypeOf(other) }
    }

    private val directEdges = mutableMapOf<DataFlowIR.FunctionSymbol, CallGraphNode>()
    private val reversedEdges = mutableMapOf<DataFlowIR.FunctionSymbol, MutableList<DataFlowIR.FunctionSymbol>>()
    private val callGraph = CallGraph(directEdges, reversedEdges)

    fun build(): CallGraph {
        val rootSet = if (hasMain) {
            listOf(symbolTable.mapFunction(findMainEntryPoint(context)!!).resolved()) +
                    moduleDFG.functions
                            .filter { it.value.isGlobalInitializer }
                            .map { it.key }
        } else {
            moduleDFG.functions.keys.filterIsInstance<DataFlowIR.FunctionSymbol.Public>()
        }
        @Suppress("LoopToCallChain")
        for (symbol in rootSet) {
            if (!directEdges.containsKey(symbol))
                dfs(symbol)
        }
        return callGraph
    }

    private fun dfs(symbol: DataFlowIR.FunctionSymbol) {
        val node = CallGraphNode(callGraph, symbol)
        directEdges.put(symbol, node)
        val list = mutableListOf<DataFlowIR.FunctionSymbol>()
        reversedEdges.put(symbol, list)
        val function = moduleDFG.functions[symbol] ?: externalModulesDFG.functionDFGs[symbol]
        val body = function!!.body
        body.nodes.filterIsInstance<DataFlowIR.Node.Call>()
                .forEach {
                    val callee = it.callee.resolved()
                    callGraph.addEdge(symbol, CallGraphNode.CallSite(it, callee))
                    if (callee is DataFlowIR.FunctionSymbol.Declared
                            && it !is DataFlowIR.Node.VirtualCall
                            && !directEdges.containsKey(callee))
                        dfs(callee)
                }
    }
}