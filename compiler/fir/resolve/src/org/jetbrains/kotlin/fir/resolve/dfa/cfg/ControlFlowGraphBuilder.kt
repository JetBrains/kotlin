/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.fir.util.listMultimapOf
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.getOrPutNullable

data class FirAnonymousFunctionReturnExpressionInfo(val expression: FirExpression, val isExplicit: Boolean)

@OptIn(CfgInternals::class)
class ControlFlowGraphBuilder {
    private val graphs: Stack<ControlFlowGraph> = stackOf()

    val isTopLevel: Boolean
        get() = graphs.isEmpty || graphs.topOrNull()?.kind == ControlFlowGraph.Kind.File

    val currentGraph: ControlFlowGraph
        get() = graphs.top()

    private val bodyBuildingMode: Boolean
        get() = graphs.isNotEmpty && currentGraph.kind != ControlFlowGraph.Kind.Class

    val levelCounter: Int
        // `try` expressions aren't subgraphs, but they increase the level in order to tell which nodes
        // are inside the try and which aren't
        get() = graphs.size + tryExitNodes.size

    private val lastNodes: Stack<CFGNode<*>> = stackOf()
    val lastNode: CFGNode<*>
        get() = lastNodes.top()

    val lastNodeOrNull: CFGNode<*>?
        get() = lastNodes.topOrNull()

    // ----------------------------------- Node caches -----------------------------------

    private val exitTargetsForReturn: MutableMap<FirFunctionSymbol<*>, FunctionExitNode> = mutableMapOf()
    private val enterToLocalClassesMembers: MutableMap<FirBasedSymbol<*>, Pair<CFGNode<*>, EdgeKind>> = mutableMapOf()

    //return jumps via finally blocks, target -> jumps
    private val nonDirectJumps: ListMultimap<CFGNode<*>, JumpNode> = listMultimapOf()

    private val argumentListSplitNodes: Stack<SplitPostponedLambdasNode?> = stackOf()
    private val postponedAnonymousFunctionNodes =
        mutableMapOf<FirFunctionSymbol<*>, Pair<CFGNode<*>, PostponedLambdaExitNode?>>()
    private val postponedLambdaExits: Stack<PostponedLambdas> = stackOf()

    private val loopConditionEnterNodes: MutableMap<FirLoop, LoopConditionEnterNode> = mutableMapOf()
    private val loopExitNodes: MutableMap<FirLoop, LoopExitNode> = mutableMapOf()

    private val whenExitNodes: Stack<WhenExitNode> = stackOf()

    private val tryExitNodes: Stack<TryExpressionExitNode> = stackOf()
    private val catchNodes: Stack<List<CatchClauseEnterNode>> = stackOf()
    private val catchBlocksInProgress: Stack<CatchClauseEnterNode> = stackOf()
    private val finallyEnterNodes: Stack<FinallyBlockEnterNode> = stackOf()
    private val finallyBlocksInProgress: Stack<FinallyBlockEnterNode> = stackOf()
    private val finallyBlocksInProgressSet = mutableSetOf<FirElement>()

    private val exitFunctionCallArgumentsNodes: Stack<FunctionCallArgumentsExitNode?> = stackOf()
    private val exitSafeCallNodes: Stack<ExitSafeCallNode> = stackOf()
    private val exitElvisExpressionNodes: Stack<ElvisExitNode> = stackOf()
    private val elvisRhsEnterNodes: Stack<ElvisRhsEnterNode> = stackOf()
    private val equalityOperatorCallLhsExitNodes: Stack<CFGNode<*>> = stackOf()

    private val notCompletedFunctionCalls: Stack<MutableList<FunctionCallNode>> = stackOf()

    // ----------------------------------- Public API -----------------------------------

    fun withinFinallyBlock(element: FirElement): Boolean {
        return finallyBlocksInProgressSet.contains(element)
    }

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirAnonymousFunctionReturnExpressionInfo>? {
        val exitNode = function.controlFlowGraphReference?.controlFlowGraph?.exitNode ?: return null

        fun CFGNode<*>.returnExpression(): FirAnonymousFunctionReturnExpressionInfo? = when (this) {
            is BlockExitNode -> when {
                // lambda@{ x } -> x
                // lambda@{ class C } -> Unit-returning stub
                function.isLambda -> {
                    val lastStatement = fir.statements.lastOrNull()

                    when {
                        // Skip last return statement because otherwise it add Nothing constraint on the lambda return type.
                        // That might lead to preliminary variable fixation to Nothing that is kind of undesirable in most cases.
                        // Note, that the expression that is going to be returned would be used as another element of `returnValues`.
                        //
                        // While that remains a bit questionable why adding such trivial Nothing constraint makes variable being fixed there
                        // but currently it doesn't look like we've got easy answers to those questions, so we just repeat K1 behavior
                        // (see `val lastExpressionArgument` at KotlinResolutionCallbacksImpl.analyzeAndGetLambdaReturnArguments)
                        // Probably, that might be removed once KT-58232 is fixed
                        lastStatement is FirReturnExpression &&
                                lastStatement.target.labeledElement.symbol == function.symbol &&
                                lastStatement.source?.kind != KtFakeSourceElementKind.ImplicitReturn.FromLastStatement ->
                            null
                        else ->
                            (lastStatement as? FirExpression
                                ?: buildUnitExpression { source = fir.statements.lastOrNull()?.source ?: fir.source }).let {
                                FirAnonymousFunctionReturnExpressionInfo(it, isExplicit = false)
                            }
                    }
                }
                // fun() { terminatingExpression } -> nothing (checker will emit an error if return type is not Unit)
                // fun() { throw } or fun() { returnsNothing() } -> Nothing-returning stub
                else -> FirStub.takeIf { _ -> previousNodes.all { it is StubNode } }
                    ?.let { FirAnonymousFunctionReturnExpressionInfo(it, isExplicit = false) }
            }
            // lambda@{ return@lambda x } -> x
            is JumpNode -> (fir as? FirReturnExpression)?.takeIf { it.target.labeledElement.symbol == function.symbol }?.result?.let {
                FirAnonymousFunctionReturnExpressionInfo(it, isExplicit = true)
            }
            else -> null // shouldn't happen? expression bodies are implicitly wrapped in `FirBlock`s
        }

        val returnValues = exitNode.previousNodes.mapNotNullTo(mutableSetOf()) {
            val edge = exitNode.edgeFrom(it)
            // * NormalPath: last expression = return value
            // * UncaughtExceptionPath: last expression = whatever threw, *not* a return value
            // * Other labels can only originate from finally block exits - look in nonDirectJumps to find the last node
            //   before the block was entered
            it.takeIf { edge.kind.usedInCfa && edge.label == NormalPath }?.returnExpression()
        }
        return nonDirectJumps[exitNode].mapNotNullTo(returnValues) { it.returnExpression() }
    }

    // ----------------------------------- Utils -----------------------------------

    private inline fun <T, E : T, EnterNode, ExitNode> enterGraph(
        fir: E,
        name: String,
        kind: ControlFlowGraph.Kind,
        nodes: (E) -> Pair<EnterNode, ExitNode>
    ): EnterNode where EnterNode : CFGNode<T>, EnterNode : GraphEnterNodeMarker, ExitNode : CFGNode<T>, ExitNode : GraphExitNodeMarker {
        val graph = ControlFlowGraph(fir as? FirDeclaration, name, kind).also { graphs.push(it) }
        val (enterNode, exitNode) = nodes(fir)
        graph.enterNode = enterNode
        graph.exitNode = exitNode
        lastNodes.push(enterNode)
        return enterNode
    }

