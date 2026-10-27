/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.Node
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIndex.InputRole
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.allOverridden
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import java.util.IdentityHashMap

/**
 * Determines which tracked values may be observed by code that [Policy] cannot rule out.
 *
 * The analysis propagates tracked values forward through merges, callee parameters, constructor
 * arguments, identity-preserving type operations, function returns, and the heap. A tracked value
 * is observed when it reaches a use that the graph cannot follow: an opaque escape, a throw,
 * virtual dispatch, dynamic access, or code and call sites that the graph cannot enumerate. A
 * [Policy] declares its own seeds, and may make a use transparent or observing first.
 *
 * The heap follows one rule: objects are precise about what they are created with, and later
 * stores are field-based. Every [Node.AllocArray] is tracked, with its elements in the `Array$Item`
 * pseudo-field, as in Kotlin/Native. A [Node.FieldWrite] or [Node.ArrayWrite] reaches every read of
 * its field regardless of the receiver, so reads stay sound for objects the analysis does not
 * track. A read whose receiver may be a tracked object additionally sees that object's contents,
 * and an observed object makes all of its current and future contents observed.
 */
internal class JsValueFlowAnalysis(private val dfg: JsModuleDFG, private val policy: Policy) {
    enum class Effect {
        /** The use is followed with the analysis's default flow rules. */
        Flow,

        /** The use neither observes the value nor passes it on. */
        Transparent,

        /** The use may observe the value in any way. */
        Observe,
    }

    interface Policy {
        /** Values to track besides the analysis's own heap objects. */
        val seeds: List<Node>

        /** The effect of [use] on tracked [value]; [Effect.Flow] unless the client has its own rule. */
        fun effectOf(value: Node, use: JsDataFlowIndex.Use): Effect
    }

    /** The tracked values stored into, and the reads of, one field of one tracked object. */
    private class FieldState {
        val contents = mutableSetOf<Node>()
        val reads = mutableSetOf<Node>()
    }

    private val index = dfg.index
    private val arrayItemField = JsDataFlowIR.Field(type = JsDataFlowIR.Type.Dynamic, name = $$"Array$Item")
    private val heap = IdentityHashMap<Node.AllocArray, MutableMap<JsDataFlowIR.Field, FieldState>>()

    /** Tracked values that may reach each node. */
    private val reaching = IdentityHashMap<Node, MutableSet<Node>>()
    private val observed = mutableSetOf<Node>()
    private val worklist = ArrayDeque<Node>()

    fun run() {
        for (node in index.nodes) {
            if (node is Node.AllocArray) {
                heap[node] = mutableMapOf()
                propagate(node = node, values = listOf(node))
            }
        }
        for (seed in policy.seeds) {
            propagate(node = seed, values = listOf(seed))
        }
        while (worklist.isNotEmpty()) {
            process(node = worklist.removeFirst())
        }
    }

    fun isObserved(value: Node): Boolean = value in observed

    private fun propagate(node: Node, values: Collection<Node>) {
        if (reaching.getOrPut(node) { mutableSetOf() }.addAll(values)) {
            worklist.add(node)
        }
    }

    private fun observe(values: Collection<Node>) {
        for (value in values) {
            if (observed.add(value) && value is Node.AllocArray) {
                checkNotNull(heap[value]).values.forEach { observe(it.contents.toList()) }
            }
        }
    }

    private fun fieldState(obj: Node.AllocArray, field: JsDataFlowIR.Field): FieldState =
        checkNotNull(heap[obj]).getOrPut(field) { FieldState() }

    private fun store(obj: Node.AllocArray, field: JsDataFlowIR.Field, values: Collection<Node>) {
        val state = fieldState(obj, field)
        val added = values.filter { state.contents.add(it) }
        if (obj in observed) {
            observe(added)
        } else {
            state.reads.forEach { propagate(it, added) }
        }
    }

    private fun process(node: Node) {
        val values = reaching[node] ?: return
        // An observed value is final and no longer needs to be followed.
        val live = values.filterNot { it in observed }
        if (live.isEmpty()) {
            return
        }

        if (index.isThrown(node)) {
            observe(live)
        }
        index.returnOwner(node)?.let { function -> propagateReturnToCallSites(function, live) }

        for (use in index.usesOf(node)) {
            val followed = live.filter { value ->
                when (policy.effectOf(value, use)) {
                    Effect.Flow -> true
                    Effect.Transparent -> false
                    Effect.Observe -> {
                        observe(listOf(value))
                        false
                    }
                }
            }
            if (followed.isNotEmpty()) {
                applyUse(use, followed)
            }
        }
    }

