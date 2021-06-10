/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class EventOccurrencesRangeInfo<E : EventOccurrencesRangeInfo<E, K>, K : Any>(
    map: PersistentMap<K, EventOccurrencesRange> = persistentMapOf()
) : ControlFlowInfo<E, K, EventOccurrencesRange>(map) {

    override fun merge(other: E): E =
        operation(other, EventOccurrencesRange::or)

    fun plus(other: E): E =
        when {
            isEmpty() -> other
            other.isEmpty() ->
                @Suppress("UNCHECKED_CAST")
                this as E
            else -> operation(other, EventOccurrencesRange::plus)
        }

    private inline fun operation(other: E, op: (EventOccurrencesRange, EventOccurrencesRange) -> EventOccurrencesRange): E {
        @Suppress("UNCHECKED_CAST")
        var result = this as E
        for (symbol in keys.union(other.keys)) {
            val kind1 = this[symbol] ?: EventOccurrencesRange.ZERO
            val kind2 = other[symbol] ?: EventOccurrencesRange.ZERO
            result = result.put(symbol, op.invoke(kind1, kind2))
        }
        return result
    }
}

class PropertyInitializationInfo(
    map: PersistentMap<FirPropertySymbol, EventOccurrencesRange> = persistentMapOf()
) : EventOccurrencesRangeInfo<PropertyInitializationInfo, FirPropertySymbol>(map) {
    companion object {
        val EMPTY = PropertyInitializationInfo()
    }

    override val constructor: (PersistentMap<FirPropertySymbol, EventOccurrencesRange>) -> PropertyInitializationInfo =
        ::PropertyInitializationInfo

    override val empty: () -> PropertyInitializationInfo =
        ::EMPTY
}

class LocalPropertyAndCapturedWriteCollector private constructor() : ControlFlowGraphVisitorVoid() {
    companion object {
        fun collect(graph: ControlFlowGraph): Pair<Set<FirPropertySymbol>, Set<FirVariableAssignment>> {
            val collector = LocalPropertyAndCapturedWriteCollector()
            graph.traverse(TraverseDirection.Forward, collector)
            return collector.symbols.keys to collector.capturedWrites
        }
    }

    // Mapping from a property symbol to its declaration context
    // `true` if the (local) property is declared in the currently visited function.
    // `false` if it is declared in a lambda or a local function (inside the currently visited function).
    private val symbols: MutableMap<FirPropertySymbol, Boolean> = mutableMapOf()

    private val lambdaOrLocalFunctionStack: MutableList<FirFunction<*>> = mutableListOf()
    private val capturedWrites: MutableSet<FirVariableAssignment> = mutableSetOf()

    override fun visitNode(node: CFGNode<*>) {}

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
        symbols[node.fir.symbol] = lambdaOrLocalFunctionStack.lastOrNull() == null
    }

    override fun visitPostponedLambdaEnterNode(node: PostponedLambdaEnterNode) {
        lambdaOrLocalFunctionStack.add(node.fir)
    }

    override fun visitPostponedLambdaExitNode(node: PostponedLambdaExitNode) {
        lambdaOrLocalFunctionStack.remove(node.fir.anonymousFunction)
    }

    override fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode, data: Nothing?) {
        lambdaOrLocalFunctionStack.add(node.fir)
    }

    override fun visitFunctionExitNode(node: FunctionExitNode) {
        lambdaOrLocalFunctionStack.remove(node.fir)
    }

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
        // Check if this variable assignment is inside a lambda or a local function.
        if (lambdaOrLocalFunctionStack.isEmpty()) return

        // Check if the assigned variable doesn't belong to any lambda or local function.
        val symbol = node.fir.referredPropertySymbol ?: return
        if (symbol !in symbols || symbols[symbol] == false) return

        // If all nested declarations are lambdas that are invoked in-place (according to the contract),
        // this variable assignment is not a captured write.
        if (lambdaOrLocalFunctionStack.all { it is FirAnonymousFunction && it.invocationKind.isInPlace }) return

        capturedWrites.add(node.fir)
    }
}

class PathAwarePropertyInitializationInfo(
    map: PersistentMap<EdgeLabel, PropertyInitializationInfo> = persistentMapOf()
) : PathAwareControlFlowInfo<PathAwarePropertyInitializationInfo, PropertyInitializationInfo>(map) {
    companion object {
        val EMPTY = PathAwarePropertyInitializationInfo(persistentMapOf(NormalPath to PropertyInitializationInfo.EMPTY))
    }

    override val constructor: (PersistentMap<EdgeLabel, PropertyInitializationInfo>) -> PathAwarePropertyInitializationInfo =
        ::PathAwarePropertyInitializationInfo

    override val empty: () -> PathAwarePropertyInitializationInfo =
        ::EMPTY
}

