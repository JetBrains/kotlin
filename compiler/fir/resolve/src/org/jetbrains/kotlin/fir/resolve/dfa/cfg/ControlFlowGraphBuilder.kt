/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.fir.util.listMultimapOf
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.random.Random

@RequiresOptIn
private annotation class CfgBuilderInternals

@OptIn(CfgInternals::class)
class ControlFlowGraphBuilder {
    @CfgBuilderInternals
    private val graphs: Stack<ControlFlowGraph> = stackOf(ControlFlowGraph(null, "<TOP_LEVEL_GRAPH>", ControlFlowGraph.Kind.TopLevel))

    @get:OptIn(CfgBuilderInternals::class)
    val currentGraph: ControlFlowGraph
        get() = graphs.top()

    private val lastNodes: Stack<CFGNode<*>> = stackOf()
    val lastNode: CFGNode<*>
        get() = lastNodes.top()

    val lastNodeOrNull: CFGNode<*>?
        get() = lastNodes.topOrNull()

    var levelCounter: Int = 0

    private val modes: Stack<Mode> = stackOf(Mode.TopLevel)
    private val mode: Mode get() = modes.top()

    private enum class Mode {
        Function, TopLevel, Body, ClassInitializer, PropertyInitializer, FieldInitializer
    }

    // ----------------------------------- Node caches -----------------------------------

    private val exitTargetsForReturn: SymbolBasedNodeStorage<FirFunction, FunctionExitNode> = SymbolBasedNodeStorage()
    private val exitTargetsForTry: Stack<CFGNode<*>> = stackOf()
    private val exitsOfAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, FunctionExitNode> = mutableMapOf()
    private val enterToLocalClassesMembers: MutableMap<FirBasedSymbol<*>, CFGNode<*>?> = mutableMapOf()

    //return jumps via finally blocks, target -> jumps
    private val nonDirectJumps: ListMultimap<CFGNode<*>, CFGNode<*>> = listMultimapOf()

    private val postponedAnonymousFunctionNodes =
        mutableMapOf<FirFunctionSymbol<*>, Pair<PostponedLambdaEnterNode, PostponedLambdaExitNode>>()
    private val dataFlowSourcesForNextCompletedCall: Stack<MutableList<CFGNode<*>>> = stackOf()

    private val loopConditionEnterNodes: NodeStorage<FirElement, LoopConditionEnterNode> = NodeStorage()
    private val loopExitNodes: NodeStorage<FirLoop, LoopExitNode> = NodeStorage()

    private val whenExitNodes: NodeStorage<FirWhenExpression, WhenExitNode> = NodeStorage()
    private val whenBranchIndices: Stack<Map<FirWhenBranch, Int>> = stackOf()

    private val binaryLogicExpressionExitNodes: Stack<AbstractBinaryExitNode<FirBinaryLogicExpression>> = stackOf()

    private val tryExitNodes: NodeStorage<FirTryExpression, TryExpressionExitNode> = NodeStorage()
    private val tryMainExitNodes: NodeStorage<FirTryExpression, TryMainBlockExitNode> = NodeStorage()
    private val catchNodeStorages: Stack<NodeStorage<FirCatch, CatchClauseEnterNode>> = stackOf()
    private val catchNodeStorage: NodeStorage<FirCatch, CatchClauseEnterNode> get() = catchNodeStorages.top()
    private val catchExitNodeStorages: Stack<NodeStorage<FirCatch, CatchClauseExitNode>> = stackOf()
    private val finallyEnterNodes: Stack<FinallyBlockEnterNode> = stackOf()
    private val finallyExitNodes: NodeStorage<FirTryExpression, FinallyBlockExitNode> = NodeStorage()

    private val initBlockExitNodes: Stack<InitBlockExitNode> = stackOf()

    private val exitSafeCallNodes: Stack<ExitSafeCallNode> = stackOf()
    private val exitElvisExpressionNodes: Stack<ElvisExitNode> = stackOf()
    private val elvisRhsEnterNodes: Stack<ElvisRhsEnterNode> = stackOf()

    private val notCompletedFunctionCalls: Stack<MutableList<FunctionCallNode>> = stackOf()

    // ----------------------------------- API for node builders -----------------------------------

    private var idCounter: Int = Random.nextInt()
    fun createId(): Int = idCounter++

    // ----------------------------------- Public API -----------------------------------

    fun isThereControlFlowInfoForAnonymousFunction(function: FirAnonymousFunction): Boolean =
        function.controlFlowGraphReference?.controlFlowGraph != null ||
                exitsOfAnonymousFunctions.containsKey(function.symbol)

    // This function might throw exception if !isThereControlFlowInfoForAnonymousFunction(function)
    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirStatement> {
        fun FirElement.extractArgument(): FirElement = when {
            this is FirReturnExpression && target.labeledElement.symbol == function.symbol -> result.extractArgument()
            else -> this
        }

        fun CFGNode<*>.extractArgument(): FirElement? = when (this) {
            is FunctionEnterNode, is TryMainBlockEnterNode, is FinallyBlockExitNode, is CatchClauseEnterNode -> null
            is BlockExitNode -> if (function.isLambda || isDead) firstPreviousNode.extractArgument() else null
            is StubNode -> firstPreviousNode.extractArgument()
            else -> fir.extractArgument()
        }

        val exitNode = function.controlFlowGraphReference?.controlFlowGraph?.exitNode
            ?: exitsOfAnonymousFunctions.getValue(function.symbol)
        val nonDirect = nonDirectJumps[exitNode]
        return (nonDirect + exitNode.previousNodes).mapNotNullTo(mutableSetOf()) {
            it.extractArgument() as FirStatement?
        }
    }

    @OptIn(CfgBuilderInternals::class)
    fun isTopLevel(): Boolean = graphs.size == 1

    // ----------------------------------- Utils -----------------------------------

    @OptIn(CfgBuilderInternals::class)
    private fun pushGraph(graph: ControlFlowGraph, mode: Mode) {
        graphs.push(graph)
        modes.push(mode)
        levelCounter++
    }

    @OptIn(CfgBuilderInternals::class)
    private fun popGraph(): ControlFlowGraph {
        levelCounter--
        modes.pop()
        return graphs.pop().also { it.complete() }
    }

    // ----------------------------------- Regular function -----------------------------------

    fun enterFunction(function: FirFunction): Pair<FunctionEnterNode, LocalFunctionDeclarationNode?> {
        require(function !is FirAnonymousFunction)
        val name = when (function) {
            is FirSimpleFunction -> function.name.asString()
            is FirPropertyAccessor -> if (function.isGetter) "<getter>" else "<setter>"
            is FirConstructor -> "<init>"
            else -> throw IllegalArgumentException("Unknown function: ${function.render()}")
        }
        val graph = ControlFlowGraph(function, name, ControlFlowGraph.Kind.Function)
        // function is local
        val localFunctionNode = runIf(mode == Mode.Body) {
            assert(currentGraph.kind.withBody)
            currentGraph.addSubGraph(graph)

            createLocalFunctionDeclarationNode(function).also {
                addNewSimpleNode(it)
            }
        }

        pushGraph(
            graph = graph,
            mode = Mode.Body
        )

        val previousNode = enterToLocalClassesMembers[function.symbol]
            ?: (function as? FirSimpleFunction)?.takeIf { it.isLocal }?.let { lastNode }

        val enterNode = createFunctionEnterNode(function).also {
            lastNodes.push(it)
        }

        if (previousNode != null) {
            if (localFunctionNode == previousNode) {
                addEdge(localFunctionNode, enterNode, preferredKind = EdgeKind.Forward)
            } else {
                addEdge(previousNode, enterNode, preferredKind = EdgeKind.DfgForward)
            }
        }

        createFunctionExitNode(function).also {
            exitTargetsForReturn.push(it)
            exitTargetsForTry.push(it)
        }

        return Pair(enterNode, localFunctionNode)
    }

    fun exitFunction(function: FirFunction): Pair<FunctionExitNode, ControlFlowGraph> {
        require(function !is FirAnonymousFunction)
        val exitNode = exitTargetsForReturn.pop()
        popAndAddEdge(exitNode)
        val graph = popGraph()
        assert(exitNode == graph.exitNode)
        exitTargetsForTry.pop().also {
            assert(it == graph.exitNode)
        }
        graph.exitNode.updateDeadStatus()
        return graph.exitNode as FunctionExitNode to graph
    }