    private fun popGraph(): ControlFlowGraph {
        return graphs.pop().also { it.complete() }
    }

    private inline fun <reified ExitNode> exitGraph(): Pair<ExitNode, ControlFlowGraph> where ExitNode : CFGNode<*>, ExitNode : GraphExitNodeMarker {
        val graph = graphs.pop()
        val exitNode = graph.exitNode as ExitNode
        popAndAddEdge(exitNode)
        if (exitNode.previousNodes.size > 1) {
            exitNode.updateDeadStatus()
        }
        graph.complete()
        return exitNode to graph
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

        val localFunctionNode = runIf(function is FirSimpleFunction && function.isLocal && bodyBuildingMode) {
            createLocalFunctionDeclarationNode(function).also { addNewSimpleNode(it) }
        }
        val kind = when {
            localFunctionNode != null -> ControlFlowGraph.Kind.LocalFunction
            function is FirConstructor -> ControlFlowGraph.Kind.Constructor
            else -> ControlFlowGraph.Kind.Function
        }
        val enterNode = enterGraph(function, name, kind) {
            createFunctionEnterNode(it) to createFunctionExitNode(it).also { exit -> exitTargetsForReturn[it.symbol] = exit }
        }
        if (localFunctionNode != null) {
            addEdge(localFunctionNode, enterNode)
        } else {
            addEdgeIfLocalClassMember(enterNode)
        }
        return Pair(localFunctionNode, enterNode)
    }

    fun exitFunction(function: FirFunction): Pair<FunctionExitNode, ControlFlowGraph> {
        require(function !is FirAnonymousFunction)
        exitTargetsForReturn.remove(function.symbol)
        return exitGraph()
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
        // that the function won't be visited for so long, we'll exit the current graph before that.
        // So we need an edge right now to enforce ordering, and mark it as dead later if needed.
        addEdge(enterNode, exitNode)
        postponedAnonymousFunctionNodes[symbol] = enterNode to exitNode
        postponedLambdaExits.top().exits.add(exitNode to EdgeKind.Forward)
        return null
    }

    fun enterAnonymousFunction(anonymousFunction: FirAnonymousFunction): FunctionEnterNode {
        val graphKind = if (anonymousFunction.invocationKind.isInPlace)
            ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace
        else
            ControlFlowGraph.Kind.AnonymousFunction
        return enterGraph(anonymousFunction, "<anonymous>", graphKind) {
            createFunctionEnterNode(it) to createFunctionExitNode(it).also { exit -> exitTargetsForReturn[anonymousFunction.symbol] = exit }
        }.also { addEdge(postponedAnonymousFunctionNodes.getValue(anonymousFunction.symbol).first, it) }
    }

    fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): Triple<FunctionExitNode, PostponedLambdaExitNode?, ControlFlowGraph> {
        exitTargetsForReturn.remove(anonymousFunction.symbol)
        val (exitNode, graph) = exitGraph<FunctionExitNode>()
        val (splitNode, postponedExitNode) = postponedAnonymousFunctionNodes.remove(anonymousFunction.symbol)!!
        val invocationKind = anonymousFunction.invocationKind
        if (postponedExitNode == null) {
            // Postponed exit node was needed so we could create lambda->call edges without having the subgraph ready. If it
            // doesn't exist, then we probably can't do that anymore, and the lambda won't be called-in-place in the CFG.
            // TODO: verify & enable this assertion?
            // assert(invocationKind?.canBeVisited() != true) { "no exit node for calledInPlace($invocationKind) lambda" }
            return Triple(exitNode, null, graph)
        }

        // Lambdas not called in-place behave as if called never, but with extra invalidation of all smart casts
        // for all variables that they reassign. That second part is handled by `FirDataFlowAnalyzer`.
        val isDefinitelyVisited = invocationKind?.isDefinitelyVisited() == true
        if (isDefinitelyVisited || splitNode.isDead) {
            // The edge that was added to enforce ordering of nodes needs to be marked as dead if this lambda is never
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

    private fun splitDataFlowForPostponedLambdas(lambdas: Set<FirFunctionSymbol<*>> = emptySet()) {
        postponedLambdaExits.push(PostponedLambdas(lambdas))
    }

    /**
     * Pop and add the current level exits (if any) to the exit corresponding with the specified
     * lambda function symbol. This is used when a postponed lambda is present within a return
     * statement for an outer lambda and data-flow information needs to be preserved.
     */
    private fun jumpDataFlowFromPostponedLambdas(symbol: FirFunctionSymbol<*>) {
        val currentLevelExits = postponedLambdaExits.pop().exits
        if (currentLevelExits.isEmpty()) return

        for ((lambdas, exits) in postponedLambdaExits.all()) {
            if (symbol in lambdas) {
                exits.addAll(currentLevelExits)
                break
            }
        }
    }

    private fun unifyDataFlowFromPostponedLambdas(node: CFGNode<*>, callCompleted: Boolean) {
        val currentLevelExits = postponedLambdaExits.pop().exits
        if (currentLevelExits.isEmpty()) return

        val nextLevelExits = postponedLambdaExits.topOrNull()?.exits.takeIf { !callCompleted }
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
                if (kind.usedInCfa || !exit.isDead) {
                    // Since `node` is a union node, it is dead iff any input is dead. For once, `propagateDeadness`
                    // semantics are correct without an `updateDeadStatus`.
                    addEdge(exit, node, label = PostponedPath, preferredKind = kind)
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
    //   exit node, or create N union nodes (1/branch) and point them into the merge node. KT-59730
    private fun mergeDataFlowFromPostponedLambdas(node: CFGNode<*>, callCompleted: Boolean) {
        val currentLevelExits = postponedLambdaExits.pop().exits
        if (currentLevelExits.isEmpty()) return

        val nextLevelExits = postponedLambdaExits.topOrNull()?.exits.takeIf { !callCompleted }
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
                addEdge(exit, node, label = PostponedPath, preferredKind = kind, propagateDeadness = false)
            }
        }
    }

    // ----------------------------------- Files -----------------------------------

    fun enterFile(file: FirFile, buildGraph: Boolean): FileEnterNode? {
        if (!buildGraph) {
            graphs.push(ControlFlowGraph(declaration = null, "<discarded file graph>", ControlFlowGraph.Kind.File))
            return null
        }

        return enterGraph(file, "FILE_GRAPH", ControlFlowGraph.Kind.File) {
            createFileEnterNode(it) to createFileExitNode(it)
        }
    }

    fun exitFile(): Pair<FileExitNode?, ControlFlowGraph?> {
        assert(currentGraph.kind == ControlFlowGraph.Kind.File)
        if (currentGraph.declaration == null) {
            graphs.pop() // Discard empty file graph.
            return null to null
        }

        // Properties of a file can be visited in any order, so data flow between them is unordered,
        // and we have to recreate the control flow after the fact.
        val enterNode = lastNodes.pop() as FileEnterNode
        val exitNode = currentGraph.exitNode as FileExitNode

        val properties = mutableListOf<ControlFlowGraph>()
        enterNode.fir.declarations.forEachGraphOwner {
            val graph = it.controlFlowGraphReference?.controlFlowGraph ?: return@forEachGraphOwner
            if (it is FirProperty) properties.add(graph)
        }

        var lastNode: CFGNode<*> = enterNode
        for (property in properties) {
            // Top-level property CFGs should never be linked with dead edges.
            CFGNode.addEdge(lastNode, property.enterNode, kind = EdgeKind.CfgForward, propagateDeadness = false)
            lastNode = property.exitNode
        }

        addEdge(lastNode, exitNode, preferredKind = EdgeKind.CfgForward, propagateDeadness = false)
        if (properties.isNotEmpty()) {
            // Fake edge to enforce ordering.
            addEdge(enterNode, exitNode, preferredKind = EdgeKind.DeadForward, propagateDeadness = false)
        }

        enterNode.subGraphs = properties
        return exitNode to popGraph()
    }

    // ----------------------------------- Classes -----------------------------------

    /**
     * The first in-place initializer is either:
     * 1. The primary constructor.
     * 2. The first property or anonymous initializer.
     */
    private fun FirClass.firstInPlaceInitializer(): FirDeclaration? {
        return declarations.find {
            it is FirControlFlowGraphOwner &&
                    (it !is FirConstructor || it.isPrimary) &&
                    it.isUsedInControlFlowGraphBuilderForClass
        }
    }

    private inline fun List<FirElement>.forEachGraphOwner(block: (FirControlFlowGraphOwner) -> Unit) {
        for (member in this) {
            if (member is FirControlFlowGraphOwner && member.memberShouldHaveGraph) {
                block(member)
            }
            if (member is FirProperty) {
                member.getter?.let { block(it) }
                member.setter?.let { block(it) }
            }
        }
    }

    private fun <E : FirDeclaration> addEdgeIfLocalClassMember(enterNode: CFGNode<E>) {
        val (source, kind) = enterToLocalClassesMembers.remove(enterNode.fir.symbol) ?: return
        addEdge(source, enterNode, preferredKind = kind)
    }

    fun enterClass(klass: FirClass, buildGraph: Boolean): Pair<CFGNode<*>?, ClassEnterNode?> {
        if (!buildGraph) {
            graphs.push(ControlFlowGraph(declaration = null, "<discarded class graph>", ControlFlowGraph.Kind.Class))
            return null to null
        }

        val localClassEnterNode = when {
            klass is FirAnonymousObject && klass.classKind != ClassKind.ENUM_ENTRY -> createAnonymousObjectEnterNode(klass)
            // Local classes are only initialized on first use, so they look pretty much like named functions:
            // control flow enters here and never leaves, and assignments invalidate smart casts.
            klass is FirRegularClass && klass.isLocal && bodyBuildingMode -> createLocalClassExitNode(klass)
            else -> null
        }?.also { addNewSimpleNode(it) }

        val name = when (klass) {
            is FirAnonymousObject -> "<anonymous object>"
            is FirRegularClass -> klass.name.asString()
            else -> throw IllegalArgumentException("Unknown class kind: ${klass::class}")
        }

        val enterNode = enterGraph(klass, name, ControlFlowGraph.Kind.Class) {
            createClassEnterNode(it) to createClassExitNode(it)
        }
        if (localClassEnterNode != null) {
            addEdge(localClassEnterNode, enterNode)
        } else {
            addEdgeIfLocalClassMember(enterNode)
        }

        if (enterNode.previousNodes.isNotEmpty()) {
            val firstInPlace = klass.firstInPlaceInitializer()
            klass.declarations.forEachGraphOwner {
                // For local classes,
                //  - the first in-place initializer,
                //  - all constructors when there are no in-place initializers,
                //  - or any this-delegating constructors,
                // should have a forward edge from ClassEnterNode.
                // Everything else should have a DFG-only forward edge.
                val kind = when {
                    firstInPlace == it -> EdgeKind.Forward
                    it is FirConstructor && (firstInPlace == null || it.delegatedConstructor?.isThis == true) -> EdgeKind.Forward
                    else -> EdgeKind.DfgForward
                }
                enterToLocalClassesMembers[(it as FirDeclaration).symbol] = enterNode to kind
            }
        }
        return localClassEnterNode to enterNode
    }

    fun exitClass(): Pair<ClassExitNode?, ControlFlowGraph?> {
        assert(currentGraph.kind == ControlFlowGraph.Kind.Class)
        if (currentGraph.declaration == null) {
            graphs.pop()
            return null to null
        }

        // Members of a class can be visited in any order, so data flow between them is unordered,
        // and we have to recreate the control flow after the fact.
        val enterNode = lastNodes.pop() as ClassEnterNode
        val exitNode = currentGraph.exitNode as ClassExitNode
        val klass = enterNode.fir
        if ((klass as FirControlFlowGraphOwner).controlFlowGraphReference != null) {
            // TODO: IDE LL API sometimes attempts to analyze a enum class while already analyzing it, causing
            //  this graph to be built twice (or more). Not sure what this means. Nothing good, probably.
            //  In any case, attempting to add more edges to subgraphs will be fatal. KT-59728
            graphs.pop()
            return null to null
        }

        val isLocalClass = klass.isLocal
        var primaryConstructor: ControlFlowGraph? = null
        val calledInPlace = mutableListOf<ControlFlowGraph>()
        val secondaryConstructors = mutableMapOf<FirConstructor, ControlFlowGraph>()
        val calledLater = mutableListOf<ControlFlowGraph>()
        klass.declarations.forEachGraphOwner {
            val graph = it.controlFlowGraphReference?.controlFlowGraph ?: return@forEachGraphOwner
            when (it) {
                is FirConstructor -> when {
                    it.isPrimary -> {
                        // The primary constructor is treated as an in-place initializer since it is always called, but a specific
                        // reference needs to be saved to inject into any secondary constructor delegation.
                        calledInPlace.add(graph)
                        primaryConstructor = graph
                    }
                    else -> secondaryConstructors[it] = graph
                }

                is FirPropertyAccessor, is FirFunction, is FirClass -> if (isLocalClass) {
                    calledLater.add(graph)
                }

                else -> calledInPlace.add(graph)
            }
        }

        // Create primary constructor and in-place initializer edges.
        val firstInPlaceEnter = calledInPlace.firstOrNull()?.enterNode
        val lastInPlaceExit = calledInPlace.fold<_, CFGNode<*>>(enterNode) { lastNode, graph ->
            // In local classes, we already have control flow (+ data flow) edge from `enterNode`
            // to primary constructor or first in-place initializer.
            if (lastNode !== enterNode || lastNode.previousNodes.isEmpty()) {
                addEdgeToSubGraph(lastNode, graph.enterNode)
            }
            graph.exitNode
        }

        for (graph in calledInPlace) {
            EdgeKind.forward(
                // => this class has a primary constructor => the last in-place initializer must have a
                // control flow edge to the exit node.
                usedInCfa = primaryConstructor != null && graph.exitNode == lastInPlaceExit,
                // => this is an anonymous object => there's only one constructor => can use `exitNode`
                // to unify data flow from all in-place-called members, including said constructor.
                usedInDfa = exitNode.isUnion,
            )?.let { edgeKind -> addEdge(graph.exitNode, exitNode, preferredKind = edgeKind) }
        }

        // Create secondary constructor edges.
        val parentConstructors = mutableMapOf<FirConstructor, FirConstructor?>()
        fun FirConstructor.parentConstructor(): FirConstructor? = parentConstructors.getOrPutNullable(this) {
            // Break cycles in some way; there will be errors on delegated constructor calls in that case.
            parentConstructors[this] = null
            delegatedConstructor?.takeIf { it.isThis }?.calleeReference
                ?.toResolvedConstructorSymbol(discardErrorReference = true)?.fir
                ?.takeIf { parent -> this !in generateSequence(parent) { it.parentConstructor() } }
        }

        fun getDelegateNodes(ctor: FirConstructor): Pair<CFGNode<FirElement>?, CFGNode<FirElement>?> {
            val parentConstructor = ctor.parentConstructor()
            val secondaryGraph = secondaryConstructors[parentConstructor]
            return when {
                secondaryGraph != null -> (firstInPlaceEnter ?: secondaryGraph.enterNode) to secondaryGraph.exitNode
                primaryConstructor != null && primaryConstructor == parentConstructor?.controlFlowGraphReference?.controlFlowGraph -> firstInPlaceEnter to lastInPlaceExit
                else -> null to null
            }
        }

        for ((ctor, graph) in secondaryConstructors) {
            val (delegatedEnter, delegatedExit) = getDelegateNodes(ctor)
            if (delegatedEnter != null && delegatedExit != null) {
                // Inject delegated constructor and other in-place initializer sub-graphs after the delegated constructor call node. This
                // ensures property initialization and use is calculated correctly when there are complex calculations for the arguments to
                // the delegated constructor.

                val delegatedConstructorCall = graph.nodes.single { it is DelegatedConstructorCallNode }
                val edgeLabel = graph.exitNode as FunctionExitNode

                val followingNodes = delegatedConstructorCall.followingNodes.toList()
                CFGNode.removeAllOutgoingEdges(delegatedConstructorCall)

                if (!isLocalClass) addEdgeToSubGraph(enterNode, graph.enterNode)

                addEdgeToSubGraph(delegatedConstructorCall, delegatedEnter, label = edgeLabel)
                for (node in followingNodes) {
                    addEdge(delegatedExit, node, preferredKind = EdgeKind.CfgForward, label = edgeLabel)
                    addEdge(delegatedConstructorCall, node, preferredKind = EdgeKind.DfgForward)
                }
            } else if (lastInPlaceExit !== enterNode || lastInPlaceExit.previousNodes.isEmpty()) {
                // Similarly, if there are no in-place initializers, we already have control flow (+ data flow) edges
                // from `enterNode` to all non-delegating constructors in local classes.
                addEdgeToSubGraph(lastInPlaceExit, graph.enterNode)
            }
            addEdge(graph.exitNode, exitNode, preferredKind = if (exitNode.isUnion) EdgeKind.Forward else EdgeKind.CfgForward)
        }

        if (primaryConstructor == null && secondaryConstructors.isEmpty()) {
            // Interfaces have no constructors, add an edge from enter to exit so that methods aren't marked as dead.
            addEdge(enterNode, exitNode, preferredKind = EdgeKind.CfgForward)
        } else {
            // Fake edge to enforce ordering.
            addEdge(enterNode, exitNode, preferredKind = EdgeKind.DeadForward, propagateDeadness = false)
        }

        // Here we're assuming that the methods are called after the object is constructed, which is really not true
        //   But it's fine, since Kotlin intentionally does not support analysis of "leaked this" bugs
        for (graph in calledLater) {
            addEdgeToSubGraph(exitNode, graph.enterNode)
        }

        enterNode.subGraphs = calledInPlace + secondaryConstructors.values
        exitNode.subGraphs = calledLater
        return exitNode.takeIf { it.isUnion } to popGraph()
    }

    fun exitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression): AnonymousObjectExpressionExitNode? {
        val klass = anonymousObjectExpression.anonymousObject
        if (klass.classKind == ClassKind.ENUM_ENTRY) return null

        return createAnonymousObjectExpressionExitNode(anonymousObjectExpression).also {
            val exitNode = klass.controlFlowGraphReference?.controlFlowGraph?.exitNode
            if (exitNode != null && lastNode is AnonymousObjectEnterNode) {
                addEdge(exitNode, it)
                // Fake edge to enforce ordering.
                addEdge(lastNodes.pop(), it, preferredKind = EdgeKind.DeadForward, propagateDeadness = false)
                lastNodes.push(it)
            } else {
                addNewSimpleNode(it)
            }
        }
    }

    // ----------------------------------- Scripts -----------------------------------

    fun enterScript(script: FirScript, buildGraph: Boolean): ScriptEnterNode? {
        if (!buildGraph) {
            graphs.push(ControlFlowGraph(declaration = null, "<discarded script graph>", ControlFlowGraph.Kind.Script))
            return null
        }
        return enterGraph(script, script.name.asString(), ControlFlowGraph.Kind.Script) {
            createScriptEnterNode(it) to createScriptExitNode(it)
        }
    }

    fun exitScript(): Pair<ScriptExitNode?, ControlFlowGraph?> {
        require(currentGraph.kind == ControlFlowGraph.Kind.Script)
        if (currentGraph.declaration == null) {
            graphs.pop() // Discard empty script graph.
            return null to null
        }

        val enterNode = lastNodes.pop() as ScriptEnterNode
        val exitNode = currentGraph.exitNode as ScriptExitNode

        val script = enterNode.fir
        requireWithAttachment(
            (script as FirControlFlowGraphOwner).controlFlowGraphReference == null,
            { "Unexpected state: script already has a CFG attached" }
        ) {
            withFirEntry("script", script)
        }

        val calledInPlace = mutableListOf<ControlFlowGraph>()

        script.declarations.forEachGraphOwner {
            val graph = it.controlFlowGraphReference?.controlFlowGraph ?: return@forEachGraphOwner
            if (it.isUsedInControlFlowGraphBuilderForScript) {
                calledInPlace.add(graph)
            }
        }

        val lastNode = calledInPlace.fold<_, CFGNode<*>>(enterNode) { lastNode, graph ->
            if (lastNode !== enterNode || lastNode.previousNodes.isEmpty()) {
                addEdgeToSubGraph(lastNode, graph.enterNode)
            }
            graph.exitNode
        }

        addEdge(lastNode, exitNode, preferredKind = EdgeKind.CfgForward, propagateDeadness = false)
        if (calledInPlace.isNotEmpty()) {
            // Fake edge to enforce ordering.
            addEdge(enterNode, exitNode, preferredKind = EdgeKind.DeadForward, propagateDeadness = false)
        }

        enterNode.subGraphs = calledInPlace
        return exitNode to popGraph()
    }

    fun enterCodeFragment(codeFragment: FirCodeFragment): CodeFragmentEnterNode {
        return enterGraph(codeFragment, "CODE_FRAGMENT_GRAPH", ControlFlowGraph.Kind.Function) {
            createCodeFragmentEnterNode(it) to createCodeFragmentExitNode(it)
        }
    }

    fun exitCodeFragment(): Pair<CodeFragmentExitNode, ControlFlowGraph> {
        return exitGraph()
    }

    // ----------------------------------- Value parameters (and it's defaults) -----------------------------------

    fun enterValueParameter(valueParameter: FirValueParameter): Pair<EnterValueParameterNode, EnterDefaultArgumentsNode>? {
        if (valueParameter.defaultValue == null) return null

        val outerEnterNode = createEnterValueParameterNode(valueParameter).also { addNewSimpleNode(it) }
        val enterNode = enterGraph(valueParameter, "default value of ${valueParameter.name}", ControlFlowGraph.Kind.DefaultArgument) {
            createEnterDefaultArgumentsNode(it) to createExitDefaultArgumentsNode(it)
        }
        addEdge(outerEnterNode, enterNode)
        return outerEnterNode to enterNode
    }

    fun exitValueParameter(valueParameter: FirValueParameter): Triple<ExitDefaultArgumentsNode, ExitValueParameterNode, ControlFlowGraph>? {
        if (valueParameter.defaultValue == null) return null

        val (exitNode, graph) = exitGraph<ExitDefaultArgumentsNode>()
        val outerExitNode = createExitValueParameterNode(valueParameter)
        addNewSimpleNode(outerExitNode)
        addEdge(exitNode, outerExitNode, propagateDeadness = false)
        return Triple(exitNode, outerExitNode, graph)
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
        if (!property.memberShouldHaveGraph) return null
        return enterGraph(property, "val ${property.name}", ControlFlowGraph.Kind.PropertyInitializer) {
            createPropertyInitializerEnterNode(it) to createPropertyInitializerExitNode(it)
        }.also { addEdgeIfLocalClassMember(it) }
    }

    fun exitProperty(property: FirProperty): Pair<PropertyInitializerExitNode, ControlFlowGraph>? {
        if (!property.memberShouldHaveGraph) return null
        return exitGraph()
    }

    // ----------------------------------- Field -----------------------------------

    fun enterField(field: FirField): FieldInitializerEnterNode? {
        if (!field.memberShouldHaveGraph) return null
        return enterGraph(field, "val ${field.name}", ControlFlowGraph.Kind.FieldInitializer) {
            createFieldInitializerEnterNode(it) to createFieldInitializerExitNode(it)
        }.also { addEdgeIfLocalClassMember(it) }
    }

    fun exitField(field: FirField): Pair<FieldInitializerExitNode, ControlFlowGraph>? {
        if (!field.memberShouldHaveGraph) return null
        return exitGraph()
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

    fun exitEqualityOperatorLhs() {
        equalityOperatorCallLhsExitNodes.push(lastNode)
    }

    /**
     * Returns a pair of nodes, where the first is the last node of the LHS of the equality operator
     * call, and the second is the exit node of the equality operator call. This allows DFA to
     * determine if an assignment took place within the RHS of the equality operator call.
     */
    fun exitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall): Pair<CFGNode<*>, EqualityOperatorCallNode> {
        val lhsExitNode = equalityOperatorCallLhsExitNodes.pop()
        val node = createEqualityOperatorCallNode(equalityOperatorCall).also { addNewSimpleNode(it) }
        return lhsExitNode to node
    }

    // ----------------------------------- Jump -----------------------------------

    fun enterJump(jump: FirJump<*>) {
        if (jump is FirReturnExpression && jump.target.labeledElement is FirAnonymousFunction) {
            splitDataFlowForPostponedLambdas()
        }
    }

    fun exitJump(jump: FirJump<*>): JumpNode {
        val node = createJumpNode(jump)
        addNonSuccessfullyTerminatingNode(node)

        if (jump is FirReturnExpression && jump.target.labeledElement is FirAnonymousFunction) {
            jumpDataFlowFromPostponedLambdas(jump.target.labeledElement.symbol)
        }

        val nextNode = when (jump) {
            is FirReturnExpression -> exitTargetsForReturn[jump.target.labeledElement.symbol]
            is FirContinueExpression -> loopConditionEnterNodes[jump.target.labeledElement]
            is FirBreakExpression -> loopExitNodes[jump.target.labeledElement]
            else -> throw IllegalArgumentException("Unknown jump type: ${jump.render()}")
        } ?: return node
        val nextFinally = finallyEnterNodes.topOrNull()?.takeIf { it.level > nextNode.level }
        if (nextFinally != null) {
            addEdge(node, nextFinally, propagateDeadness = false, label = nextNode)
            nonDirectJumps.put(nextNode, node)
        } else if (nextNode.returnPathIsBackwards) {
            addBackEdge(node, nextNode)
        } else {
            addEdge(node, nextNode, propagateDeadness = false)
        }
        return node
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
        splitDataFlowForPostponedLambdas()
        return node
    }

    fun exitWhenSubjectExpression(expression: FirWhenSubjectExpression): WhenSubjectExpressionExitNode {
        return createWhenSubjectExpressionExitNode(expression).also { addNewSimpleNode(it) }
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch): WhenBranchConditionEnterNode {
        return createWhenBranchConditionEnterNode(whenBranch).also { addNewSimpleNode(it) }
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch): Pair<WhenBranchConditionExitNode, WhenBranchResultEnterNode> {
        val conditionExitNode = createWhenBranchConditionExitNode(whenBranch).also { addNewSimpleNode(it) }
        lastNodes.push(conditionExitNode) // keep one for next condition entry
        val branchEnterNode = createWhenBranchResultEnterNode(whenBranch).also { addNewSimpleNode(it) }
        return conditionExitNode to branchEnterNode
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch): WhenBranchResultExitNode {
        val node = createWhenBranchResultExitNode(whenBranch)
        popAndAddEdge(node)
        addEdge(node, whenExitNodes.top(), propagateDeadness = false)
        return node
    }

    fun exitWhenExpression(
        whenExpression: FirWhenExpression,
        callCompleted: Boolean
    ): Pair<WhenExitNode, WhenSyntheticElseBranchNode?> {
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
        return whenExitNode to syntheticElseBranchNode
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop): Pair<LoopEnterNode, LoopConditionEnterNode> {
        val loopEnterNode = createLoopEnterNode(loop).also { addNewSimpleNode(it) }
        loopExitNodes[loop] = createLoopExitNode(loop)
        val conditionEnterNode = createLoopConditionEnterNode(loop.condition, loop).also { addNewSimpleNode(it) }
        loopConditionEnterNodes[loop] = conditionEnterNode
        return loopEnterNode to conditionEnterNode
    }

    fun exitWhileLoopCondition(loop: FirLoop): Pair<LoopConditionExitNode, LoopBlockEnterNode> {
        val conditionExitNode = createLoopConditionExitNode(loop.condition, loop).also { addNewSimpleNode(it) }
        val conditionConstBooleanValue = loop.condition.booleanLiteralValue
        addEdge(conditionExitNode, loopExitNodes.getValue(loop), propagateDeadness = false, isDead = conditionConstBooleanValue == true)
        val loopBlockEnterNode = createLoopBlockEnterNode(loop)
        addNewSimpleNode(loopBlockEnterNode, conditionConstBooleanValue == false)
        return conditionExitNode to loopBlockEnterNode
    }

    fun exitWhileLoop(loop: FirLoop): Triple<LoopConditionEnterNode, LoopBlockExitNode, LoopExitNode> {
        val loopBlockExitNode = createLoopBlockExitNode(loop)
        popAndAddEdge(loopBlockExitNode)
        val conditionEnterNode = loopConditionEnterNodes.remove(loop)!!
        addBackEdge(loopBlockExitNode, conditionEnterNode)
        val loopExitNode = loopExitNodes.remove(loop)!!
        loopExitNode.updateDeadStatus()
        lastNodes.push(loopExitNode)
        return Triple(conditionEnterNode, loopBlockExitNode, loopExitNode)
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop): Pair<LoopEnterNode, LoopBlockEnterNode> {
        val loopEnterNode = createLoopEnterNode(loop).also { addNewSimpleNode(it) }
        loopExitNodes[loop] = createLoopExitNode(loop)
        val blockEnterNode = createLoopBlockEnterNode(loop).also { addNewSimpleNode(it) }
        lastNodes.push(blockEnterNode) // to add back edge at the end
        loopConditionEnterNodes[loop] = createLoopConditionEnterNode(loop.condition, loop)
        return loopEnterNode to blockEnterNode
    }

    fun enterDoWhileLoopCondition(loop: FirLoop): Pair<LoopBlockExitNode, LoopConditionEnterNode> {
        val blockExitNode = createLoopBlockExitNode(loop).also { addNewSimpleNode(it) }
        // This may sound shocking, but `do...while` conditions can `continue` to themselves,
        // so we can't pop the node off the stack here.
        val conditionEnterNode = loopConditionEnterNodes.getValue(loop).also { addNewSimpleNode(it) }
        // Might have had live `continue`s with an unreachable block exit, so recompute deadness.
        conditionEnterNode.updateDeadStatus()
        return blockExitNode to conditionEnterNode
    }

    fun exitDoWhileLoop(loop: FirLoop): Pair<LoopConditionExitNode, LoopExitNode> {
        loopConditionEnterNodes.remove(loop)
        val conditionExitNode = createLoopConditionExitNode(loop.condition, loop)
        val conditionBooleanValue = loop.condition.booleanLiteralValue
        popAndAddEdge(conditionExitNode)
        val blockEnterNode = lastNodes.pop()
        require(blockEnterNode is LoopBlockEnterNode)
        addBackEdge(conditionExitNode, blockEnterNode, isDead = conditionBooleanValue == false)
        val loopExit = loopExitNodes.remove(loop)!!
        addEdge(conditionExitNode, loopExit, propagateDeadness = false, isDead = conditionBooleanValue == true)
        loopExit.updateDeadStatus()
        lastNodes.push(loopExit)
        return conditionExitNode to loopExit
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression): CFGNode<FirBinaryLogicExpression> {
        return when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> createBinaryAndEnterNode(binaryLogicExpression)
            LogicOperationKind.OR -> createBinaryOrEnterNode(binaryLogicExpression)
        }.also { addNewSimpleNode(it) }
    }

    fun exitLeftBinaryLogicExpressionArgument(
        binaryLogicExpression: FirBinaryLogicExpression
    ): Pair<CFGNode<FirBinaryLogicExpression>, CFGNode<FirBinaryLogicExpression>> {
        val (leftExitNode, rightEnterNode) = when (binaryLogicExpression.kind) {
            LogicOperationKind.AND ->
                createBinaryAndExitLeftOperandNode(binaryLogicExpression) to createBinaryAndEnterRightOperandNode(binaryLogicExpression)
            LogicOperationKind.OR ->
                createBinaryOrExitLeftOperandNode(binaryLogicExpression) to createBinaryOrEnterRightOperandNode(binaryLogicExpression)
        }
        addNewSimpleNode(leftExitNode)
        lastNodes.push(leftExitNode) // to create an exit edge later
        val rhsNeverExecuted =
            binaryLogicExpression.leftOperand.booleanLiteralValue == (binaryLogicExpression.kind != LogicOperationKind.AND)
        addNewSimpleNode(rightEnterNode, isDead = rhsNeverExecuted)
        return leftExitNode to rightEnterNode
    }

    fun exitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression): AbstractBinaryExitNode<FirBinaryLogicExpression> {
        val exitNode = when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> createBinaryAndExitNode(binaryLogicExpression)
            LogicOperationKind.OR -> createBinaryOrExitNode(binaryLogicExpression)
        }
        val rightNode = lastNodes.pop()
        val leftNode = lastNodes.pop()
        val rhsAlwaysExecuted =
            binaryLogicExpression.leftOperand.booleanLiteralValue == (binaryLogicExpression.kind == LogicOperationKind.AND)
        addEdge(leftNode, exitNode, propagateDeadness = !rhsAlwaysExecuted, isDead = rhsAlwaysExecuted)
        addEdge(rightNode, exitNode, propagateDeadness = rhsAlwaysExecuted)
        lastNodes.push(exitNode)
        return exitNode
    }

    private val FirExpression.booleanLiteralValue: Boolean? get() = (this as? FirLiteralExpression<*>)?.value as? Boolean?

    // ----------------------------------- Try-catch-finally -----------------------------------

    fun enterTryExpression(tryExpression: FirTryExpression): Pair<TryExpressionEnterNode, TryMainBlockEnterNode> {
        val enterTryExpressionNode = createTryExpressionEnterNode(tryExpression).also { addNewSimpleNode(it) }
        tryExitNodes.push(createTryExpressionExitNode(tryExpression))

        val enterTryMainBlockNode = createTryMainBlockEnterNode(tryExpression).also { addNewSimpleNode(it) }

        catchNodes.push(tryExpression.catches.map { createCatchClauseEnterNode(it) })
        if (tryExpression.finallyBlock != null) {
            finallyEnterNodes.push(createFinallyBlockEnterNode(tryExpression))
        }

        // These edges should really be from `enterTryMainBlockNode`, but there is no practical difference
        // so w/e. In fact, `enterTryExpressionNode` is just 100% redundant.
        for (catchEnterNode in catchNodes.top()) {
            addEdge(enterTryExpressionNode, catchEnterNode)
        }
        if (tryExpression.finallyBlock != null) {
            addEdge(enterTryExpressionNode, finallyEnterNodes.top(), label = UncaughtExceptionPath)
        }

        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        return enterTryExpressionNode to enterTryMainBlockNode
    }

    fun exitTryMainBlock(): TryMainBlockExitNode {
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
            addEdge(node, catchEnterNode, propagateDeadness = false)
        }
        return node
    }

    fun enterCatchClause(catch: FirCatch): CatchClauseEnterNode {
        val catchEnterNode = catchBlocksInProgress.pop()
        assert(catchEnterNode.fir == catch)
        if (tryExitNodes.top().fir.finallyBlock != null) {
            addEdge(catchEnterNode, finallyEnterNodes.top(), propagateDeadness = false, label = UncaughtExceptionPath)
        }
        lastNodes.push(catchEnterNode)
        return catchEnterNode
    }

    fun exitCatchClause(catch: FirCatch): CatchClauseExitNode {
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
            finallyBlocksInProgressSet.add(it.fir)
        }
    }

    fun exitFinallyBlock(): FinallyBlockExitNode {
        val enterNode = finallyBlocksInProgress.top()
        val tryExitNode = tryExitNodes.top()
        val exitNode = createFinallyBlockExitNode(enterNode.fir)
        popAndAddEdge(exitNode)
        addEdge(exitNode, tryExitNode, isDead = enterNode.allNormalInputsAreDead)
        val nextExitLevel = levelOfNextExceptionCatchingGraph()
        val nextFinally = finallyEnterNodes.topOrNull()?.takeIf { it.level > nextExitLevel }
        if (nextFinally != null) {
            // `PathAwareControlFlowGraphVisitor` has a special case that this path matches any label
            // that is not otherwise matched by the edges below.
            addEdge(exitNode, nextFinally, label = UncaughtExceptionPath, propagateDeadness = false)
        }

        // Make sure only incoming edge labels are matched when exiting
        val incomingEdges = enterNode.previousNodes.map { it.edgeTo(enterNode).label }.toSet()
        val nextFinallyOrExitLevel = nextFinally?.level ?: nextExitLevel
        //                   /-----------v
        // f@ { try { return@f } finally { b }; c }
        //                                   \-----^
        exitNode.addReturnEdges(exitTargetsForReturn.values.filter { it in incomingEdges }, nextFinallyOrExitLevel)
        //                               /-----------v
        // f@ while (x) { try { continue@f } finally { b }; c }
        //          ^------------------------------------/
        exitNode.addReturnEdges(loopConditionEnterNodes.values.filter { it in incomingEdges }, nextFinallyOrExitLevel)
        //                            /-----------v
        // f@ while (x) { try { break@f } finally { b }; c }
        //                                            \-----^
        exitNode.addReturnEdges(loopExitNodes.values.filter { it in incomingEdges }, nextFinallyOrExitLevel)
        return exitNode
    }

    private val FinallyBlockEnterNode.allNormalInputsAreDead: Boolean
        get() = previousNodes.all {
            val edge = edgeFrom(it)
            edge.kind.isDead || edge.label != NormalPath
        }

    private fun <T> CFGNode<*>.addReturnEdges(nodes: Iterable<T>, minLevel: Int) where T : CFGNode<*>, T : EdgeLabel {
        for (node in nodes) {
            when {
                // TODO: this check is imprecise and can add redundant edges:
                // x@{
                //     try {
                //         return@x
                //     } finally {}
                // }
                // try {} finally { /* return@x target is in nonDirectJumps */ }
                //  KT-59725
                node.level < minLevel || node !in nonDirectJumps -> continue
                // TODO: if the input to finally with that label is dead, then so should be the exit probably. KT-59725
                node.returnPathIsBackwards -> addBackEdge(this, node, label = node)
                else -> addEdge(this, node, propagateDeadness = false, label = node)
            }
        }
    }

    fun exitTryExpression(callCompleted: Boolean): TryExpressionExitNode {
        var haveNothingReturnCall = false
        notCompletedFunctionCalls.pop().forEach { haveNothingReturnCall = completeFunctionCall(it) || haveNothingReturnCall }
        val node = tryExitNodes.pop()
        if (node.fir.finallyBlock != null) {
            val enterFinallyNode = finallyBlocksInProgress.pop()
            finallyBlocksInProgressSet.remove(enterFinallyNode.fir)

            /**
             * If it appears that after completion try main expression returns nothing and try has finally block,
             *   we should make edge from finally exist to try exit a dead (and it may be not dead originally
             *   before completion)
             */
            if (haveNothingReturnCall && enterFinallyNode.allNormalInputsAreDead) {
                val exitFinallyNode = node.previousNodes.single()
                assert(exitFinallyNode is FinallyBlockExitNode)
                CFGNode.removeAllIncomingEdges(node)
                addEdge(exitFinallyNode, node, isDead = true)
            }
        }
        mergeDataFlowFromPostponedLambdas(node, callCompleted)
        node.updateDeadStatus()
        lastNodes.push(node)
        return node
    }

    // Called-in-place function graphs are effectively inlined, exceptions go to enclosing function.
    private fun levelOfNextExceptionCatchingGraph(): Int =
        graphs.all().first { it.kind != ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace }.exitNode.level

    // this is a workaround to make function call dead when call is completed _after_ building its node in the graph
    // this happens when completing the last call in try/catch blocks
    // TODO: this doesn't make fully 'right' Nothing node (doesn't support going to catch and pass through finally)
    //  because doing those afterwards is quite challenging
    //  it would be much easier if we could build calls after full completion only, at least for Nothing calls
    //  KT-59726
    // @returns `true` if node actually returned Nothing
    private fun completeFunctionCall(node: FunctionCallNode): Boolean {
        if (!node.fir.hasNothingType) return false
        val stub = StubNode(node.owner, node.level)
        val edges = node.followingNodes.map { it to node.edgeTo(it) }
        CFGNode.removeAllOutgoingEdges(node)
        CFGNode.addEdge(node, stub, EdgeKind.DeadForward, propagateDeadness = false)
        for ((to, edge) in edges) {
            val kind = if (edge.kind.isBack) EdgeKind.DeadBackward else EdgeKind.DeadForward
            CFGNode.addEdge(stub, to, kind, propagateDeadness = false, label = edge.label)
            to.updateDeadStatus()
            propagateDeadnessForward(to)
        }
        return true
    }

    // ----------------------------------- Resolvable call -----------------------------------

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression): QualifiedAccessNode {
        val returnsNothing = qualifiedAccessExpression.hasNothingType
        val node = createQualifiedAccessNode(qualifiedAccessExpression)
        if (returnsNothing) {
            addNonSuccessfullyTerminatingNode(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
    }

    fun exitSmartCastExpression(smartCastExpression: FirSmartCastExpression): SmartCastExpressionExitNode {
        val node = createSmartCastExitNode(smartCastExpression)
        addNewSimpleNode(node)
        return node
    }

    fun exitResolvedQualifierNode(resolvedQualifier: FirResolvedQualifier): ResolvedQualifierNode {
        return createResolvedQualifierNode(resolvedQualifier).also(this::addNewSimpleNode)
    }

    fun enterCall(lambdas: Set<FirFunctionSymbol<*>> = emptySet()) {
        splitDataFlowForPostponedLambdas(lambdas)
    }

    fun enterCallArguments(call: FirStatement, anonymousFunctions: List<FirAnonymousFunction>): FunctionCallArgumentsEnterNode? {
        if (anonymousFunctions.isEmpty()) {
            argumentListSplitNodes.push(null)
        } else {
            val splitNode = createSplitPostponedLambdasNode(call, anonymousFunctions)
            anonymousFunctions.associateTo(postponedAnonymousFunctionNodes) { it.symbol to (splitNode to null) }
            argumentListSplitNodes.push(splitNode)
        }

        if (call is FirFunctionCall) {
            val enterNode = createFunctionCallArgumentsEnterNode(call)
            val exitNode = createFunctionCallArgumentsExitNode(call, enterNode)

            exitFunctionCallArgumentsNodes.push(exitNode)
            addNewSimpleNode(enterNode)

            return enterNode
        } else {
            exitFunctionCallArgumentsNodes.push(null)
            return null
        }
    }

    fun exitCallArguments(): Pair<SplitPostponedLambdasNode?, FunctionCallArgumentsExitNode?> {
        val splitNode = argumentListSplitNodes.pop()?.also { addNewSimpleNode(it) }
        val exitNode = exitFunctionCallArgumentsNodes.pop()?.also { addNewSimpleNode(it) }
        return splitNode to exitNode
    }

    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean): FunctionCallNode {
        val returnsNothing = functionCall.hasNothingType
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
        val node = createDelegatedConstructorCallNode(call)
        unifyDataFlowFromPostponedLambdas(node, callCompleted)
        addNewSimpleNode(node)
        return node
    }

    fun exitStringConcatenationCall(call: FirStringConcatenationCall): StringConcatenationCallNode {
        val node = createStringConcatenationCallNode(call)
        unifyDataFlowFromPostponedLambdas(node, callCompleted = true)
        addNewSimpleNode(node)
        return node
    }

    fun exitLiteralExpression(literalExpression: FirLiteralExpression<*>): LiteralExpressionNode {
        return createLiteralExpressionNode(literalExpression).also { addNewSimpleNode(it) }
    }

    fun exitVariableDeclaration(variable: FirProperty): VariableDeclarationNode {
        return createVariableDeclarationNode(variable).also { addNewSimpleNode(it) }
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment): VariableAssignmentNode {
        val node = createVariableAssignmentNode(assignment).also { addNewSimpleNode(it) }

        // Create edges from each assignment to any catch and/or finally blocks. Assignment is the only thing which can downgrade
        // smart-casting, and exceptions can be thrown almost anywhere, so it is easier to track assignment than exceptions. This makes sure
        // that any smart-cast downgrade which happens in a try block is recognized in catch and finally blocks.
        val nextCatch = catchNodes.topOrNull()
        if (!nextCatch.isNullOrEmpty()) {
            val kind = if (nextCatch.first().level > levelOfNextExceptionCatchingGraph()) EdgeKind.Forward else EdgeKind.DfgForward
            for (catchEnterNode in nextCatch) {
                addEdge(node, catchEnterNode, preferredKind = kind, propagateDeadness = false)
            }
        }
        val nextFinally = finallyEnterNodes.topOrNull()
        if (nextFinally != null) {
            val kind = if (nextFinally.level > levelOfNextExceptionCatchingGraph()) EdgeKind.Forward else EdgeKind.DfgForward
            addEdge(node, nextFinally, preferredKind = kind, propagateDeadness = false, label = UncaughtExceptionPath)
        }

        return node
    }

    fun exitThrowExceptionNode(throwExpression: FirThrowExpression): ThrowExceptionNode {
        return createThrowExceptionNode(throwExpression).also { addNonSuccessfullyTerminatingNode(it) }
    }

    fun exitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, callCompleted: Boolean): CheckNotNullCallNode {
        val node = createCheckNotNullCallNode(checkNotNullCall)
        unifyDataFlowFromPostponedLambdas(node, callCompleted)
        if (checkNotNullCall.hasNothingType) {
            addNonSuccessfullyTerminatingNode(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
    }

    // ----------------------------------- Fake expressions -----------------------------------

    fun enterFakeExpression(): FakeExpressionEnterNode {
        // Things like annotations and `contract { ... }` use normal call resolution, but aren't real expressions
        // and are never evaluated. We'll push all nodes created in the process into a stub graph, then throw it away.
        return enterGraph(null, "<compile-time expression graph>", ControlFlowGraph.Kind.FakeCall) {
            createFakeExpressionEnterNode() to createFakeExpressionEnterNode()
        }
    }

    fun exitFakeExpression() {
        lastNodes.pop()
        graphs.pop().also { assert(it.kind == ControlFlowGraph.Kind.FakeCall) }
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
        return enterGraph(initBlock, "init block", ControlFlowGraph.Kind.ClassInitializer) {
            createInitBlockEnterNode(it) to createInitBlockExitNode(it)
        }.also { addEdgeIfLocalClassMember(it) }
    }

    fun exitInitBlock(): Pair<InitBlockExitNode, ControlFlowGraph> {
        return exitGraph()
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
            // TODO Refactor annotation arguments phase to not build CFG so that we can use resolvedType instead, see KT-61834
            @OptIn(UnresolvedExpressionTypeAccess::class)
            val lhsIsNull = elvisExpression.lhs.coneTypeOrNull?.isNullableNothing == true
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

    // -------------------------------------------------------------------------------------------------------------------------

    fun reset() {
        enterToLocalClassesMembers.clear()
        postponedLambdaExits.reset()
        lastNodes.reset()
    }

    // ----------------------------------- Edge utils -----------------------------------

    private fun addNewSimpleNode(node: CFGNode<*>, isDead: Boolean = false) {
        addEdge(lastNodes.pop(), node, preferredKind = if (isDead) EdgeKind.DeadForward else EdgeKind.Forward)
        lastNodes.push(node)
    }

    private fun addNonSuccessfullyTerminatingNode(node: CFGNode<*>) {
        popAndAddEdge(node)
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
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

    private fun addEdgeToSubGraph(from: CFGNode<*>, to: CFGNode<*>, label: EdgeLabel = NormalPath) {
        val wasDead = to.isDead
        val isDead = wasDead || from.isDead
        // Can only add control flow since data flow for every node that follows `to` has already been computed.
        CFGNode.addEdge(from, to, if (isDead) EdgeKind.DeadForward else EdgeKind.CfgForward, propagateDeadness = true, label)
        if (isDead && !wasDead) {
            propagateDeadnessForward(to)
        }
    }

    private fun addBackEdge(from: CFGNode<*>, to: CFGNode<*>, isDead: Boolean = false, label: EdgeLabel = NormalPath) {
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

    private data class PostponedLambdas(
        val lambdas: Set<FirFunctionSymbol<*>>,
        val exits: MutableList<Pair<CFGNode<*>, EdgeKind>> = mutableListOf(),
    )
}

fun FirDeclaration?.isLocalClassOrAnonymousObject() = ((this as? FirRegularClass)?.isLocal == true) || this is FirAnonymousObject

private val FirControlFlowGraphOwner.memberShouldHaveGraph: Boolean
    get() = when (this) {
        is FirProperty -> initializer != null || delegate != null || hasExplicitBackingField
        is FirField -> initializer != null
        else -> true
    }

/**
 * @return true for [FirControlFlowGraphOwner] which, as a class member, should be part of the class
 */
val FirControlFlowGraphOwner.isUsedInControlFlowGraphBuilderForClass: Boolean
    get() = when (this) {
        is FirProperty, is FirField -> memberShouldHaveGraph
        is FirConstructor, is FirAnonymousInitializer -> true
        is FirFunction, is FirClass -> false
        else -> true
    }

/**
 * @return true for [FirControlFlowGraphOwner] which, as a file member, should be part of the file
 */
val FirControlFlowGraphOwner.isUsedInControlFlowGraphBuilderForFile: Boolean
    get() = when (this) {
        is FirProperty -> memberShouldHaveGraph
        else -> false
    }

/**
 * @return true for [FirControlFlowGraphOwner] which, as a script statement, should be part of the script
 */
val FirControlFlowGraphOwner.isUsedInControlFlowGraphBuilderForScript: Boolean
    get() = when (this) {
        is FirProperty, is FirField, is FirAnonymousInitializer -> memberShouldHaveGraph
        else -> false
    }

// TODO Refactor annotation arguments phase to not build CFG so that we can use resolvedType instead, see KT-61834
@OptIn(UnresolvedExpressionTypeAccess::class)
private val FirExpression.hasNothingType: Boolean
    get() = coneTypeOrNull?.isNothing == true