class PropertyInitializationInfoCollector(
    private val localProperties: Set<FirPropertySymbol>,
    private val declaredVariableCollector: DeclaredVariableCollector = DeclaredVariableCollector(),
) : ControlFlowGraphVisitor<PathAwarePropertyInitializationInfo, Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>>() {
    override fun visitNode(
        node: CFGNode<*>,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        if (data.isEmpty()) return PathAwarePropertyInitializationInfo.EMPTY
        return data.map { (label, info) -> info.applyLabel(node, label) }
            .reduce(PathAwarePropertyInitializationInfo::merge)
    }

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        val symbol = node.fir.referredPropertySymbol ?: return dataForNode
        return if (symbol !in localProperties) {
            dataForNode
        } else {
            processVariableWithAssignment(dataForNode, symbol)
        }
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        return processVariableWithAssignment(
            dataForNode,
            node.fir.symbol,
            overwriteRange = node.fir.initializer == null && node.fir.delegate == null
        )
    }

    fun getData(graph: ControlFlowGraph) =
        graph.collectDataForNode(
            TraverseDirection.Forward,
            PathAwarePropertyInitializationInfo.EMPTY,
            this
        )

    private fun processVariableWithAssignment(
        dataForNode: PathAwarePropertyInitializationInfo,
        symbol: FirPropertySymbol,
        overwriteRange: Boolean = false,
    ): PathAwarePropertyInitializationInfo {
        assert(dataForNode.keys.isNotEmpty())
        return if (overwriteRange)
            overwriteRange(dataForNode, symbol, EventOccurrencesRange.ZERO, ::PathAwarePropertyInitializationInfo)
        else
            addRange(dataForNode, symbol, EventOccurrencesRange.EXACTLY_ONCE, ::PathAwarePropertyInitializationInfo)
    }

    // --------------------------------------------------
    // Data flows of declared/assigned variables in loops
    // --------------------------------------------------

    private fun enterCapturingStatement(statement: FirStatement): Set<FirPropertySymbol> =
        declaredVariableCollector.enterCapturingStatement(statement)

    private fun exitCapturingStatement(statement: FirStatement) {
        declaredVariableCollector.exitCapturingStatement(statement)
    }

    // A merge point for a loop with `continue`
    override fun visitLoopEnterNode(
        node: LoopEnterNode,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        val declaredVariableSymbolsInLoop = enterCapturingStatement(node.fir)
        if (declaredVariableSymbolsInLoop.isEmpty())
            return visitNode(node, data)

        return filterDeclaredVariableSymbolsInCapturedScope(node, declaredVariableSymbolsInLoop, data)
    }

    // A merge point for while loop
    override fun visitLoopConditionEnterNode(
        node: LoopConditionEnterNode,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        val declaredVariableSymbolsInLoop = declaredVariableCollector.declaredVariablesPerElement[node.loop]
        if (declaredVariableSymbolsInLoop.isEmpty())
            return visitNode(node, data)

        return filterDeclaredVariableSymbolsInCapturedScope(node, declaredVariableSymbolsInLoop, data)
    }

    // A merge point for do-while loop
    override fun visitLoopBlockEnterNode(
        node: LoopBlockEnterNode,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        val declaredVariableSymbolsInLoop = declaredVariableCollector.declaredVariablesPerElement[node.fir]
        if (declaredVariableSymbolsInLoop.isEmpty())
            return visitNode(node, data)

        return filterDeclaredVariableSymbolsInCapturedScope(node, declaredVariableSymbolsInLoop, data)
    }

    private fun filterDeclaredVariableSymbolsInCapturedScope(
        node: CFGNode<*>,
        declaredVariableSymbolsInCapturedScope: Collection<FirPropertySymbol>,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        var filteredData = data
        for (variableSymbol in declaredVariableSymbolsInCapturedScope) {
            filteredData = filteredData.map { (label, pathAwareInfo) ->
                label to if (label is LoopBackPath) {
                    removeRange(pathAwareInfo, variableSymbol, ::PathAwarePropertyInitializationInfo)
                } else {
                    pathAwareInfo
                }
            }
        }
        return visitNode(node, filteredData)
    }

    override fun visitLoopExitNode(
        node: LoopExitNode,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        exitCapturingStatement(node.fir)
        return visitNode(node, data)
    }
}

// Note that [PreliminaryLoopVisitor] in FIR DFA collects assigned variable names.
// This one collects declared variable symbols per capturing statements.
class DeclaredVariableCollector {
    val declaredVariablesPerElement: SetMultimap<FirStatement, FirPropertySymbol> = setMultimapOf()