    // ----------------------------------- Anonymous function -----------------------------------
    // TODO: this is ALL WRONG.
    //
    // There are two cases we need to distinguish.
    //
    //  1. Function calls can have contracts that specify lambdas as "called in place".
    //     This only works on lambdas DIRECTLY used as function arguments. So `f({ a })`,
    //     and not `f(if (p) { { a } } else { { b } })`.
    //
    //  2. Every other place where a lambda's type is context-dependent. In this case
    //     we can't analyze the lambda just yet, but that doesn't matter since the lambda
    //     is not "called in place" so neither control nor data flow will pass through it.
    //
    // So case 2 is simple: add a placeholder node, then when we analyze the lambda attach
    // it as a subgraph to that node. If that node is an argument to a function call,
    // it should only be placed after every other argument to have control flow pass through
    // them before entering the lambda.
    //
    // Case 1 is where the fun, for some definition of "fun", happens.
    //
    // In the basic case (completed call), control and data flow should look like this:
    //                 /---> [EXACTLY_ONCE] --\
    //                 |--> [AT_LEAST_ONCE] --|
    //                 |<-----------------/   |
    //    [arguments] -+    /------------v    +-> function call
    //                 |--> [AT_MOST_ONCE] ---|
    //                 |<--------------\      |
    //                 \-----> [UNKNOWN] -----/
    //                        \----------^
    // This is already problematic, since all these lambdas should share ONE entry node
    // to which control can return after AT_LEAST_ONCE and UNKNOWN lambdas. To make
    // matters worse, if the call is not complete, then the lambdas inside it *and*
    // other lambdas in whatever expression this is a part of can be resolved in any order,
    // so the data flow cannot look like this - until some call is finally completed, there
    // will be holes in the graph where some lambdas' subgraphs should be. Instead, we
    // should be collecting the lambdas as they are being resolved, filling gaps in the
    // control flow graph, and then redirecting the outgoing data flow edges into the next
    // completed call node. Unfortunately, I'm not sure how this fits into the subgraph
    // design. Also, touching the `PostponedLambda{Enter,Exit}Node`s in any way seem to
    // break random stuff that probably looks at `node.fir` with zero regard as to what
    // the node even means.
    fun visitPostponedAnonymousFunction(anonymousFunctionExpression: FirAnonymousFunctionExpression): Pair<PostponedLambdaEnterNode, PostponedLambdaExitNode> {
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        val enterNode = createPostponedLambdaEnterNode(anonymousFunction)
        val exitNode = createPostponedLambdaExitNode(anonymousFunctionExpression)
        val symbol = anonymousFunction.symbol
        postponedAnonymousFunctionNodes[symbol] = enterNode to exitNode
        addNewSimpleNode(enterNode)
        addNewSimpleNode(exitNode, preferredKind = EdgeKind.DfgForward)
        return enterNode to exitNode
    }

    fun enterAnonymousFunction(anonymousFunction: FirAnonymousFunction): Pair<PostponedLambdaEnterNode?, FunctionEnterNode> {
        val symbol = anonymousFunction.symbol
        val preparedEnterNode = postponedAnonymousFunctionNodes[symbol]?.first
        val outerEnterNode = preparedEnterNode ?: createPostponedLambdaEnterNode(anonymousFunction).also { addNewSimpleNode(it) }

        pushGraph(ControlFlowGraph(anonymousFunction, "<anonymous>", ControlFlowGraph.Kind.AnonymousFunction), Mode.Function)
        val enterNode = createFunctionEnterNode(anonymousFunction)
        val exitNode = createFunctionExitNode(anonymousFunction)
        exitsOfAnonymousFunctions[symbol] = exitNode
        exitTargetsForReturn.push(exitNode)
        if (!anonymousFunction.invocationKind.isInPlace) {
            exitTargetsForTry.push(exitNode)
        }

        addEdge(outerEnterNode, enterNode)
        lastNodes.push(enterNode)
        return outerEnterNode.takeIf { preparedEnterNode == null } to enterNode
    }

    fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): Triple<FunctionExitNode, PostponedLambdaExitNode?, ControlFlowGraph> {
        val symbol = anonymousFunction.symbol
        val exitNode = exitsOfAnonymousFunctions.remove(symbol)!!.also {
            require(it == exitTargetsForReturn.pop())
            if (!anonymousFunction.invocationKind.isInPlace) {
                require(it == exitTargetsForTry.pop())
            }
        }
        popAndAddEdge(exitNode)
        exitNode.updateDeadStatus()

        val graph = popGraph().also { graph ->
            assert(graph.declaration == anonymousFunction)
            assert(graph.exitNode == exitNode)
            // TODO: disregarding the edges is probably not correct, though this should never find any nodes anyway
            dataFlowSourcesForNextCompletedCall.topOrNull()?.removeAll { it.owner == graph }
        }

        val (postponedEnterNode, postponedExitNode) = postponedAnonymousFunctionNodes.remove(symbol)
            ?: return Triple(exitNode, null, graph).also { (lastNode as PostponedLambdaEnterNode).owner.addSubGraph(graph) }

        val invocationKind = anonymousFunction.invocationKind
        var changedExitDataFlow = false
        // Lambdas not called in-place behave as if called never, but with extra invalidation of all smart casts
        // for all variables that they reassign. That second part is handled by `FirDataFlowAnalyzer`.
        if (invocationKind?.isDefinitelyVisited() != true) {
            // Data flow from enter -> exit already exists, only need to also mark that edge as control flow.
            CFGNode.addJustKindEdge(postponedEnterNode, postponedExitNode, EdgeKind.CfgForward, propagateDeadness = true)
        }
        if (invocationKind?.canBeVisited() == true) {
            val currentCallsPostponedLambdas = dataFlowSourcesForNextCompletedCall.top()
            // Since the skipping edge goes to `postponedExitNode` rather than `exitNode`, if
            // we try to merge data flow for not-definitely-called lambdas from `exitNode` into the next call
            // we won't get a correct result. TODO: that seems hacky and points to the edges being wrong.
            if (invocationKind.isDefinitelyVisited()) {
                // When a call has lambda arguments that the function says it will call in place, control goes through
                // all other arguments, then through the lambdas in parallel, then out of the function call. This parallel
                // (in terms of ordering, not multi-threaded or w/e) execution is represented with the call being a union node.
                // TODO: currently this is only done for data flow; control flow incorrectly pretends lambdas
                //  are called in the order they are provided to the call, which is wrong. This needs to be fixed. Somehow.
                addEdge(exitNode, postponedExitNode, preferredKind = EdgeKind.CfgForward, propagateDeadness = true)
                currentCallsPostponedLambdas.add(postponedExitNode)
            } else {
                addEdge(exitNode, postponedExitNode, propagateDeadness = invocationKind.isDefinitelyVisited())
                changedExitDataFlow = true
            }
            if (invocationKind.canBeRevisited()) {
                addBackEdge(postponedExitNode, postponedEnterNode)
            }
        }

        postponedEnterNode.owner.addSubGraph(graph)
        return Triple(exitNode, postponedExitNode.takeIf { changedExitDataFlow }, graph)
    }

    fun exitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression): AnonymousFunctionExpressionExitNode {
        return createAnonymousFunctionExpressionExitNode(anonymousFunctionExpression).also {
            addNewSimpleNode(it)
        }
    }

    private fun splitDataFlowForPostponedLambdas() {
        dataFlowSourcesForNextCompletedCall.push(mutableListOf())
    }

    private fun MutableList<CFGNode<*>>.addPostponedLambdaDataFlowEdgesTo(node: CFGNode<*>) {
        for (exitNode in this) {
            // To avoid storing nodes from subgraphs in the list, we have PostponedLambdaExitNode instead of the real
            // exit node of the lambda subgraph. The latter is the previous node of the former. Everything else is
            // already a join node in this graph.
            val functionExitOrMerge = if (exitNode is PostponedLambdaExitNode) exitNode.lastPreviousNode else exitNode
            addEdge(functionExitOrMerge, node, preferredKind = EdgeKind.DfgForward, propagateDeadness = false)
        }
    }

    private fun <T> unifyDataFlowFromPostponedLambdas(node: T, callCompleted: Boolean) where T : CFGNode<*>, T : UnionNodeMarker {
        val currentLevelExits = dataFlowSourcesForNextCompletedCall.pop()
        if (currentLevelExits.isEmpty()) return
        val nextLevelExits = dataFlowSourcesForNextCompletedCall.topOrNull().takeIf { !callCompleted }
        if (nextLevelExits != null) {
            // Call is incomplete, don't pass data flow from lambdas inside it to lambdas in the outer call.
            nextLevelExits.addAll(currentLevelExits)
        } else {
            currentLevelExits.addPostponedLambdaDataFlowEdgesTo(node)
        }
    }

    // There may be branching expressions on the way from a called-in-place lambda
    // to the next completed call:
    //
    //   f(if (p) { x; run { a } else { y; run { b } }, c)
    //
    // which result in a hole-y control flow graph at the time when we need to resolve `c`:
    //
    //   p -+--> x ->  ??   -> run#1 --+-> c -> f
    //       \-> y ->  ??   -> run#2 -/
    //
    // Ideally, we want to pretend that the lambdas are not called-in-place until we get
    // to `f`, at which point the lambdas are guaranteed to be resolved and we should be
    // able to reconstruct the entire data flow. The problem is that the call/when/etc.
    // exit nodes on the way from the lambda to the function call exit node can have
    // statements attached to them, so unless we want to re-do all the work, it's too late
    // by the time we get there. And we can't just forever ignore the lambdas either, as
    // they may reassign variables and so the data we've gathered about them should be
    // invalidated. So what we do here is merge the data from the lambdas with the data
    // obtained without them: this can only erase statements that are not provably correct.
    private fun mergeDataFlowFromPostponedLambdas(node: CFGNode<*>, callCompleted: Boolean): MergePostponedLambdaExitsNode? {
        val currentLevelExits = dataFlowSourcesForNextCompletedCall.pop()
        if (currentLevelExits.isEmpty()) return null

        val nextLevelExits = dataFlowSourcesForNextCompletedCall.topOrNull().takeIf { !callCompleted }
        return if (nextLevelExits != null) {
            // Call is incomplete, don't pass data flow from lambdas inside it to lambdas in the outer call.
            // TODO: this is wrong, we don't necessarily have all the lambdas yet... Also,
            //  the lambdas in the list can come from different branches, and this should be
            //  more like merge(node, union(lambdas from branch 1), ..., union(from branch N)).
            createMergePostponedLambdaExitsNode(node.fir).also {
                addEdge(node, it)
                currentLevelExits.addPostponedLambdaDataFlowEdgesTo(it)
                nextLevelExits.add(it)
            }
        } else {
            currentLevelExits.addPostponedLambdaDataFlowEdgesTo(node)
            null
        }
    }

    // ----------------------------------- Classes -----------------------------------

    fun enterClass() {
        pushGraph(
            ControlFlowGraph(null, "STUB_CLASS_GRAPH", ControlFlowGraph.Kind.Stub),
            mode = Mode.ClassInitializer
        )
    }

    fun exitClass() {
        popGraph()
    }

    fun exitClass(klass: FirClass): ControlFlowGraph {
        exitClass()
        val name = when (klass) {
            is FirAnonymousObject -> "<anonymous object>"
            is FirRegularClass -> klass.name.asString()
            else -> throw IllegalArgumentException("Unknown class kind: ${klass::class}")
        }

        val classGraph = ControlFlowGraph(klass, name, ControlFlowGraph.Kind.ClassInitializer)
        pushGraph(classGraph, Mode.ClassInitializer)
        val exitNode = createClassExitNode(klass)
        var node: CFGNode<*> = createClassEnterNode(klass)
        var prevInitPartNode: CFGNode<*>? = null
        for (declaration in klass.declarations) {
            val graph = when (declaration) {
                is FirProperty -> declaration.controlFlowGraphReference?.controlFlowGraph
                is FirField -> declaration.controlFlowGraphReference?.controlFlowGraph
                is FirAnonymousInitializer -> declaration.controlFlowGraphReference?.controlFlowGraph
                else -> null
            } ?: continue

            createPartOfClassInitializationNode(declaration as FirControlFlowGraphOwner).also {
                addEdge(node, it, preferredKind = EdgeKind.CfgForward)
                addEdge(it, graph.enterNode, preferredKind = EdgeKind.CfgForward)
                node = graph.exitNode

                if (prevInitPartNode != null) addEdge(prevInitPartNode!!, it, preferredKind = EdgeKind.DeadForward)
                it.updateDeadStatus()
                prevInitPartNode = it
            }
        }
        addEdge(node, exitNode, preferredKind = EdgeKind.CfgForward)
        if (prevInitPartNode != null) addEdge(prevInitPartNode!!, exitNode, preferredKind = EdgeKind.DeadForward)
        exitNode.updateDeadStatus()
        return popGraph()
    }

    fun prepareForLocalClassMembers(members: Collection<FirDeclaration>) {
        members.forEachMember {
            enterToLocalClassesMembers[it.symbol] = lastNodes.topOrNull()
        }
    }

    fun cleanAfterForLocalClassMembers(members: Collection<FirDeclaration>) {
        members.forEachMember {
            enterToLocalClassesMembers.remove(it.symbol)
        }
    }

    fun exitLocalClass(klass: FirRegularClass): Pair<LocalClassExitNode, ControlFlowGraph> {
        val graph = exitClass(klass).also {
            currentGraph.addSubGraph(it)
        }
        val node = createLocalClassExitNode(klass).also {
            addNewSimpleNodeIfPossible(it)
        }
        visitLocalClassFunctions(klass, node)
        addEdge(node, graph.enterNode, preferredKind = EdgeKind.CfgForward)
        return node to graph
    }

    fun enterAnonymousObject(anonymousObject: FirAnonymousObject): AnonymousObjectEnterNode {
        val enterNode = createAnonymousObjectEnterNode(anonymousObject)
        // TODO: looks like there was some problem with enum initializers that causes `lastNodes` to be empty
        lastNodes.popOrNull()?.let { addEdge(it, enterNode, preferredKind = EdgeKind.Forward) }
        lastNodes.push(enterNode)
        enterClass()
        return enterNode
    }

    fun exitAnonymousObject(anonymousObject: FirAnonymousObject): Pair<AnonymousObjectExitNode, ControlFlowGraph> {
        val graph = exitClass(anonymousObject).also {
            currentGraph.addSubGraph(it)
        }
        val enterNode = lastNodes.popOrNull()
        if (enterNode !is AnonymousObjectEnterNode) {
            throw AssertionError("anonymous object exit should be preceded by anonymous object enter, but got $enterNode")
        }
        val exitNode = createAnonymousObjectExitNode(anonymousObject)
        // TODO: Intentionally not using anonymous object init blocks for data flow? Might've been a FE1.0 bug.
        addEdge(enterNode, graph.enterNode, preferredKind = EdgeKind.CfgForward)
        if (!graph.exitNode.isDead) {
            addEdge(graph.exitNode, exitNode, preferredKind = EdgeKind.CfgForward)
        }
        addEdge(enterNode, exitNode, preferredKind = EdgeKind.DfgForward)
        // TODO: Here we're assuming that the methods are called after the object is constructed, which is really not true
        //   (init blocks can call them). But FE1.0 did so too, hence the following code compiles and prints 0:
        //     val x: Int
        //     object {
        //         fun bar() = x
        //         init { x = bar() }
        //     }
        //     println(x)
        visitLocalClassFunctions(anonymousObject, exitNode)
        lastNodes.push(exitNode)
        return exitNode to graph
    }

    fun exitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression): AnonymousObjectExpressionExitNode {
        return createAnonymousObjectExpressionExitNode(anonymousObjectExpression).also {
            addNewSimpleNodeIfPossible(it)
        }
    }

    fun enterScript(script: FirScript): ScriptEnterNode {
        pushGraph(
            ControlFlowGraph(null, "SCRIPT_GRAPH", ControlFlowGraph.Kind.Function),
            mode = Mode.Body
        )
        val enterNode = createScriptEnterNode(script)
        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitScript(script: FirScript): ScriptExitNode {
        return createScriptExitNode(script).also {
            addNewSimpleNodeIfPossible(it)
        }
    }

    private fun visitLocalClassFunctions(klass: FirClass, node: CFGNodeWithSubgraphs<*>) {
        klass.declarations.filterIsInstance<FirFunction>().forEach { function ->
            val functionGraph = function.controlFlowGraphReference?.controlFlowGraph
            if (functionGraph != null && functionGraph.owner == null) {
                addEdge(node, functionGraph.enterNode, preferredKind = EdgeKind.CfgForward)
                node.addSubGraph(functionGraph)
            }
        }
    }

    // ----------------------------------- Value parameters (and it's defaults) -----------------------------------

    fun enterValueParameter(valueParameter: FirValueParameter): EnterDefaultArgumentsNode? {
        if (valueParameter.defaultValue == null) return null
        val graph = ControlFlowGraph(valueParameter, "default value of ${valueParameter.name}", ControlFlowGraph.Kind.DefaultArgument)
        currentGraph.addSubGraph(graph)
        pushGraph(graph, Mode.Body)

        createExitDefaultArgumentsNode(valueParameter).also {
            exitTargetsForTry.push(it)
        }

        return createEnterDefaultArgumentsNode(valueParameter).also {
            addEdge(lastNode, it)
            lastNodes.push(it)
        }
    }

    fun exitValueParameter(valueParameter: FirValueParameter): Pair<ExitDefaultArgumentsNode, ControlFlowGraph>? {
        if (valueParameter.defaultValue == null) return null
        val exitNode = exitTargetsForTry.pop() as ExitDefaultArgumentsNode
        popAndAddEdge(exitNode)
        val graph = popGraph()
        require(exitNode == graph.exitNode)
        return exitNode to graph
    }

    // ----------------------------------- Block -----------------------------------

    fun enterBlock(block: FirBlock): BlockEnterNode {
        return createBlockEnterNode(block).also {
            addNewSimpleNode(it)
        }
    }

    fun exitBlock(block: FirBlock): CFGNode<*> {
        return createBlockExitNode(block).also {
            addNewSimpleNode(it)
        }
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty): PropertyInitializerEnterNode? {
        if (property.initializer == null && property.delegate == null && !property.hasExplicitBackingField) return null

        val graph = ControlFlowGraph(property, "val ${property.name}", ControlFlowGraph.Kind.PropertyInitializer)
        pushGraph(graph, Mode.PropertyInitializer)

        val enterNode = createPropertyInitializerEnterNode(property)
        val exitNode = createPropertyInitializerExitNode(property)
        exitTargetsForTry.push(exitNode)

        enterToLocalClassesMembers[property.symbol]?.let {
            addEdge(it, enterNode, preferredKind = EdgeKind.DfgForward)
        }

        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitProperty(property: FirProperty): Pair<PropertyInitializerExitNode, ControlFlowGraph>? {
        if (property.initializer == null && property.delegate == null && !property.hasExplicitBackingField) return null
        val exitNode = exitTargetsForTry.pop() as PropertyInitializerExitNode
        popAndAddEdge(exitNode)
        val graph = popGraph()
        assert(exitNode == graph.exitNode)
        return exitNode to graph
    }

    // ----------------------------------- Field -----------------------------------

    fun enterField(field: FirField): FieldInitializerEnterNode? {
        if (field.initializer == null) return null

        val graph = ControlFlowGraph(field, "val ${field.name}", ControlFlowGraph.Kind.FieldInitializer)
        pushGraph(graph, Mode.FieldInitializer)

        val enterNode = createFieldInitializerEnterNode(field)
        val exitNode = createFieldInitializerExitNode(field)
        exitTargetsForTry.push(exitNode)

        enterToLocalClassesMembers[field.symbol]?.let {
            addEdge(it, enterNode, preferredKind = EdgeKind.DfgForward)
        }

        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitField(field: FirField): Pair<FieldInitializerExitNode, ControlFlowGraph>? {
        if (field.initializer == null) return null
        val exitNode = exitTargetsForTry.pop() as FieldInitializerExitNode
        popAndAddEdge(exitNode)
        val graph = popGraph()
        assert(exitNode == graph.exitNode)
        return exitNode to graph
    }

    // ----------------------------------- Delegate -----------------------------------

    fun enterDelegateExpression() {
        splitDataFlowForPostponedLambdas()
    }

    fun exitDelegateExpression(fir: FirExpression): DelegateExpressionExitNode {
        return createDelegateExpressionExitNode(fir).also {
            // `val x by y` is resolved as either `val x$delegate = y.provideDelegate()` or `val x$delegate = y.id()`,
            // where `fun <T> T.id(): T`...except `id` doesn't exist, and what that means is that `y` is resolved in
            // context-dependent mode, and we don't necessarily get an enclosing completed call to unify data flow in.
            // This node serves as a substitute.
            unifyDataFlowFromPostponedLambdas(it, callCompleted = true)
            addNewSimpleNode(it)
        }
    }

    // ----------------------------------- Operator call -----------------------------------

    fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall): TypeOperatorCallNode {
        return createTypeOperatorCallNode(typeOperatorCall).also { addNewSimpleNode(it) }
    }

    fun exitComparisonExpression(comparisonExpression: FirComparisonExpression): ComparisonExpressionNode {
        return createComparisonExpressionNode(comparisonExpression).also { addNewSimpleNode(it) }
    }

    fun exitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall): EqualityOperatorCallNode {
        return createEqualityOperatorCallNode(equalityOperatorCall).also { addNewSimpleNode(it) }
    }

    // ----------------------------------- Jump -----------------------------------

    fun exitJump(jump: FirJump<*>): JumpNode {
        val node = createJumpNode(jump)
        val nextNode = when (jump) {
            is FirReturnExpression -> exitTargetsForReturn[jump.target.labeledElement.symbol]
            is FirContinueExpression -> loopConditionEnterNodes[jump.target.labeledElement.condition]
            is FirBreakExpression -> loopExitNodes[jump.target.labeledElement]
            else -> throw IllegalArgumentException("Unknown jump type: ${jump.render()}")
        }

        val labelForFinallyBLock = when (jump) {
            is FirReturnExpression -> ReturnPath(jump.target.labeledElement.symbol)
            is FirLoopJump -> LoopPath(jump)
            else -> NormalPath
        }

        // while (...) continue // <- jump back
        // do continue while (...) // <- jump forward (block exit node not created yet)
        // do ... while (continue) // <- jump back (to itself), believe it or not
        val isBack = nextNode is LoopConditionEnterNode &&
                (nextNode.loop !is FirDoWhileLoop || nextNode.previousNodes.any { it is LoopBlockExitNode })
        addNodeWithJump(
            node,
            nextNode,
            isBack = isBack,
            trackJump = jump is FirReturnExpression,
            label = if (isBack) LoopBackPath else NormalPath,
            labelForFinallyBLock = labelForFinallyBLock
        )
        return node
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression): WhenEnterNode {
        val node = createWhenEnterNode(whenExpression)
        addNewSimpleNode(node)
        whenExitNodes.push(createWhenExitNode(whenExpression))
        whenBranchIndices.push(whenExpression.branches.mapIndexed { index, branch -> branch to index }.toMap())
        notCompletedFunctionCalls.push(mutableListOf())
        levelCounter++
        splitDataFlowForPostponedLambdas()
        return node
    }

    fun exitWhenSubjectExpression(expression: FirWhenSubjectExpression): WhenSubjectExpressionExitNode {
        return createWhenSubjectExpressionExitNode(expression).also { addNewSimpleNode(it) }
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch): WhenBranchConditionEnterNode {
        levelCounter += whenBranchIndices.top().getValue(whenBranch)
        return createWhenBranchConditionEnterNode(whenBranch).also { addNewSimpleNode(it) }.also { levelCounter++ }
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch): Pair<WhenBranchConditionExitNode, WhenBranchResultEnterNode> {
        levelCounter--
        val conditionExitNode = createWhenBranchConditionExitNode(whenBranch).also {
            addNewSimpleNode(it)
        }.also { levelCounter++ }
        val branchEnterNode = createWhenBranchResultEnterNode(whenBranch).also {
            lastNodes.push(it)
            addEdge(conditionExitNode, it)
        }
        return conditionExitNode to branchEnterNode
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch): WhenBranchResultExitNode {
        levelCounter--
        val node = createWhenBranchResultExitNode(whenBranch)
        popAndAddEdge(node)
        val whenExitNode = whenExitNodes.top()
        addEdge(node, whenExitNode, propagateDeadness = false)
        levelCounter -= whenBranchIndices.top().getValue(whenBranch)
        return node
    }

    fun exitWhenExpression(
        whenExpression: FirWhenExpression,
        callCompleted: Boolean
    ): Triple<WhenExitNode, WhenSyntheticElseBranchNode?, MergePostponedLambdaExitsNode?> {
        val whenExitNode = whenExitNodes.pop()
        // exit from last condition node still on stack
        // we should remove it
        notCompletedFunctionCalls.pop().forEach(::completeFunctionCall)
        val lastWhenConditionExit = lastNodes.pop()
        val syntheticElseBranchNode = if (!whenExpression.isProperlyExhaustive) {
            createWhenSyntheticElseBranchNode(whenExpression).apply {
                addEdge(lastWhenConditionExit, this)
                addEdge(this, whenExitNode)
            }
        } else null
        whenExitNode.updateDeadStatus()
        lastNodes.push(whenExitNode)
        levelCounter--
        whenBranchIndices.pop()
        return Triple(whenExitNode, syntheticElseBranchNode, mergeDataFlowFromPostponedLambdas(whenExitNode, callCompleted))
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop): Pair<LoopEnterNode, LoopConditionEnterNode> {
        val loopEnterNode = createLoopEnterNode(loop).also {
            addNewSimpleNode(it)
        }
        loopExitNodes.push(createLoopExitNode(loop))
        levelCounter++
        val conditionEnterNode = createLoopConditionEnterNode(loop.condition, loop).also {
            addNewSimpleNode(it)
            loopConditionEnterNodes.push(it)
        }
        levelCounter++
        return loopEnterNode to conditionEnterNode
    }

    fun exitWhileLoopCondition(loop: FirLoop): Pair<LoopConditionExitNode, LoopBlockEnterNode> {
        levelCounter--
        val conditionExitNode = createLoopConditionExitNode(loop.condition)
        addNewSimpleNode(conditionExitNode)
        val conditionConstBooleanValue = loop.condition.booleanConstValue
        addEdge(conditionExitNode, loopExitNodes.top(), propagateDeadness = false, isDead = conditionConstBooleanValue == true)
        val loopBlockEnterNode = createLoopBlockEnterNode(loop)
        addNewSimpleNode(loopBlockEnterNode, conditionConstBooleanValue == false)
        levelCounter++
        return conditionExitNode to loopBlockEnterNode
    }

    fun exitWhileLoop(loop: FirLoop): Triple<LoopConditionEnterNode, LoopBlockExitNode, LoopExitNode> {
        levelCounter--
        val loopBlockExitNode = createLoopBlockExitNode(loop)
        popAndAddEdge(loopBlockExitNode)
        val conditionEnterNode = loopConditionEnterNodes.pop()
        addBackEdge(loopBlockExitNode, conditionEnterNode, label = LoopBackPath)
        val loopExitNode = loopExitNodes.pop()
        loopExitNode.updateDeadStatus()
        lastNodes.push(loopExitNode)
        levelCounter--
        return Triple(conditionEnterNode, loopBlockExitNode, loopExitNode)
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop): Pair<LoopEnterNode, LoopBlockEnterNode> {
        val loopEnterNode = createLoopEnterNode(loop)
        addNewSimpleNode(loopEnterNode)
        loopExitNodes.push(createLoopExitNode(loop))
        levelCounter++
        val blockEnterNode = createLoopBlockEnterNode(loop)
        addNewSimpleNode(blockEnterNode)
        // put block enter node twice so we can refer it after exit from loop condition
        lastNodes.push(blockEnterNode)
        loopConditionEnterNodes.push(createLoopConditionEnterNode(loop.condition, loop))
        levelCounter++
        return loopEnterNode to blockEnterNode
    }

    fun enterDoWhileLoopCondition(loop: FirLoop): Pair<LoopBlockExitNode, LoopConditionEnterNode> {
        levelCounter--
        val blockExitNode = createLoopBlockExitNode(loop).also { addNewSimpleNode(it) }
        // This may sound shocking, but `do...while` conditions can `continue` to themselves,
        // so we can't pop the node off the stack here.
        val conditionEnterNode = loopConditionEnterNodes.top().also { addNewSimpleNode(it) }
        levelCounter++
        return blockExitNode to conditionEnterNode
    }

    fun exitDoWhileLoop(loop: FirLoop): Pair<LoopConditionExitNode, LoopExitNode> {
        loopConditionEnterNodes.pop()
        levelCounter--
        val conditionExitNode = createLoopConditionExitNode(loop.condition)
        val conditionBooleanValue = loop.condition.booleanConstValue
        popAndAddEdge(conditionExitNode)
        val blockEnterNode = lastNodes.pop()
        require(blockEnterNode is LoopBlockEnterNode)
        addBackEdge(conditionExitNode, blockEnterNode, isDead = conditionBooleanValue == false, label = LoopBackPath)
        val loopExit = loopExitNodes.pop()
        addEdge(conditionExitNode, loopExit, propagateDeadness = false, isDead = conditionBooleanValue == true)
        loopExit.updateDeadStatus()
        lastNodes.push(loopExit)
        levelCounter--
        return conditionExitNode to loopExit
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression): CFGNode<FirBinaryLogicExpression> {
        val (enterNode, exitNode) = when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> createBinaryAndEnterNode(binaryLogicExpression) to createBinaryAndExitNode(binaryLogicExpression)
            LogicOperationKind.OR -> createBinaryOrEnterNode(binaryLogicExpression) to createBinaryOrExitNode(binaryLogicExpression)
        }
        addNewSimpleNode(enterNode)
        binaryLogicExpressionExitNodes.push(exitNode)
        levelCounter++
        return enterNode
    }

    fun exitLeftBinaryLogicExpressionArgument(
        binaryLogicExpression: FirBinaryLogicExpression
    ): Pair<CFGNode<FirBinaryLogicExpression>, CFGNode<FirBinaryLogicExpression>> {
        levelCounter--
        val leftBooleanConstValue = binaryLogicExpression.leftOperand.booleanConstValue
        val (leftExitNode, rightEnterNode) = when (binaryLogicExpression.kind) {
            LogicOperationKind.AND ->
                createBinaryAndExitLeftOperandNode(binaryLogicExpression) to createBinaryAndEnterRightOperandNode(binaryLogicExpression)
            LogicOperationKind.OR ->
                createBinaryOrExitLeftOperandNode(binaryLogicExpression) to createBinaryOrEnterRightOperandNode(binaryLogicExpression)
        }
        val isAnd = binaryLogicExpression.kind == LogicOperationKind.AND
        val lastNode = lastNodes.pop()
        addEdge(lastNode, leftExitNode)
        addEdge(leftExitNode, binaryLogicExpressionExitNodes.top(), propagateDeadness = false, isDead = leftBooleanConstValue == isAnd)
        addEdge(leftExitNode, rightEnterNode, isDead = leftBooleanConstValue == !isAnd)
        lastNodes.push(rightEnterNode)
        levelCounter++
        return leftExitNode to rightEnterNode
    }

    fun exitBinaryLogicExpression(): AbstractBinaryExitNode<FirBinaryLogicExpression> {
        levelCounter--
        val exitNode = binaryLogicExpressionExitNodes.pop()
        val rightNode = lastNodes.pop()
        addEdge(rightNode, exitNode, propagateDeadness = false)
        exitNode.updateDeadStatus()
        lastNodes.push(exitNode)
        return exitNode
    }

    private val FirExpression.booleanConstValue: Boolean? get() = (this as? FirConstExpression<*>)?.value as? Boolean?

    // ----------------------------------- Try-catch-finally -----------------------------------

    fun enterTryExpression(tryExpression: FirTryExpression): Pair<TryExpressionEnterNode, TryMainBlockEnterNode> {
        catchNodeStorages.push(NodeStorage())
        catchExitNodeStorages.push(NodeStorage())
        val enterTryExpressionNode = createTryExpressionEnterNode(tryExpression)
        addNewSimpleNode(enterTryExpressionNode)
        val tryExitNode = createTryExpressionExitNode(tryExpression)
        tryExitNodes.push(tryExitNode)
        levelCounter++
        val enterTryNodeBlock = createTryMainBlockEnterNode(tryExpression)
        addNewSimpleNode(enterTryNodeBlock)

        val exitTryNodeBlock = createTryMainBlockExitNode(tryExpression)
        tryMainExitNodes.push(exitTryNodeBlock)

        for (catch in tryExpression.catches) {
            val catchNode = createCatchClauseEnterNode(catch)
            catchNodeStorage.push(catchNode)
            // a flow where an exception of interest is thrown and caught before executing any of try-main block.
            addEdge(enterTryExpressionNode, catchNode)
        }
        levelCounter++

        if (tryExpression.finallyBlock != null) {
            val finallyEnterNode = createFinallyBlockEnterNode(tryExpression)
            // a flow where an uncaught exception is thrown before executing any of try-main block.
            addEdge(enterTryExpressionNode, finallyEnterNode, propagateDeadness = false, label = UncaughtExceptionPath)
            finallyEnterNodes.push(finallyEnterNode)
            finallyExitNodes.push(createFinallyBlockExitNode(tryExpression))
        }
        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        return enterTryExpressionNode to enterTryNodeBlock
    }

    fun exitTryMainBlock(): TryMainBlockExitNode {
        levelCounter--
        val node = tryMainExitNodes.top()
        popAndAddEdge(node)
        node.updateDeadStatus()
        val finallyEnterNode = finallyEnterNodes.topOrNull()
        // NB: Check the level to avoid adding an edge to the finally block at an upper level.
        if (finallyEnterNode != null && finallyEnterNode.level == levelCounter + 1) {
            // TODO: in case of return/continue/break in try main block, we need a unique label.
            addEdge(node, finallyEnterNode)
            //in case try exit is dead, but there is other edges to finally (eg return)
            // actually finallyEnterNode can't be dead, except for the case when the whole try is dead
            finallyEnterNode.updateDeadStatus()
        } else {
            addEdge(node, tryExitNodes.top(), propagateDeadness = false)
        }
        return node
    }

    fun enterCatchClause(catch: FirCatch): CatchClauseEnterNode {
        return catchNodeStorage[catch]!!.also {
            val tryMainExitNode = tryMainExitNodes.top()
            // a flow where an exception of interest is thrown and caught after executing all of try-main block.
            addEdge(tryMainExitNode, it)
            //tryMainExitNode might be dead (eg main block contains return), but it doesn't mean catch block is also dead
            it.updateDeadStatus()
            val finallyEnterNode = finallyEnterNodes.topOrNull()
            // a flow where an uncaught exception is thrown before executing any of catch clause.
            // NB: Check the level to avoid adding an edge to the finally block at an upper level.
            if (finallyEnterNode != null && finallyEnterNode.level == levelCounter + 1) {
                addEdge(it, finallyEnterNode, propagateDeadness = false, label = UncaughtExceptionPath)
            } else {
                addEdge(it, exitTargetsForTry.top(), label = UncaughtExceptionPath)
            }
            lastNodes.push(it)
            levelCounter++
        }
    }

    fun exitCatchClause(catch: FirCatch): CatchClauseExitNode {
        levelCounter--
        return createCatchClauseExitNode(catch).also {
            popAndAddEdge(it)
            val finallyEnterNode = finallyEnterNodes.topOrNull()
            // NB: Check the level to avoid adding an edge to the finally block at an upper level.
            if (finallyEnterNode != null && finallyEnterNode.level == levelCounter + 1) {
                // TODO: in case of return/continue/break in catch clause, we need a unique label.
                addEdge(it, finallyEnterNode, propagateDeadness = false)
            } else {
                addEdge(it, tryExitNodes.top(), propagateDeadness = false)
            }
            catchExitNodeStorages.top().push(it)
        }
    }

    fun enterFinallyBlock(): FinallyBlockEnterNode {
        val enterNode = finallyEnterNodes.pop()
        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitFinallyBlock(): FinallyBlockExitNode {
        return finallyExitNodes.pop().also { finallyExit ->
            popAndAddEdge(finallyExit)
            val tryExitNode = tryExitNodes.top()
            // a flow where either there wasn't any exception or caught if any.
            addEdge(finallyExit, tryExitNode)
            if (finallyExit.isDead) {
                //refresh forward links, which were created before finalizing try expression (eg created by `break`)
                propagateDeadnessForward(finallyExit)
            }
            // a flow that exits to the exit target while there was an uncaught exception.
            //todo this edge might exist already if try has jump outside, so we effectively lose labeled edge here
            addEdgeIfNotExist(finallyExit, exitTargetsForTry.top(), propagateDeadness = false, label = UncaughtExceptionPath)
            // TODO: differentiate flows that return/(re)throw in try main block and/or catch clauses
            //   To do so, we need mappings from such distinct label to original exit target (fun exit or loop)
            //   Also, CFG should support multiple edges towards the same destination node
        }
    }

    fun exitTryExpression(callCompleted: Boolean): Pair<TryExpressionExitNode, MergePostponedLambdaExitsNode?> {
        levelCounter--
        catchNodeStorages.pop()
        val catchExitNodes = catchExitNodeStorages.pop()
        val tryMainExitNode = tryMainExitNodes.pop()

        notCompletedFunctionCalls.pop().forEach(::completeFunctionCall)

        val node = tryExitNodes.pop()
        node.updateDeadStatus()
        lastNodes.push(node)

        val allCatchesAreDead = tryMainExitNode.fir.catches.all { catch -> catchExitNodes[catch]!!.isDead }
        val tryMainBlockIsDead = tryMainExitNode.previousNodes.all { prev ->
            prev.isDead || tryMainExitNode.incomingEdges.getValue(prev).label != NormalPath
        }
        if (tryMainBlockIsDead && allCatchesAreDead) {
            //if try expression doesn't have any regular non-dead exits, we add stub to make everything after dead
            val stub = createStubNode()
            popAndAddEdge(stub)
            lastNodes.push(stub)
        }

        return node to mergeDataFlowFromPostponedLambdas(node, callCompleted)
    }

    //this is a workaround to make function call dead when call is completed _after_ building its node in the graph
    //this happens when completing the last call in try/catch blocks
    //todo this doesn't make fully 'right' Nothing node (doesn't support going to catch and pass through finally)
    // because doing those afterwards is quite challenging
    // it would be much easier if we could build calls after full completion only, at least for Nothing calls
    private fun completeFunctionCall(node: FunctionCallNode) {
        if (!node.fir.resultType.isNothing) return
        val stub = withLevelOfNode(node) { createStubNode() }
        val edges = node.followingNodes.map { it to node.outgoingEdges.getValue(it) }
        CFGNode.removeAllOutgoingEdges(node)
        addEdge(node, stub)
        for ((to, edge) in edges) {
            addEdge(
                from = stub,
                to = to,
                isBack = edge.kind.isBack,
                preferredKind = edge.kind,
                label = edge.label
            )
        }
        stub.followingNodes.forEach { propagateDeadnessForward(it, deep = true) }
    }

    // ----------------------------------- Resolvable call -----------------------------------

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression): QualifiedAccessNode {
        val returnsNothing = qualifiedAccessExpression.resultType.isNothing
        val node = createQualifiedAccessNode(qualifiedAccessExpression)
        if (returnsNothing) {
            addNodeThatReturnsNothing(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
    }

    fun exitSmartCastExpression(smartCastExpression: FirSmartCastExpression): SmartCastExpressionExitNode {
        val returnsNothing = smartCastExpression.resultType.isNothing
        val node = createSmartCastExitNode(smartCastExpression)
        if (returnsNothing) {
            addNodeThatReturnsNothing(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
    }

    fun exitResolvedQualifierNode(resolvedQualifier: FirResolvedQualifier): ResolvedQualifierNode {
        return createResolvedQualifierNode(resolvedQualifier).also(this::addNewSimpleNode)
    }

    fun enterCall() {
        levelCounter++
        splitDataFlowForPostponedLambdas()
    }

    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean): FunctionCallNode {
        levelCounter--
        val returnsNothing = functionCall.resultType.isNothing
        val node = createFunctionCallNode(functionCall)
        unifyDataFlowFromPostponedLambdas(node, callCompleted)
        if (returnsNothing) {
            addNodeThatReturnsNothing(node)
        } else {
            addNewSimpleNode(node)
        }
        if (!returnsNothing && !callCompleted) {
            notCompletedFunctionCalls.topOrNull()?.add(node)
        }
        return node
    }

    fun exitDelegatedConstructorCall(call: FirDelegatedConstructorCall, callCompleted: Boolean): DelegatedConstructorCallNode {
        levelCounter--
        val node = createDelegatedConstructorCallNode(call)
        unifyDataFlowFromPostponedLambdas(node, callCompleted)
        addNewSimpleNode(node)
        return node
    }

    fun exitStringConcatenationCall(call: FirStringConcatenationCall): StringConcatenationCallNode {
        levelCounter--
        val node = createStringConcatenationCallNode(call)
        unifyDataFlowFromPostponedLambdas(node, callCompleted = true)
        addNewSimpleNode(node)
        return node
    }

    fun exitConstExpression(constExpression: FirConstExpression<*>): ConstExpressionNode {
        return createConstExpressionNode(constExpression).also { addNewSimpleNode(it) }
    }

    fun exitVariableDeclaration(variable: FirProperty): VariableDeclarationNode {
        return createVariableDeclarationNode(variable).also { addNewSimpleNode(it) }
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment): VariableAssignmentNode {
        return createVariableAssignmentNode(assignment).also { addNewSimpleNode(it) }
    }

    fun exitThrowExceptionNode(throwExpression: FirThrowExpression): ThrowExceptionNode {
        return createThrowExceptionNode(throwExpression).also { addNodeThatReturnsNothing(it) }
    }

    fun exitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, callCompleted: Boolean): CheckNotNullCallNode {
        levelCounter--
        val node = createCheckNotNullCallNode(checkNotNullCall)
        unifyDataFlowFromPostponedLambdas(node, callCompleted)
        if (checkNotNullCall.resultType.isNothing) {
            addNodeThatReturnsNothing(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
    }

    // ----------------------------------- Annotations -----------------------------------

    fun enterAnnotation(annotation: FirAnnotation): AnnotationEnterNode {
        val graph = ControlFlowGraph(null, "STUB_GRAPH_FOR_ANNOTATION_CALL", ControlFlowGraph.Kind.AnnotationCall)
        pushGraph(graph, Mode.Body)
        return createAnnotationEnterNode(annotation).also {
            lastNodes.push(it)
        }
    }

    fun exitAnnotation(annotation: FirAnnotation): AnnotationExitNode {
        val node = createAnnotationExitNode(annotation)
        popAndAddEdge(node)
        popGraph()
        return node
    }

    // ----------------------------------- Callable references -----------------------------------

    fun exitCallableReference(callableReferenceAccess: FirCallableReferenceAccess): CallableReferenceNode {
        return createCallableReferenceNode(callableReferenceAccess).also { addNewSimpleNode(it) }
    }

    fun exitGetClassCall(getClassCall: FirGetClassCall): GetClassCallNode {
        return createGetClassCallNode(getClassCall).also { addNewSimpleNode(it) }
    }

    // ----------------------------------- Block -----------------------------------

    fun enterInitBlock(initBlock: FirAnonymousInitializer): InitBlockEnterNode {
        // TODO: questionable moment that we should pass data flow from init to init

        val graph = ControlFlowGraph(initBlock, "init block", ControlFlowGraph.Kind.Function)
        pushGraph(graph, Mode.Body)
        val enterNode = createInitBlockEnterNode(initBlock).also {
            lastNodes.push(it)
        }
        val lastNode = runIf(lastNode is InitBlockExitNode) { lastNodes.pop() } ?: enterToLocalClassesMembers[initBlock.symbol]
        lastNode?.let { addEdge(it, enterNode, preferredKind = EdgeKind.DfgForward) }

        createInitBlockExitNode(initBlock).also {
            initBlockExitNodes.push(it)
            exitTargetsForTry.push(it)
        }

        return enterNode
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer): Pair<InitBlockExitNode, ControlFlowGraph> {
        val exitNode = initBlockExitNodes.pop()
        require(exitNode == exitTargetsForTry.pop())
        popAndAddEdge(exitNode)
        val graph = popGraph()
        assert(graph.declaration == initBlock)
        exitNode.updateDeadStatus()
        return exitNode to graph
    }

    // ----------------------------------- Safe calls -----------------------------------

    fun enterSafeCall(safeCall: FirSafeCallExpression): EnterSafeCallNode {
        val enterNode = createEnterSafeCallNode(safeCall)
        val exitNode = createExitSafeCallNode(safeCall)
        exitSafeCallNodes.push(exitNode)
        val lastNode = lastNodes.pop()
        if (lastNode is ExitSafeCallNode) {
            // Only the non-null branch of the previous safe call can enter this one.
            //   a ----> a.b -----> a?.b.c ------> a?.b?.c
            //       \-----\-> a?.b (null) ---^
            addEdge(lastNode.lastPreviousNode, enterNode)
        } else {
            addEdge(lastNode, enterNode)
        }
        val nextElvisRHS = elvisRhsEnterNodes.topOrNull()
        if (nextElvisRHS?.fir?.lhs === safeCall) {
            // Can skip the null edge directly to elvis RHS.
            //                            /-----------v
            //   a ----> a.b ----> a?.b ----> c ----> a?.b ?: c
            //       \------------------------^
            addEdge(lastNode, nextElvisRHS)
        } else {
            addEdge(lastNode, exitNode)
        }
        lastNodes.push(enterNode)
        splitDataFlowForPostponedLambdas()
        return enterNode
    }

    fun exitSafeCall(): Pair<ExitSafeCallNode, MergePostponedLambdaExitsNode?> {
        // There will be two paths towards this exit safe call node:
        // one from the node prior to the enclosing safe call, and
        // the other from the selector part in the enclosing safe call.
        // Note that *neither* points to the safe call directly.
        // So, when it comes to the real exit of the enclosing block/function,
        // the safe call bound to this exit safe call node should be retrieved.
        return exitSafeCallNodes.pop().let {
            addNewSimpleNode(it)
            it.updateDeadStatus()
            // Safe calls only have one user-specified branch, so if any lambdas were postponed, they still are.
            it to mergeDataFlowFromPostponedLambdas(it, callCompleted = false)
        }
    }

    // ----------------------------------- Elvis -----------------------------------

    fun enterElvis(elvisExpression: FirElvisExpression) {
        elvisRhsEnterNodes.push(createElvisRhsEnterNode(elvisExpression))
        splitDataFlowForPostponedLambdas()
    }

    fun exitElvisLhs(elvisExpression: FirElvisExpression): Triple<ElvisLhsExitNode, ElvisLhsIsNotNullNode, ElvisRhsEnterNode> {
        val exitNode = createElvisExitNode(elvisExpression).also {
            exitElvisExpressionNodes.push(it)
        }

        val lhsExitNode = createElvisLhsExitNode(elvisExpression).also {
            popAndAddEdge(it)
        }

        val lhsIsNotNullNode = createElvisLhsIsNotNullNode(elvisExpression).also {
            val lhsIsNull = elvisExpression.lhs.typeRef.coneTypeSafe<ConeKotlinType>()?.isNullableNothing == true
            addEdge(lhsExitNode, it, isDead = lhsIsNull)
            addEdge(it, exitNode, propagateDeadness = false)
        }

        val rhsEnterNode = elvisRhsEnterNodes.pop().also {
            // Can only have a previous node if the LHS is a safe call, in which case it's the safe
            // call's receiver - then RHS is not dead unless said receiver is dead (or never null).
            addEdge(lhsExitNode, it, propagateDeadness = it.previousNodes.isEmpty())
        }
        lastNodes.push(rhsEnterNode)
        return Triple(lhsExitNode, lhsIsNotNullNode, rhsEnterNode)
    }

    fun exitElvis(lhsIsNotNull: Boolean, callCompleted: Boolean): Pair<ElvisExitNode, MergePostponedLambdaExitsNode?> {
        val exitNode = exitElvisExpressionNodes.pop()
        addNewSimpleNode(exitNode, isDead = lhsIsNotNull)
        exitNode.updateDeadStatus()
        return exitNode to mergeDataFlowFromPostponedLambdas(exitNode, callCompleted)
    }

    // ----------------------------------- Contract description -----------------------------------

    fun enterContractDescription(): CFGNode<*> {
        pushGraph(ControlFlowGraph(null, "contract description", ControlFlowGraph.Kind.TopLevel), Mode.Body)

        return createContractDescriptionEnterNode().also {
            lastNodes.push(it)
            exitTargetsForTry.push(it)
        }
    }

    fun exitContractDescription() {
        lastNodes.pop()
        exitTargetsForTry.pop()
        popGraph()
    }

    // -------------------------------------------------------------------------------------------------------------------------

    fun reset() {
        exitsOfAnonymousFunctions.clear()
        dataFlowSourcesForNextCompletedCall.reset()
        lastNodes.reset()
    }

    fun dropSubgraphFromCall(call: FirFunctionCall) {
        val graphs = mutableListOf<ControlFlowGraph>()

        call.acceptChildren(object : FirDefaultVisitor<Unit, Any?>() {
            override fun visitElement(element: FirElement, data: Any?) {
                element.acceptChildren(this, null)
            }

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?) {
                anonymousFunction.controlFlowGraphReference?.accept(this, null)
            }

            override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?) {
                anonymousObject.controlFlowGraphReference?.accept(this, null)
            }

            override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: Any?) {
                val graph = controlFlowGraphReference.controlFlowGraph ?: return
                if (graph.owner == null) return
                graphs += graph
            }
        }, null)

        for (graph in graphs) {
            currentGraph.removeSubGraph(graph)
        }
    }

    // ----------------------------------- Edge utils -----------------------------------

    private fun addNewSimpleNode(
        node: CFGNode<*>,
        isDead: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward
    ): CFGNode<*> {
        val lastNode = lastNodes.pop()
        addEdge(lastNode, node, isDead = isDead, preferredKind = preferredKind)
        lastNodes.push(node)
        return lastNode
    }

    private fun addNodeThatReturnsNothing(node: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Forward) {
        // If an expression, which returns Nothing, ...(1)
        val targetNode = when {
            tryExitNodes.isEmpty -> {
                // (1)... isn't inside a try expression, that is an uncaught exception path.
                exitTargetsForTry.top()
            }
            // (1)... inside a try expression...(2)
            finallyEnterNodes.topOrNull()?.level == levelCounter -> {
                // (2)... with finally
                // Either in try-main or catch. Route to `finally`
                finallyEnterNodes.top()
            }
            // (2)... without finally or within finally ...(3)
            tryExitNodes.top().fir.finallyBlock == null -> {
                // (3)... without finally ...(4)
                // Either in try-main or catch.
                val tryMainExitNode = tryMainExitNodes.top()
                if (tryMainExitNode.followingNodes.isNotEmpty()) {
                    // (4)... in catch, i.e., re-throw.
                    exitTargetsForTry.top()
                } else {
                    // (4)... in try-main. Route to exit of try main block.
                    // We already have edges from the exit of try main block to each catch clause.
                    // This edge makes the remaining part of try main block, e.g., following `when` branches, marked as dead.
                    tryMainExitNode
                }
            }
            // (3)... within finally.
            else -> exitTargetsForTry.top()
        }
        if (targetNode is TryMainBlockExitNode) {
            val catches = targetNode.fir.catches
            if (catches.isEmpty()) {
                addNodeWithJump(node, exitTargetsForTry.top(), preferredKind, label = UncaughtExceptionPath)
            } else {
                //edges to all the catches
                addNodeWithJumpToCatches(node, catches.map { catchNodeStorage[it]!! }, preferredKind = preferredKind)
            }
        } else {
            addNodeWithJump(node, targetNode, preferredKind, label = UncaughtExceptionPath)
        }
    }

    private fun addNodeWithJump(
        node: CFGNode<*>,
        targetNode: CFGNode<*>?,
        preferredKind: EdgeKind = EdgeKind.Forward,
        isBack: Boolean = false,
        label: EdgeLabel = NormalPath,
        labelForFinallyBLock: EdgeLabel = label,
        trackJump: Boolean = false
    ) {
        popAndAddEdge(node, preferredKind)
        if (targetNode != null) {
            if (isBack) {
                addBackEdge(node, targetNode, label = label)
            } else {
                // go through all final nodes between node and target
                val finallyNodes = finallyBefore(targetNode)
                val finalFrom = finallyNodes.fold(node) { from, (finallyEnter, tryExit) ->
                    addEdgeIfNotExist(from, finallyEnter, propagateDeadness = false, label = labelForFinallyBLock)
                    tryExit
                }
                addEdgeIfNotExist(
                    finalFrom,
                    targetNode,
                    propagateDeadness = false,
                    label = if (finallyNodes.isEmpty()) label else labelForFinallyBLock
                )
                if (trackJump && finallyNodes.isNotEmpty()) {
                    //actually we can store all returns like this, but not sure if it makes anything better
                    nonDirectJumps.put(targetNode, node)
                }
            }
        }
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    private fun addNodeWithJumpToCatches(
        node: CFGNode<*>,
        targets: List<CatchClauseEnterNode>,
        label: EdgeLabel = UncaughtExceptionPath,
        preferredKind: EdgeKind = EdgeKind.Forward
    ) {
        require(targets.isNotEmpty())
        popAndAddEdge(node, preferredKind)
        targets.forEach { target ->
            addEdge(node, target, propagateDeadness = false, label = label)
        }
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    private fun finallyBefore(target: CFGNode<*>): List<Pair<FinallyBlockEnterNode, FinallyBlockExitNode>> {
        return finallyEnterNodes.all().takeWhile { it.level > target.level }.map { finallyEnter ->
            val finallyExit = finallyExitNodes[finallyEnter.fir]!!
            finallyEnter to finallyExit
        }
    }

    private fun popAndAddEdge(to: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Forward) {
        addEdge(lastNodes.pop(), to, preferredKind = preferredKind)
    }

    private fun addEdgeIfNotExist(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        isDead: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward,
        label: EdgeLabel = NormalPath
    ) {
        if (!from.followingNodes.contains(to)) {
            addEdge(from, to, propagateDeadness, isDead, preferredKind = preferredKind, label = label)
        }
    }

    private fun addEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        isDead: Boolean = false,
        isBack: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward,
        label: EdgeLabel = NormalPath
    ) {
        val kind = if (isDead || from.isDead || to.isDead) {
            if (isBack) EdgeKind.DeadBackward else EdgeKind.DeadForward
        } else preferredKind
        CFGNode.addEdge(from, to, kind, propagateDeadness, label)
    }

    private fun addBackEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        isDead: Boolean = false,
        label: EdgeLabel = NormalPath
    ) {
        addEdge(from, to, propagateDeadness = false, isDead = isDead, isBack = true, preferredKind = EdgeKind.CfgBackward, label = label)
    }

    private fun propagateDeadnessForward(node: CFGNode<*>, deep: Boolean = false) {
        if (!node.isDead) return
        node.followingNodes
            .filter { node.outgoingEdges.getValue(it).kind == EdgeKind.Forward }
            .forEach { target ->
                CFGNode.addJustKindEdge(node, target, EdgeKind.DeadForward, false)
                target.updateDeadStatus()
                if (deep) {
                    propagateDeadnessForward(target, true)
                }
            }
    }

    // ----------------------------------- Utils -----------------------------------

    private inline fun Collection<FirDeclaration>.forEachMember(block: (FirDeclaration) -> Unit) {
        for (member in this) {
            for (callableDeclaration in member.unwrap()) {
                block(callableDeclaration)
            }
        }
    }

    private fun FirDeclaration.unwrap(): List<FirDeclaration> =
        when (this) {
            is FirFunction, is FirAnonymousInitializer, is FirField -> listOf(this)
            is FirProperty -> listOfNotNull(this.getter, this.setter, this)
            else -> emptyList()
        }

    private fun addNewSimpleNodeIfPossible(newNode: CFGNode<*>, isDead: Boolean = false): CFGNode<*>? {
        if (lastNodes.isEmpty) return null
        return addNewSimpleNode(newNode, isDead)
    }

    private fun <R> withLevelOfNode(node: CFGNode<*>, f: () -> R): R {
        val last = levelCounter
        levelCounter = node.level
        try {
            return f()
        } finally {
            levelCounter = last
        }
    }

}

fun FirDeclaration?.isLocalClassOrAnonymousObject() = ((this as? FirRegularClass)?.isLocal == true) || this is FirAnonymousObject
