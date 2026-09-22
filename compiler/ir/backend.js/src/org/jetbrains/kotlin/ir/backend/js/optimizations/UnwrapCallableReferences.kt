/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.Node
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIndex
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIndex.InputRole
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsModuleDFG
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.allOverridden
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.IdentityHashMap

/**
 * Removes a K-callable wrapper when its observable behavior is equivalent to the raw function.
 *
 * The pass follows each `constructCallableReference(fn, ...)` result through the program-wide
 * [JsModuleDFG]. A wrapper used only as an invocation receiver can be replaced with `fn`. A use
 * that can observe K-callable metadata, wrapper identity, or an unmodeled escape keeps the
 * wrapper.
 *
 * Runs after [org.jetbrains.kotlin.ir.backend.js.lower.DeduplicateCallableReferenceFactoriesLowering],
 * so each canonical factory is decided once, and after dead code elimination, so unreachable
 * metadata observers do not affect the decision.
 */
class UnwrapCallableReferences(private val context: JsIrBackendContext) {
    /**
     * Replaces every wrapper proven unnecessary and reports whether all existing wrappers were
     * removed.
     *
     * A true result means the callable-reference reflection support may have become unreachable,
     * so the optimization pipeline should rerun dead code elimination. The result is false when
     * there were no wrappers or when any wrapper must remain.
     */
    fun lower(dfg: JsModuleDFG): Boolean {
        val analyzer = KFunctionWrapperDemandAnalyzer(context, dfg)
        val toUnwrap = analyzer.wrappersToUnwrap()
        if (toUnwrap.isEmpty()) {
            return false
        }

        val transformer = object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()
                if (expression !in toUnwrap) return expression
                return checkNotNull(expression.arguments.first())
            }
        }
        for (module in dfg.modules) {
            module.transformChildrenVoid(transformer)
        }
        return toUnwrap.size == analyzer.wrapperCount
    }
}

/**
 * Determines which [Node.CallableReferenceWrapper] values can have wrapper-specific behavior.
 *
 * The worklist propagates wrappers forward through merges, callee parameters, constructor
 * arguments, field writes and reads, identity-preserving type operations, and function returns.
 * Invocation with the wrapper as receiver is transparent. Any use that can inspect identity or
 * K-callable metadata keeps the wrapper, as does a throw, an opaque escape, virtual dispatch, or
 * flow into code or call sites that the graph cannot enumerate.
 */
