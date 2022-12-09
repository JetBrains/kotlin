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
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.fir.util.listMultimapOf
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
    private val enterToLocalClassesMembers: MutableMap<FirBasedSymbol<*>, CFGNode<*>?> = mutableMapOf()

    //return jumps via finally blocks, target -> jumps
    private val nonDirectJumps: ListMultimap<CFGNode<*>, CFGNode<*>> = listMultimapOf()

    private val argumentListSplitNodes: Stack<SplitPostponedLambdasNode?> = stackOf()
    private val postponedAnonymousFunctionNodes =
        mutableMapOf<FirFunctionSymbol<*>, Pair<CFGNodeWithSubgraphs<*>, PostponedLambdaExitNode?>>()
    private val postponedLambdaExits: Stack<MutableList<Pair<CFGNode<*>, EdgeKind>>> = stackOf()

    private val loopConditionEnterNodes: NodeStorage<FirElement, LoopConditionEnterNode> = NodeStorage()
    private val loopExitNodes: NodeStorage<FirLoop, LoopExitNode> = NodeStorage()

    private val whenExitNodes: Stack<WhenExitNode> = stackOf()

    private val binaryLogicExpressionExitNodes: Stack<AbstractBinaryExitNode<FirBinaryLogicExpression>> = stackOf()

    private val tryExitNodes: NodeStorage<FirTryExpression, TryExpressionExitNode> = NodeStorage()
    private val catchNodes: Stack<List<CatchClauseEnterNode>> = stackOf()
    private val catchBlocksInProgress: Stack<CatchClauseEnterNode> = stackOf()
    private val finallyEnterNodes: Stack<FinallyBlockEnterNode> = stackOf()
    private val finallyBlocksInProgress: Stack<FinallyBlockEnterNode> = stackOf()

    private val initBlockExitNodes: Stack<InitBlockExitNode> = stackOf()

    private val exitSafeCallNodes: Stack<ExitSafeCallNode> = stackOf()
    private val exitElvisExpressionNodes: Stack<ElvisExitNode> = stackOf()
    private val elvisRhsEnterNodes: Stack<ElvisRhsEnterNode> = stackOf()

    private val notCompletedFunctionCalls: Stack<MutableList<FunctionCallNode>> = stackOf()

    // ----------------------------------- API for node builders -----------------------------------

    private var idCounter: Int = Random.nextInt()
    fun createId(): Int = idCounter++

    // ----------------------------------- Public API -----------------------------------

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirExpression>? {
        val exitNode = function.controlFlowGraphReference?.controlFlowGraph?.exitNode ?: return null

        fun CFGNode<*>.returnExpression(): FirExpression? = when (this) {
            is BlockExitNode -> when {
                // lambda@{ x } -> x
                // lambda@{ class C } -> Unit-returning stub
                function.isLambda -> fir.statements.lastOrNull() as? FirExpression
                    ?: buildUnitExpression { source = fir.statements.lastOrNull()?.source ?: fir.source }
                // fun() { terminatingExpression } -> nothing (checker will emit an error if return type is not Unit)
                // fun() { throw } or fun() { returnsNothing() } -> Nothing-returning stub
                else -> FirStub.takeIf { _ -> previousNodes.all { it is StubNode } }
            }
            // lambda@{ return@lambda x } -> x
            is JumpNode -> (fir as? FirReturnExpression)?.takeIf { it.target.labeledElement.symbol == function.symbol }?.result
            else -> null // shouldn't happen? expression bodies are implicitly wrapped in `FirBlock`s
        }

        val returnValues = exitNode.previousNodes.mapNotNullTo(mutableSetOf()) {
            val edge = exitNode.edgeFrom(it)
            // * NormalPath: last expression = return value
            // * UncaughtExceptionPath: last expression = whatever threw, *not* a return value
            // * ReturnPath(this lambda): these go from `finally` blocks, so that's not the return value;
            //   look in `nonDirectJumps` instead.
            it.takeIf { edge.kind.usedInCfa && edge.label == NormalPath }?.returnExpression()
        }
        return nonDirectJumps[exitNode].mapNotNullTo(returnValues) { it.returnExpression() }
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

    fun enterFunction(function: FirFunction): Pair<LocalFunctionDeclarationNode?, FunctionEnterNode> {
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

        return Pair(localFunctionNode, enterNode)
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
    // To implement this, we create dummy enter+exit nodes, then as the call is resolved
    // plop in the lambdas in between them and add looping/skipping edges depending
    // on how many times the lambda is called (only known after we select a candidate
    // for the call). Then, when the call is exited, we add edges from the dummy exit
    // nodes instead of directly from lambdas.
    //
    // If the call is not complete, then it is not guaranteed that lambdas will be resolved
    // before we have to create the function call node. In that case we can still add
    // control flow edges from the dummy nodes, but not data flow edges as the data flow
    // for them may not have been computed yet. Instead, these edges are redirected
    // into the outer call. The outermost call *has* to be completed, so at some point
    // all data will be unified in a single call node.
    fun enterAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression): AnonymousFunctionExpressionNode? {
        val symbol = anonymousFunctionExpression.anonymousFunction.symbol
        val enterNode = postponedAnonymousFunctionNodes[symbol]?.first
            ?: return createAnonymousFunctionExpressionNode(anonymousFunctionExpression).also {
                addNewSimpleNode(it)
                // Not in an argument list, won't be called in-place, don't need an exit node.
                postponedAnonymousFunctionNodes[symbol] = it to null
            }
        val exitNode = createPostponedLambdaExitNode(anonymousFunctionExpression)
        // Ideally we'd only add this edge in `exitAnonymousFunction`, but unfortunately it's possible
        // that the function won't be visited for so long, we'll exit `currentGraph` before that.
        // When exiting a graph, all nodes in it are topologically sorted, so unless we add some edge
        // here, `exitNode` will be dropped from the node list. Oops. TODO: fix `orderNodes` someday.
        addEdge(enterNode, exitNode)
        postponedAnonymousFunctionNodes[symbol] = enterNode to exitNode
        postponedLambdaExits.top().add(exitNode to EdgeKind.Forward)
        return null
    }

    fun enterAnonymousFunction(anonymousFunction: FirAnonymousFunction): FunctionEnterNode {
        val symbol = anonymousFunction.symbol
        val flowSourceNode = postponedAnonymousFunctionNodes.getValue(symbol).first
        pushGraph(ControlFlowGraph(anonymousFunction, "<anonymous>", ControlFlowGraph.Kind.AnonymousFunction), Mode.Function)
        val enterNode = createFunctionEnterNode(anonymousFunction)
        val exitNode = createFunctionExitNode(anonymousFunction)
        exitTargetsForReturn.push(exitNode)
        if (!anonymousFunction.invocationKind.isInPlace) {
            exitTargetsForTry.push(exitNode)
        }
        addEdge(flowSourceNode, enterNode)
        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): Triple<FunctionExitNode, PostponedLambdaExitNode?, ControlFlowGraph> {
        val symbol = anonymousFunction.symbol
        val exitNode = exitTargetsForReturn.pop().also {
            if (!anonymousFunction.invocationKind.isInPlace) {
                require(it == exitTargetsForTry.pop())
            }
        }
        popAndAddEdge(exitNode)
        exitNode.updateDeadStatus()

        val graph = popGraph()
        assert(graph.declaration == anonymousFunction)
        assert(graph.exitNode == exitNode)

        val (splitNode, postponedExitNode) = postponedAnonymousFunctionNodes.remove(symbol)!!
        splitNode.addSubGraph(graph)
        // May not be the current graph: `select(run { run { generic() } }, 1)`
        splitNode.owner.addSubGraph(graph)

        val invocationKind = anonymousFunction.invocationKind
        if (postponedExitNode == null) {
            // Postponed exit node was needed so we could create lambda->call edges without having the subgraph ready. If it
            // doesn't exist, then we probably can't do that anymore, and the lambda won't be called-in-place in the CFG.
            // TODO: verify & enable this assertion?
            //assert(invocationKind?.canBeVisited() != true) { "no exit node for calledInPlace($invocationKind) lambda" }
            return Triple(exitNode, null, graph)
        }

        // Lambdas not called in-place behave as if called never, but with extra invalidation of all smart casts
        // for all variables that they reassign. That second part is handled by `FirDataFlowAnalyzer`.
        val isDefinitelyVisited = invocationKind?.isDefinitelyVisited() == true
        if (isDefinitelyVisited || splitNode.isDead) {
            // The edge that was added as a hack to enforce ordering of nodes needs to be marked as dead if this lambda is never
            // skipped. Or if the entry node is dead, because at the time we added the hack-edge we didn't know that.
            CFGNode.killEdge(splitNode, postponedExitNode, propagateDeadness = !isDefinitelyVisited)
        }
        if (invocationKind?.canBeVisited() == true) {
            addEdge(exitNode, postponedExitNode, propagateDeadness = isDefinitelyVisited)
            if (invocationKind.canBeRevisited()) {
                addBackEdge(postponedExitNode, splitNode)
            }
        }
        return Triple(exitNode, postponedExitNode, graph)
    }

    private fun splitDataFlowForPostponedLambdas() {
        postponedLambdaExits.push(mutableListOf())
    }

    private fun <T> unifyDataFlowFromPostponedLambdas(node: T, callCompleted: Boolean) where T : CFGNode<*>, T : UnionNodeMarker {
        val currentLevelExits = postponedLambdaExits.pop()
        if (currentLevelExits.isEmpty()) return

        val nextLevelExits = postponedLambdaExits.topOrNull().takeIf { !callCompleted }
        if (nextLevelExits != null) {
            // Call is incomplete, don't pass data flow from lambdas inside it to lambdas in the outer call.
            for ((exit, kind) in currentLevelExits) {
                if (kind.usedInCfa) {
                    addEdge(exit, node, preferredKind = EdgeKind.CfgForward)
                }
                nextLevelExits.add(exit to EdgeKind.DfgForward)
            }
        } else {
            for ((exit, kind) in currentLevelExits) {
                // Do not add data flow edges from non-terminating lambdas; there is no "dead data flow only"
                // `EdgeKind`. TODO?
                if (kind.usedInCfa || !exit.isDead) {
                    // Since `node` is a union node, it is dead iff any input is dead. For once, `propagateDeadness`
                    // semantics are correct without an `updateDeadStatus`.
                    addEdge(exit, node, preferredKind = kind)
                }
            }
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
    // to `f`, at which point the lambdas are guaranteed to be resolved, and we should be
    // able to reconstruct the entire data flow. The problem is that the call/when/etc.
    // exit nodes on the way from the lambda to the function call exit node can have
    // statements attached to them, so unless we want to re-do all the work, it's too late
    // by the time we get there. And we can't just forever ignore the lambdas either, as
    // they may reassign variables and so the data we've gathered about them should be
    // invalidated. So what we do here is merge the data from the lambdas with the data
    // obtained without them: this can only erase statements that are not provably correct.
    //
    // TODO: an alternative is to delay computing incoming flow for "branch result exit" nodes
    //   until the entire "when" is resolved; then either unify each branch's lambdas into its
    //   exit node, or create N union nodes (1/branch) and point them into the merge node.
    private fun mergeDataFlowFromPostponedLambdas(node: CFGNode<*>, callCompleted: Boolean) {
        val currentLevelExits = postponedLambdaExits.pop()
        if (currentLevelExits.isEmpty()) return

        val nextLevelExits = postponedLambdaExits.topOrNull().takeIf { !callCompleted }
        if (nextLevelExits != null) {
            node.updateDeadStatus()
            nextLevelExits += createMergePostponedLambdaExitsNode(node.fir).also {
                addEdge(node, it) // copy liveness (deadness?) from `node`
                for ((exit, kind) in currentLevelExits) {
                    if (kind.usedInCfa) {
                        addEdge(exit, node, preferredKind = EdgeKind.CfgForward, propagateDeadness = false)
                    }
                    addEdge(exit, it, preferredKind = EdgeKind.DfgForward, propagateDeadness = false)
                }
            } to EdgeKind.DfgForward
        } else {
            for ((exit, kind) in currentLevelExits) {
                // `node` is a merge node for many inputs anyhow so someone will call `updateDeadStatus` on it.
                addEdge(exit, node, preferredKind = kind, propagateDeadness = false)
            }
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
                currentGraph.addSubGraph(graph)
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
                currentGraph.addSubGraph(functionGraph)
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

    private val FirProperty.hasInitialization: Boolean
        get() = initializer != null || delegate != null || hasExplicitBackingField

    fun enterProperty(property: FirProperty): PropertyInitializerEnterNode? {
        if (!property.hasInitialization) return null

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
        if (!property.hasInitialization) return null
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

    fun enterJump(jump: FirJump<*>) {
        // Data flow from anonymous functions in return values does not merge with any enclosing calls.
        // For named functions, the return value has to be a completed call anyway, so there should
        // be no postponed lambdas in it.
        if (jump is FirReturnExpression && jump.target.labeledElement is FirAnonymousFunction) {
            splitDataFlowForPostponedLambdas()
        }
    }

    fun exitJump(jump: FirJump<*>): JumpNode {
        val node = createJumpNode(jump)
        addNonTerminatingNode(node)

        if (jump is FirReturnExpression && jump.target.labeledElement is FirAnonymousFunction) {
            // TODO: these should be DFA-only edges; they should be pointed into the postponed function exit node?
            //  With builder inference, lambdas are not necessarily resolved starting from the innermost one...
            //  See analysis test cfg/postponedLambdaInReturn.kt.
            postponedLambdaExits.pop()
        }

        val nextNode = when (jump) {
            is FirReturnExpression -> exitTargetsForReturn[jump.target.labeledElement.symbol]
            is FirContinueExpression -> loopConditionEnterNodes[jump.target.labeledElement.condition]
            is FirBreakExpression -> loopExitNodes[jump.target.labeledElement]
            else -> throw IllegalArgumentException("Unknown jump type: ${jump.render()}")
        } ?: return node
        val nextFinally = finallyEnterNodes.topOrNull()?.takeIf { it.level > nextNode.level }
        if (nextFinally != null) {
            addEdge(node, nextFinally, propagateDeadness = false, label = nextNode.returnPathLabel)
            nonDirectJumps.put(nextNode, node)
        } else if (nextNode.returnPathIsBackwards) {
            addBackEdge(node, nextNode)
        } else {
            addEdge(node, nextNode, propagateDeadness = false)
        }
        return node
    }

    private val CFGNode<*>.returnPathLabel: EdgeLabel
        get() = when (this) {
            is FunctionExitNode -> ReturnPath(fir.symbol)
            is LoopConditionEnterNode -> LoopContinuePath(loop)
            is LoopExitNode -> LoopBreakPath(fir)
            else -> throw IllegalStateException("not a labeled jump target: $this")
        }

    // while (x) { continue }
    //       ^------------/ back
    // do { continue } while (x)
    //             \---------^ forward
    // do { x } while (continue)
    //                ^-------/ back
    private val CFGNode<*>.returnPathIsBackwards: Boolean
        get() = this is LoopConditionEnterNode && (loop !is FirDoWhileLoop || previousNodes.any { it is LoopBlockExitNode })

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression): WhenEnterNode {
        val node = createWhenEnterNode(whenExpression)
        addNewSimpleNode(node)
        whenExitNodes.push(createWhenExitNode(whenExpression))
        notCompletedFunctionCalls.push(mutableListOf())
        levelCounter++
        splitDataFlowForPostponedLambdas()
        return node
    }

    fun exitWhenSubjectExpression(expression: FirWhenSubjectExpression): WhenSubjectExpressionExitNode {
        return createWhenSubjectExpressionExitNode(expression).also { addNewSimpleNode(it) }
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch): WhenBranchConditionEnterNode {
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
        levelCounter++
        return node
    }

    fun exitWhenExpression(
        whenExpression: FirWhenExpression,
        callCompleted: Boolean
    ): Pair<WhenExitNode, WhenSyntheticElseBranchNode?> {
        levelCounter -= whenExpression.branches.size
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
        mergeDataFlowFromPostponedLambdas(whenExitNode, callCompleted)
        whenExitNode.updateDeadStatus()
        lastNodes.push(whenExitNode)
        levelCounter--
        return whenExitNode to syntheticElseBranchNode
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
        addBackEdge(loopBlockExitNode, conditionEnterNode)
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
        // Might have had live `continue`s with an unreachable block exit, so recompute deadness.
        conditionEnterNode.updateDeadStatus()
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
        addBackEdge(conditionExitNode, blockEnterNode, isDead = conditionBooleanValue == false)
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
        val enterTryExpressionNode = createTryExpressionEnterNode(tryExpression)
        val exitTryExpressionNode = createTryExpressionExitNode(tryExpression)
        addNewSimpleNode(enterTryExpressionNode)
        tryExitNodes.push(exitTryExpressionNode)
        levelCounter++

        val enterTryMainBlockNode = createTryMainBlockEnterNode(tryExpression)
        addNewSimpleNode(enterTryMainBlockNode)

        catchNodes.push(tryExpression.catches.map { createCatchClauseEnterNode(it) })
        if (tryExpression.finallyBlock != null) {
            finallyEnterNodes.push(createFinallyBlockEnterNode(tryExpression))
        }

        // These edges should really be from `enterTryMainBlockNode`, but there is no practical difference
        // so w/e. In fact, `enterTryExpressionNode` is just 100% redundant.
        // TODO: this is more or less `addExceptionEdgesFrom(enterTryExpressionNode)`. Hmm.
        for (catchEnterNode in catchNodes.top()) {
            addEdge(enterTryExpressionNode, catchEnterNode)
        }
        if (tryExpression.finallyBlock != null) {
            addEdge(enterTryExpressionNode, finallyEnterNodes.top(), label = UncaughtExceptionPath)
        }

        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        levelCounter++
        return enterTryExpressionNode to enterTryMainBlockNode
    }

    fun exitTryMainBlock(): TryMainBlockExitNode {
        levelCounter--
        val exitTryExpressionNode = tryExitNodes.top()
        val node = createTryMainBlockExitNode(exitTryExpressionNode.fir)
        popAndAddEdge(node)
        // try { a } catch (e) { b } [finally { c }]
        //         \-----------------^
        val nextNode = if (node.fir.finallyBlock != null) finallyEnterNodes.top() else exitTryExpressionNode
        // Liveness of `exitTryExpressionNode` will be computed at the end since there are `catch`es.
        // And the `finally` node is never dead unless the entire try-finally is dead.
        addEdge(node, nextNode, propagateDeadness = false)
        for (catchEnterNode in catchNodes.pop().asReversed()) {
            catchBlocksInProgress.push(catchEnterNode)
            // At least merge the data flow from enter + exit...but this doesn't really help,
            // see the comment for `addExceptionEdgesFrom`. Better than nothing, though.
            // Like `finally`, `catch` nodes are only dead if the entire try-catch is dead.
            addEdge(node, catchEnterNode, propagateDeadness = false)
        }
        return node
    }

    fun enterCatchClause(catch: FirCatch): CatchClauseEnterNode {
        val catchEnterNode = catchBlocksInProgress.pop()
        assert(catchEnterNode.fir == catch)
        if (tryExitNodes.top().fir.finallyBlock != null) {
            // TODO: not sure this does anything?
            addEdge(catchEnterNode, finallyEnterNodes.top(), propagateDeadness = false, label = UncaughtExceptionPath)
        }
        lastNodes.push(catchEnterNode)
        levelCounter++
        return catchEnterNode
    }

    fun exitCatchClause(catch: FirCatch): CatchClauseExitNode {
        levelCounter--
        val exitTryExpressionNode = tryExitNodes.top()
        val catchExitNode = createCatchClauseExitNode(catch)
        popAndAddEdge(catchExitNode)
        // try { a } catch (e1) { b } catch (e2) { c } [finally { d }]
        //                          \------------------^
        val nextNode = if (exitTryExpressionNode.fir.finallyBlock != null) finallyEnterNodes.top() else exitTryExpressionNode
        addEdge(catchExitNode, nextNode, propagateDeadness = false)
        return catchExitNode
    }

    fun enterFinallyBlock(): FinallyBlockEnterNode {
        return finallyEnterNodes.pop().also {
            lastNodes.push(it)
            finallyBlocksInProgress.push(it)
        }
    }

    fun exitFinallyBlock(): FinallyBlockExitNode {
        val enterNode = finallyBlocksInProgress.pop()
        val tryExitNode = tryExitNodes.top()
        val exitNode = createFinallyBlockExitNode(enterNode.fir)
        popAndAddEdge(exitNode)
        val allNormalInputsAreDead = enterNode.previousNodes.all {
            val edge = enterNode.edgeFrom(it)
            edge.kind.isDead || edge.label != NormalPath
        }
        addEdge(exitNode, tryExitNode, isDead = allNormalInputsAreDead)
        // TODO: there should also be edges to outer catch blocks? Control flow can go like this:
        //   try { try { throw E2() } catch (e: E1) { } finally { } } catch (e: E2) { }
        //                        \-----------------------------^ \-----------------^
        //  Wait, that's just `addExceptionEdgesFrom(exitNode)` again!
        val nextExitLevel = exitTargetsForTry.top().level
        val nextFinally = finallyEnterNodes.topOrNull()?.takeIf { it.level > nextExitLevel }
        if (nextFinally != null) {
            addEdge(exitNode, nextFinally, label = UncaughtExceptionPath, propagateDeadness = false)
        }

        val nextFinallyOrExitLevel = nextFinally?.level ?: nextExitLevel
        //                   /-----------v
        // f@ { try { return@f } finally { b }; c }
        //                                   \-----^
        exitNode.addReturnEdges(exitTargetsForReturn, nextFinallyOrExitLevel)
        //                               /-----------v
        // f@ while (x) { try { continue@f } finally { b }; c }
        //          ^------------------------------------/
        exitNode.addReturnEdges(loopConditionEnterNodes, nextFinallyOrExitLevel)
        //                            /-----------v
        // f@ while (x) { try { break@f } finally { b }; c }
        //                                            \-----^
        exitNode.addReturnEdges(loopExitNodes, nextFinallyOrExitLevel)
        return exitNode
    }

    private fun <T : CFGNode<*>> CFGNode<*>.addReturnEdges(nodes: Stack<T>, minLevel: Int) {
        for (node in nodes.all()) {
            when {
                node.level < minLevel -> break
                // TODO: this check is imprecise and can add redundant edges:
                //   x@{ try { return@x } finally {}; try {} finally { /* return@x target is in nonDirectJumps */ }
                node !in nonDirectJumps -> continue
                // TODO: if the input to finally with that label is dead, then so should be the exit probably
                node.returnPathIsBackwards -> addBackEdge(this, node, label = node.returnPathLabel)
                else -> addEdge(this, node, propagateDeadness = false, label = node.returnPathLabel)
            }
        }
    }

    fun exitTryExpression(callCompleted: Boolean): TryExpressionExitNode {
        levelCounter--
        notCompletedFunctionCalls.pop().forEach(::completeFunctionCall)
        val node = tryExitNodes.pop()
        mergeDataFlowFromPostponedLambdas(node, callCompleted)
        node.updateDeadStatus()
        lastNodes.push(node)
        return node
    }

    // TODO: these edges are true for literally any node in the graph. Their existence for *some* nodes might lead
    //  to a false sense of security, but things are broken. This should be some sort of implicit knowledge instead
    //  of requiring a ton of edges? (Some nodes never throw, but calls are never safe, and most useful stuff is calls.)
    //    var x: Any?
    //    x = ""
    //    try {
    //      x = null
    //      listOf(1, 2, 3).single()
    //      x = ""
    //    } catch (e: Throwable) { x.length } // oops
    //  R8 devs say they tried the "implicit knowledge" way but failed and decided to add all the edges - bad sign...
    private fun addExceptionEdgesFrom(node: CFGNode<*>) {
        val nextCatch = catchNodes.topOrNull()
        if (nextCatch != null) {
            for (catchEnterNode in nextCatch) {
                addEdge(node, catchEnterNode, propagateDeadness = false)
            }
        }
        val nextFinally = finallyEnterNodes.topOrNull()
        if (nextFinally != null && nextFinally.level > exitTargetsForTry.top().level) {
            addEdge(node, nextFinally, propagateDeadness = false, label = UncaughtExceptionPath)
        }
    }

    //this is a workaround to make function call dead when call is completed _after_ building its node in the graph
    //this happens when completing the last call in try/catch blocks
    //todo this doesn't make fully 'right' Nothing node (doesn't support going to catch and pass through finally)
    // because doing those afterwards is quite challenging
    // it would be much easier if we could build calls after full completion only, at least for Nothing calls
    private fun completeFunctionCall(node: FunctionCallNode) {
        if (!node.fir.resultType.isNothing) return
        val stub = withLevelOfNode(node) { createStubNode() }
        val edges = node.followingNodes.map { it to node.edgeTo(it) }
        CFGNode.removeAllOutgoingEdges(node)
        CFGNode.addEdge(node, stub, EdgeKind.DeadForward, propagateDeadness = false)
        for ((to, edge) in edges) {
            val kind = if (edge.kind.isBack) EdgeKind.DeadBackward else EdgeKind.DeadForward
            CFGNode.addEdge(stub, to, kind, propagateDeadness = false, label = edge.label)
            to.updateDeadStatus()
            propagateDeadnessForward(to)
        }
    }

    // ----------------------------------- Resolvable call -----------------------------------

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression): QualifiedAccessNode {
        val returnsNothing = qualifiedAccessExpression.resultType.isNothing
        val node = createQualifiedAccessNode(qualifiedAccessExpression)
        if (returnsNothing) {
            addNonSuccessfullyTerminatingNode(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
    }

    fun exitSmartCastExpression(smartCastExpression: FirSmartCastExpression): SmartCastExpressionExitNode {
        val returnsNothing = smartCastExpression.resultType.isNothing
        val node = createSmartCastExitNode(smartCastExpression)
        if (returnsNothing) {
            addNonSuccessfullyTerminatingNode(node)
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

    fun enterCallArguments(fir: FirStatement, anonymousFunctions: List<FirAnonymousFunction>) {
        if (anonymousFunctions.isEmpty()) {
            argumentListSplitNodes.push(null)
        } else {
            val splitNode = createSplitPostponedLambdasNode(fir, anonymousFunctions)
            anonymousFunctions.associateTo(postponedAnonymousFunctionNodes) { it.symbol to (splitNode to null) }
            argumentListSplitNodes.push(splitNode)
        }
    }

    fun exitCallArguments(): SplitPostponedLambdasNode? {
        return argumentListSplitNodes.pop()?.also { addNewSimpleNode(it) }
    }

    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean): FunctionCallNode {
        levelCounter--
        val returnsNothing = functionCall.resultType.isNothing
        val node = createFunctionCallNode(functionCall)
        unifyDataFlowFromPostponedLambdas(node, callCompleted)
        if (returnsNothing) {
            addNonSuccessfullyTerminatingNode(node)
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
        return createThrowExceptionNode(throwExpression).also { addNonSuccessfullyTerminatingNode(it) }
    }

    fun exitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, callCompleted: Boolean): CheckNotNullCallNode {
        levelCounter--
        val node = createCheckNotNullCallNode(checkNotNullCall)
        unifyDataFlowFromPostponedLambdas(node, callCompleted)
        if (checkNotNullCall.resultType.isNothing) {
            addNonSuccessfullyTerminatingNode(node)
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

    fun exitSafeCall(): ExitSafeCallNode {
        // There will be two paths towards this exit safe call node:
        // one from the node prior to the enclosing safe call, and
        // the other from the selector part in the enclosing safe call.
        // Note that *neither* points to the safe call directly.
        // So, when it comes to the real exit of the enclosing block/function,
        // the safe call bound to this exit safe call node should be retrieved.
        return exitSafeCallNodes.pop().also {
            addNewSimpleNode(it)
            // Safe calls only have one user-specified branch, so if any lambdas were postponed, they still are.
            mergeDataFlowFromPostponedLambdas(it, callCompleted = false)
            it.updateDeadStatus()
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

    fun exitElvis(lhsIsNotNull: Boolean, callCompleted: Boolean): ElvisExitNode {
        val exitNode = exitElvisExpressionNodes.pop()
        addNewSimpleNode(exitNode, isDead = lhsIsNotNull)
        mergeDataFlowFromPostponedLambdas(exitNode, callCompleted)
        exitNode.updateDeadStatus()
        return exitNode
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
        postponedLambdaExits.reset()
        lastNodes.reset()
    }

    // ----------------------------------- Edge utils -----------------------------------

    private fun addNewSimpleNode(node: CFGNode<*>, isDead: Boolean = false) {
        addEdge(lastNodes.pop(), node, preferredKind = if (isDead) EdgeKind.DeadForward else EdgeKind.Forward)
        lastNodes.push(node)
    }

    private fun addNewSimpleNodeIfPossible(newNode: CFGNode<*>) {
        if (lastNodes.isEmpty) return
        addNewSimpleNode(newNode)
    }

    private fun addNonTerminatingNode(node: CFGNode<*>) {
        popAndAddEdge(node)
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    private fun addNonSuccessfullyTerminatingNode(node: CFGNode<*>) {
        addNonTerminatingNode(node)
        addExceptionEdgesFrom(node)
    }

    private fun popAndAddEdge(to: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Forward) {
        addEdge(lastNodes.pop(), to, preferredKind = preferredKind)
    }

    private fun addEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        isDead: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward,
        label: EdgeLabel = NormalPath
    ) {
        val kind = if (isDead || from.isDead || to.isDead) {
            if (preferredKind.isBack) EdgeKind.DeadBackward else EdgeKind.DeadForward
        } else preferredKind
        CFGNode.addEdge(from, to, kind, propagateDeadness, label)
    }

    private fun addBackEdge(from: CFGNode<*>, to: CFGNode<*>, isDead: Boolean = false, label: EdgeLabel = LoopBackPath) {
        val kind = if (isDead || from.isDead || to.isDead) EdgeKind.DeadBackward else EdgeKind.CfgBackward
        CFGNode.addEdge(from, to, kind, propagateDeadness = false, label = label)
    }

    private fun propagateDeadnessForward(node: CFGNode<*>) {
        if (!node.isDead) return
        for (next in node.followingNodes) {
            val kind = node.edgeTo(next).kind
            if (CFGNode.killEdge(node, next, propagateDeadness = false) && !kind.isBack && kind.usedInCfa) {
                next.updateDeadStatus()
                propagateDeadnessForward(next)
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