    private fun applyUse(use: JsDataFlowIndex.Use, values: List<Node>) {
        when (val consumer = use.consumer) {
            is Node.Variable -> propagate(consumer, values)
            is Node.VirtualCall -> observe(values)
            is Node.Call -> propagateIntoCallee(consumer.callee, use.argumentIndex(), values)
            is Node.NewInstance -> propagateIntoCallee(consumer.constructor, use.argumentIndex(), values)
            is Node.TypeCheck ->
                if (consumer.resultIsArgument) {
                    propagate(consumer, values)
                } else {
                    observe(values)
                }
            is Node.AllocArray -> store(consumer, arrayItemField, values)
            is Node.FieldWrite, is Node.ArrayWrite -> when (use.role) {
                InputRole.AssignedValue -> possibleReadsOf(consumer).forEach { propagate(it, values) }
                // Storing into a tracked object does not expose it; the properties of any other value may be its state.
                InputRole.Receiver -> observe(values.filterNot { it is Node.AllocArray })
                else -> observe(values)
            }
            is Node.FieldRead, is Node.ArrayRead -> when (use.role) {
                InputRole.Receiver -> {
                    observe(values.filterNot { it is Node.AllocArray })
                    val field = fieldOf(consumer)
                    for (obj in values.filterIsInstance<Node.AllocArray>()) {
                        val state = fieldState(obj, field)
                        if (state.reads.add(consumer)) {
                            propagate(consumer, state.contents)
                        }
                    }
                }
                else -> observe(values)
            }
            else -> observe(values)
        }
    }

    private fun fieldOf(access: Node): JsDataFlowIR.Field = when (access) {
        is Node.FieldRead -> access.field
        is Node.FieldWrite -> access.field
        is Node.ArrayRead, is Node.ArrayWrite -> arrayItemField
        else -> error("Not a heap access: $access")
    }

    private fun possibleReadsOf(write: Node): List<Node> = when (write) {
        is Node.FieldWrite -> index.readsOf(write.field)
        is Node.ArrayWrite -> index.arrayReads // all reads, safe option
        else -> error("Not a heap write: $write")
    }

    private fun JsDataFlowIndex.Use.argumentIndex(): Int =
        checkNotNull(role as? InputRole.Argument) { "Expected an argument use by $consumer" }.index

    private fun propagateIntoCallee(callee: JsDataFlowIR.FunctionSymbol, argIndex: Int, values: List<Node>) {
        if (callee.isExported || callee.isExternal) {
            observe(values)
            return
        }
        // No parameter node means the value crosses into a body the graph cannot inspect.
        val paramNode = index.parameterAt(callee, argIndex)
        if (paramNode != null) {
            propagate(paramNode, values)
        } else {
            observe(values)
        }
    }

    /**
     * A returned value is received at every call site of [function] and of the methods it
     * overrides (virtual calls are recorded against the overridden callee). If the call sites
     * cannot be enumerated — the function is exported, address-taken, local, or overrides an
     * exported or external member — the value is observed.
     */
    private fun propagateReturnToCallSites(function: JsDataFlowIR.Function, values: List<Node>) {
        val symbol = function.symbol
        val declaration = symbol.irSimpleFunction
        if (symbol.isExported ||
            declaration == null ||
            index.isAddressTaken(symbol) ||
            declaration.parent is IrFunction
        ) {
            observe(values)
            return
        }
        considerCallSites(symbol, values)

        for (overridden in declaration.allOverridden()) {
            if (overridden.isEffectivelyExternal()) {
                observe(values)
                return
            }
            // An unmapped override has neither a body nor a call site in the linked program.
            val overriddenSymbol = dfg.symbolTable.functionMap[overridden] ?: continue
            if (overriddenSymbol.isExported) {
                observe(values)
                return
            }
            considerCallSites(overriddenSymbol, values)
        }
    }

    private fun considerCallSites(symbol: JsDataFlowIR.FunctionSymbol, values: List<Node>) {
        index.callsTo(symbol).forEach { propagate(it, values) }
    }
}
