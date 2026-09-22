/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.ExecutionUnit
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.Field
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.Function
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.FunctionSymbol
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.Node
import java.util.IdentityHashMap

/**
 * Structural index of the value flows and declaration relationships in a DFG snapshot.
 *
 * Its backing collections use identity semantics, matching the symbol and node identity contract of [JsDataFlowIR].
 * Scope nodes are excluded from [nodes] because they organize graph traversal but do not participate in value flow.
 */
internal class JsDataFlowIndex {
    /** A use of a value by [consumer] in the position described by [role]. */
    class Use(val consumer: Node, val role: InputRole)

    /** Identifies which input of a consumer receives a value. */
    sealed interface InputRole {
        /** A parameter-aligned argument of a call-like operation. */
        class Argument(val index: Int) : InputRole

        /** An element of a merge or opaque escape. */
        class Element(val index: Int) : InputRole

        /** The receiver of a field or dynamic operation. */
        object Receiver : InputRole

        /** The value stored by a field write. */
        object AssignedValue : InputRole

        /** The value transformed or inspected by a unary operation. */
        object Operand : InputRole
    }

    private val uses = IdentityHashMap<Node, MutableList<Use>>()
    private val callSites = IdentityHashMap<FunctionSymbol, MutableList<Node.Call>>()
    private val parameters = IdentityHashMap<FunctionSymbol, Array<Node.Parameter?>>()
    private val returnOwners = IdentityHashMap<Node, Function>()
    private val thrownValues = mutableSetOf<Node>()
    private val fieldReads = IdentityHashMap<Field, MutableList<Node.FieldRead>>()
    private val addressTakenFunctions = mutableSetOf<FunctionSymbol>()

    /** Every non-scope node in the indexed execution units. */
    val nodes: List<Node>
        field = mutableListOf<Node>()

    fun usesOf(node: Node): List<Use> = uses[node].orEmpty()

    fun callsTo(symbol: FunctionSymbol): List<Node.Call> = callSites[symbol].orEmpty()

    fun parameterAt(symbol: FunctionSymbol, index: Int): Node.Parameter? =
        parameters[symbol]?.getOrNull(index)

    fun returnOwner(node: Node): Function? = returnOwners[node]

    fun isThrown(node: Node): Boolean = node in thrownValues

    fun readsOf(field: Field): List<Node.FieldRead> = fieldReads[field].orEmpty()

    fun isAddressTaken(symbol: FunctionSymbol): Boolean = symbol in addressTakenFunctions

    private fun addUse(value: Node, consumer: Node, role: InputRole) {
        val nodeUsage = uses.getOrPut(value) { mutableListOf() }
        nodeUsage.add(Use(consumer, role))
    }

    private fun recordNode(node: Node, function: Function?) {
        when (node) {
            is Node.Variable -> node.values.forEachIndexed { index, value ->
                addUse(value = value, consumer = node, role = InputRole.Element(index))
            }
            is Node.Call -> {
                callSites.getOrPut(node.callee) { mutableListOf() }.add(node)
                node.arguments.forEachIndexed { index, argument ->
                    addUse(value = argument, consumer = node, role = InputRole.Argument(index))
                }
            }
            is Node.FieldRead -> {
                fieldReads.getOrPut(node.field) { mutableListOf() }.add(node)
                node.receiver?.let { addUse(value = it, consumer = node, role = InputRole.Receiver) }
            }
            is Node.FieldWrite -> {
                node.receiver?.let { addUse(value = it, consumer = node, role = InputRole.Receiver) }
                addUse(value = node.value, consumer = node, role = InputRole.AssignedValue)
            }
            is Node.NewInstance -> node.constructorArguments.forEachIndexed { index, argument ->
                addUse(value = argument, consumer = node, role = InputRole.Argument(index))
            }
            is Node.OpaqueValue -> node.values.forEachIndexed { index, value ->
                addUse(value = value, consumer = node, role = InputRole.Element(index))
            }
            is Node.TypeCheck -> addUse(value = node.argument, consumer = node, role = InputRole.Operand)
            is Node.DynamicAccess -> {
                addUse(value = node.receiver, consumer = node, role = InputRole.Receiver)
                node.arguments.forEachIndexed { index, argument ->
                    addUse(value = argument, consumer = node, role = InputRole.Argument(index))
                }
            }
            is Node.FunctionReference -> addressTakenFunctions += node.symbol
            is Node.Parameter -> {
                val owner = checkNotNull(function) { "Parameter node outside a function" }
                val functionParameters = parameters.getOrPut(owner.symbol) {
                    arrayOfNulls(owner.symbol.parameters.size)
                }
                check(node.index in functionParameters.indices) {
                    "Parameter ${node.index} is out of range for ${owner.symbol}"
                }
                functionParameters[node.index] = node
            }
            else -> {}
        }
    }

    companion object {
        fun from(executionUnits: List<ExecutionUnit>) = JsDataFlowIndex().apply {
            for (executionUnit in executionUnits) {
                val function = executionUnit as? Function
                if (function != null) {
                    returnOwners[function.returns] = function
                }
                thrownValues += executionUnit.body.throws
                executionUnit.body.forEachNonScopeNode { node ->
                    nodes += node
                    recordNode(node, function)
                }
            }
        }
    }
}