    fun enterCapturingStatement(statement: FirStatement): Set<FirPropertySymbol> {
        assert(statement is FirLoop || statement is FirClass<*> || statement is FirFunction<*>)
        if (statement !in declaredVariablesPerElement) {
            statement.accept(visitor, null)
        }
        return declaredVariablesPerElement[statement]
    }

    fun exitCapturingStatement(statement: FirStatement) {
        assert(statement is FirLoop || statement is FirClass<*> || statement is FirFunction<*>)
        declaredVariablesPerElement.removeKey(statement)
    }

    fun resetState() {
        declaredVariablesPerElement.clear()
    }

    // FirStatement -- closest statement (loop/lambda/local declaration) which may contain reassignments
    private val visitor = object : FirVisitor<Unit, FirStatement?>() {
        override fun visitElement(element: FirElement, data: FirStatement?) {
            element.acceptChildren(this, data)
        }

        override fun visitProperty(property: FirProperty, data: FirStatement?) {
            if (property.isLocal) {
                requireNotNull(data)
                declaredVariablesPerElement.put(data, property.symbol)
            }
            visitElement(property, data)
        }

        override fun visitWhileLoop(whileLoop: FirWhileLoop, data: FirStatement?) {
            visitCapturingStatement(whileLoop, data)
        }

        override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: FirStatement?) {
            visitCapturingStatement(doWhileLoop, data)
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: FirStatement?) {
            visitCapturingStatement(anonymousFunction, data)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: FirStatement?) {
            visitCapturingStatement(simpleFunction, data)
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: FirStatement?) {
            visitCapturingStatement(regularClass, data)
        }

        override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: FirStatement?) {
            visitCapturingStatement(anonymousObject, data)
        }

        private fun visitCapturingStatement(statement: FirStatement, parent: FirStatement?) {
            visitElement(statement, statement)
            if (parent != null) {
                declaredVariablesPerElement.putAll(parent, declaredVariablesPerElement[statement])
            }
        }
    }
}

internal fun <P : PathAwareControlFlowInfo<P, S>, S : ControlFlowInfo<S, K, EventOccurrencesRange>, K : Any> addRange(
    pathAwareInfo: P,
    key: K,
    range: EventOccurrencesRange,
    constructor: (PersistentMap<EdgeLabel, S>) -> P
): P {
    // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 } }
    // after (if key is p1):
    //   { |-> { p1 |-> PI1 + r }, l1 |-> { p1 |-> r, p2 |-> PI2 } }
    return updateRange(pathAwareInfo, key, { existingKind -> existingKind + range }, constructor)
}

internal fun <P : PathAwareControlFlowInfo<P, S>, S : ControlFlowInfo<S, K, EventOccurrencesRange>, K : Any> overwriteRange(
    pathAwareInfo: P,
    key: K,
    range: EventOccurrencesRange,
    constructor: (PersistentMap<EdgeLabel, S>) -> P
): P {
    // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 } }
    // after (if key is p1):
    //   { |-> { p1 |-> r }, l1 |-> { p1 |-> r, p2 |-> PI2 } }
    return updateRange(pathAwareInfo, key, { range }, constructor)
}

private inline fun <P : PathAwareControlFlowInfo<P, S>, S : ControlFlowInfo<S, K, EventOccurrencesRange>, K : Any> updateRange(
    pathAwareInfo: P,
    key: K,
    computeNewRange: (EventOccurrencesRange) -> EventOccurrencesRange,
    constructor: (PersistentMap<EdgeLabel, S>) -> P
): P {
    var resultMap = persistentMapOf<EdgeLabel, S>()
    // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 } }
    for ((label, dataPerLabel) in pathAwareInfo) {
        val existingKind = dataPerLabel[key] ?: EventOccurrencesRange.ZERO
        val kind = computeNewRange.invoke(existingKind)
        resultMap = resultMap.put(label, dataPerLabel.put(key, kind))
    }
    // after (if key is p1):
    //   { |-> { p1 |-> computeNewRange(PI1) }, l1 |-> { p1 |-> r, p2 |-> PI2 } }
    return constructor(resultMap)
}

private fun <P : PathAwareControlFlowInfo<P, S>, S : ControlFlowInfo<S, K, EventOccurrencesRange>, K : Any> removeRange(
    pathAwareInfo: P,
    key: K,
    constructor: (PersistentMap<EdgeLabel, S>) -> P
): P {
    var resultMap = persistentMapOf<EdgeLabel, S>()
    // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 } }
    for ((label, dataPerLabel) in pathAwareInfo) {
        resultMap = resultMap.put(label, dataPerLabel.remove(key))
    }
    // after (if key is p1):
    //   { |-> { }, l1 |-> { p2 |-> PI2 } }
    return constructor(resultMap)
}