private class KFunctionWrapperDemandAnalyzer(
    private val context: JsIrBackendContext,
    private val dfg: JsModuleDFG,
) {
    private val index = dfg.index
    private val wrappers = index.nodes.filterIsInstance<Node.CallableReferenceWrapper>()

    private val reaching = IdentityHashMap<Node, MutableSet<Node.CallableReferenceWrapper>>()
    private val keep = mutableSetOf<Node.CallableReferenceWrapper>()
    private val worklist = ArrayDeque<Node>()

    val wrapperCount: Int get() = wrappers.size

    fun wrappersToUnwrap(): Set<IrCall> {
        if (wrappers.isEmpty()) {
            return emptySet()
        }

        for (wrapper in wrappers) {
            propagate(node = wrapper, wrappers = listOf(wrapper))
        }
        while (worklist.isNotEmpty()) {
            process(node = worklist.removeFirst())
        }

        return wrappers.mapNotNullTo(mutableSetOf()) { wrapper ->
            wrapper.irCall.takeUnless { wrapper in keep }
        }
    }

    private fun propagate(node: Node, wrappers: List<Node.CallableReferenceWrapper>) {
        if (reaching.getOrPut(node) { mutableSetOf() }.addAll(wrappers)) {
            worklist.add(node)
        }
    }

    private fun process(node: Node) {
        val wrappers = reaching[node] ?: return
        val live = wrappers.filter { it !in keep }
        if (live.isEmpty()) {
            return
        }

        if (index.isThrown(node)) {
            keep += live
        }
        index.returnOwner(node)?.let { function -> propagateReturnToCallSites(function, live) }

        index.usesOf(node).forEach { use ->
            when (val consumer = use.consumer) {
                is Node.Variable -> propagate(consumer, live)
                is Node.Call -> handleCallUse(consumer, use.argumentIndex(), live)
                is Node.FieldWrite -> {
                    if (use.role == InputRole.AssignedValue) {
                        index.readsOf(consumer.field).forEach { propagate(it, live) }
                    } else {
                        keep += live
                    }
                }
                is Node.FieldRead -> keep += live
                is Node.TypeCheck ->
                    if (consumer.resultIsArgument) {
                        propagate(consumer, live)
                    } else {
                        keep += live
                    }
                is Node.DynamicAccess -> {
                    val isInvokeReceiver =
                        use.role == InputRole.Receiver && consumer.operator == IrDynamicOperator.INVOKE
                    if (!isInvokeReceiver) keep += live
                }
                is Node.NewInstance -> propagateIntoCallee(consumer.constructor, use.argumentIndex(), live)
                else -> keep += live
            }
        }
    }

    private fun JsDataFlowIndex.Use.argumentIndex(): Int =
        checkNotNull(role as? InputRole.Argument) { "Expected an argument use by $consumer" }.index

    private fun handleCallUse(call: Node.Call, argIndex: Int, live: List<Node.CallableReferenceWrapper>) {
        if (argIndex == 0 && call.isInvoke()) {
            // Calling the wrapper observes the underlying function, not wrapper metadata.
            return
        }
        if (call.isEqualsOrHashCode() || call.isTypeOf() || call is Node.VirtualCall) {
            keep += live
            return
        }
        propagateIntoCallee(call.callee, argIndex, live)
    }

    private fun propagateIntoCallee(
        callee: JsDataFlowIR.FunctionSymbol,
        argIndex: Int,
        live: List<Node.CallableReferenceWrapper>,
    ) {
        if (callee.isExported || callee.isExternal) {
            keep += live
            return
        }
        // No parameter node means the value crosses into a body the graph cannot inspect.
        val paramNode = index.parameterAt(callee, argIndex)
        if (paramNode != null) {
            propagate(paramNode, live)
        } else {
            keep += live
        }
    }

    /**
     * A returned wrapper is observed at every call site of [function] and of the methods it
     * overrides (virtual calls are recorded against the overridden callee). If the call sites
     * cannot be enumerated — the function is exported, address-taken, local, or overrides an
     * exported or external member — the wrapper is kept.
     */
    private fun propagateReturnToCallSites(
        function: JsDataFlowIR.Function,
        live: List<Node.CallableReferenceWrapper>,
    ) {
        val symbol = function.symbol
        val declaration = symbol.irSimpleFunction
        if (symbol.isExported ||
            declaration == null ||
            index.isAddressTaken(symbol) ||
            declaration.parent is IrFunction
        ) {
            keep += live
            return
        }
        considerCallSites(symbol, live)

        for (overridden in declaration.allOverridden()) {
            if (overridden.isEffectivelyExternal()) {
                keep += live
                return
            }
            // An unmapped override has neither a body nor a call site in the linked program.
            val overriddenSymbol = dfg.symbolTable.functionMap[overridden] ?: continue
            if (overriddenSymbol.isExported) {
                keep += live
                return
            }
            considerCallSites(overriddenSymbol, live)
        }
    }

    private fun considerCallSites(symbol: JsDataFlowIR.FunctionSymbol, live: List<Node.CallableReferenceWrapper>) {
        index.callsTo(symbol).forEach { propagate(it, live) }
    }

    private fun Node.Call.isInvoke(): Boolean {
        val function = callee.irFunction
        return function.name == OperatorNameConventions.INVOKE &&
                function.parameters.firstOrNull()?.kind == IrParameterKind.DispatchReceiver
    }

    private fun Node.Call.isEqualsOrHashCode(): Boolean {
        val declaration = callee.irFunction as? IrSimpleFunction ?: return false
        val name = declaration.name
        if (name == OperatorNameConventions.EQUALS || name == OperatorNameConventions.HASH_CODE) {
            return true
        }
        val symbol = declaration.symbol
        return symbol == context.symbols.jsEquals || symbol == context.symbols.jsHashCode
    }

    private fun Node.Call.isTypeOf(): Boolean {
        val declaration = callee.irFunction as? IrSimpleFunction ?: return false
        return declaration.symbol == context.symbols.jsTypeOf
    }
}
