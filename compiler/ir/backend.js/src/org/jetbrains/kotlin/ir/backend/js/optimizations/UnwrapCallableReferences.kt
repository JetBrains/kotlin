/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.Node
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsModuleDFG
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
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
 * Rewrites `constructCallableReference(fn, ...)` to plain `fn` when whole-program analysis over
 * the [JsModuleDFG] proves the K-callable metadata attached by the wrapper (`.name`, arity,
 * reference equality, `is KFunction`, …) is never observed.
 *
 * [KFunctionWrapperDemandAnalyzer] follows each wrapper through the graph: a wrapper that is
 * only ever *called* behaves identically to the raw function and is unwrapped; any use that
 * could observe its identity or metadata — or any flow the graph cannot follow — keeps it.
 *
 * Runs after [org.jetbrains.kotlin.ir.backend.js.lower.DeduplicateCallableReferenceFactoriesLowering],
 * so each canonical factory is decided once, and after dead code elimination, so unreachable
 * metadata observers (e.g. a SAM adapter's generated `equals`) do not force keeps.
 */
class UnwrapCallableReferences(private val context: JsIrBackendContext) {
    /**
     * Returns `true` when every wrapper in the program was unwrapped — the reflection runtime
     * (`constructCallableReference` and what only it uses) may then be dead.
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
 * Worklist analysis: every [Node.CallableReferenceWrapper] flows forward through phis, callee
 * parameters (constructor arguments included), field writes to same-field reads, passthrough
 * casts, and function returns to call sites. A wrapper is kept when it reaches
 * `equals`/`hashCode`, `jsTypeOf` (what `is KFunction` lowers to), a K-metadata field read,
 * non-INVOKE dynamic access, an exported/external/bodiless callee, an overridable virtual call,
 * a throw, an escape ([Node.OpaqueValue]), or the return of a function whose call sites cannot
 * be enumerated. Invocation with the wrapper as receiver never keeps.
 */
private class KFunctionWrapperDemandAnalyzer(
    private val context: JsIrBackendContext,
    private val dfg: JsModuleDFG,
) {
    private val index = UseIndex().apply {
        for (function in dfg.functions) {
            functionByReturns[function.body.returns] = function
            throwsNodes[function.body.throws] = function
            function.body.forEachNonScopeNode { recordNode(it, function) }
        }
    }

    private val reaching = IdentityHashMap<Node, MutableSet<Node.CallableReferenceWrapper>>()
    private val keep = mutableSetOf<Node.CallableReferenceWrapper>()
    private val worklist = ArrayDeque<Node>()

    val wrapperCount: Int get() = index.wrappers.size

    fun wrappersToUnwrap(): Set<IrCall> {
        if (index.wrappers.isEmpty()) {
            return emptySet()
        }

        for (wrapper in index.wrappers) {
            propagate(node = wrapper, wrappers = listOf(wrapper))
        }
        while (worklist.isNotEmpty()) {
            process(node = worklist.removeFirst())
        }

        return index.wrappers.mapNotNullTo(mutableSetOf()) { wrapper ->
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
        val live = wrappers.filterNot { it in keep }
        if (live.isEmpty()) {
            return
        }

        if (index.throwsNodes[node] != null) {
            keep += live
        }
        index.functionByReturns[node]?.let { propagateReturnToCallSites(it, live) }

        index.uses[node]?.forEach { use ->
            when (val consumer = use.consumer) {
                is Node.Variable -> propagate(consumer, live)
                is Node.Call -> handleCallUse(consumer, use.argIndex, live)
                is Node.FieldWrite -> index.fieldReads[consumer.field]?.forEach { propagate(it, live) }
                is Node.FieldRead ->
                    if (consumer.field.isKMetadata()) keep += live else propagate(consumer, live)
                is Node.TypeCheck ->
                    if (consumer.passthrough) propagate(consumer, live) else keep += live
                is Node.DynamicAccess -> {
                    val isInvokeReceiver = use.argIndex == null && consumer.operator == IrDynamicOperator.INVOKE
                    if (!isInvokeReceiver) keep += live
                }
                is Node.NewInstance -> propagateIntoCallee(consumer.constructor, use.argIndex, live)
                else -> keep += live
            }
        }
    }

    private fun handleCallUse(call: Node.Call, argIndex: Int?, live: List<Node.CallableReferenceWrapper>) {
        if (argIndex == 0 && call.isInvoke()) {
            // The wrapper is the "invoke" receiver: invocation does not observe its metadata.
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
        argIndex: Int?,
        live: List<Node.CallableReferenceWrapper>,
    ) {
        if (argIndex == null || callee.isExported || callee.isExternal) {
            keep += live
            return
        }
        // The parameter node exists only when the callee's body was analyzed. Without it the
        // wrapper crosses into code the graph cannot see — it could read `.name`, compare the
        // reference — so it must keep its metadata.
        val paramNode = index.parameters[callee]?.getOrNull(argIndex)
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
            symbol in index.referencedFunctions ||
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
            // An unmapped overridden method has nobody and no call in the program.
            val overriddenSymbol = dfg.symbolTable.functionMap[overridden] ?: continue
            if (overriddenSymbol.isExported) {
                keep += live
                return
            }
            considerCallSites(overriddenSymbol, live)
        }
    }

    private fun considerCallSites(symbol: JsDataFlowIR.FunctionSymbol, live: List<Node.CallableReferenceWrapper>) {
        index.callSites[symbol]?.forEach { propagate(it, live) }
    }

    private fun Node.Call.isInvoke(): Boolean {
        val function = callee.irFunction ?: return false
        return function.name == OperatorNameConventions.INVOKE &&
                function.parameters.firstOrNull()?.kind == IrParameterKind.DispatchReceiver
    }

    private fun Node.Call.isEqualsOrHashCode(): Boolean {
        val declaration = callee.irDeclaration as? IrSimpleFunction ?: return false
        val name = declaration.name
        if (name == OperatorNameConventions.EQUALS || name == OperatorNameConventions.HASH_CODE) {
            return true
        }
        val symbol = declaration.symbol
        return symbol == context.symbols.jsEquals || symbol == context.symbols.jsHashCode
    }

    private fun Node.Call.isTypeOf(): Boolean {
        val declaration = callee.irDeclaration as? IrSimpleFunction ?: return false
        return declaration.symbol == context.symbols.jsTypeOf
    }

    private fun JsDataFlowIR.Field.isKMetadata(): Boolean {
        val n = name ?: return false
        return n == Namer.KCALLABLE_NAME ||
                n == Namer.KCALLABLE_ARITY ||
                n == Namer.KCALLABLE_FLAGS ||
                n == Namer.KCALLABLE_ID ||
                n == Namer.KCALLABLE_BOUND_VALUES
    }
}

private class UseSite(
    val consumer: Node,
    val argIndex: Int? = null,
)

/**
 * Reverse-flow index over the whole-program DFG: for each node, the consumers that read it,
 * plus call sites per callee, parameter nodes per function, return/throw phis, field reads per
 * field, functions whose address is taken, and every wrapper call.
 */
private class UseIndex {
    val uses = IdentityHashMap<Node, MutableList<UseSite>>()
    val callSites = IdentityHashMap<JsDataFlowIR.FunctionSymbol, MutableList<Node.Call>>()
    val parameters = IdentityHashMap<JsDataFlowIR.FunctionSymbol, Array<Node.Parameter?>>()
    val functionByReturns = IdentityHashMap<Node.Variable, JsDataFlowIR.Function>()
    val throwsNodes = IdentityHashMap<Node.Variable, JsDataFlowIR.Function>()
    val fieldReads = IdentityHashMap<JsDataFlowIR.Field, MutableList<Node.FieldRead>>()
    val referencedFunctions = mutableSetOf<JsDataFlowIR.FunctionSymbol>()
    val wrappers = mutableListOf<Node.CallableReferenceWrapper>()

    fun addUse(value: Node, consumer: Node, argIndex: Int? = null) {
        uses.getOrPut(value) { mutableListOf() }.add(UseSite(consumer, argIndex))
    }

    fun recordNode(node: Node, function: JsDataFlowIR.Function) {
        when (node) {
            is Node.Variable -> node.values.forEach { addUse(it, node) }
            is Node.Call -> {
                callSites.getOrPut(node.callee) { mutableListOf() }.add(node)
                node.arguments.forEachIndexed { index, edge -> addUse(edge, node, index) }
                if (node is Node.CallableReferenceWrapper) {
                    wrappers += node
                }
            }
            is Node.FieldRead -> {
                fieldReads.getOrPut(node.field) { mutableListOf() }.add(node)
                node.receiver?.let { addUse(it, node) }
            }
            is Node.FieldWrite -> {
                node.receiver?.let { addUse(it, node) }
                addUse(node.value, node)
            }
            is Node.NewInstance ->
                node.constructorArguments.forEachIndexed { index, edge -> addUse(edge, node, index) }
            is Node.OpaqueValue -> node.values.forEach { addUse(it, node) }
            is Node.TypeCheck -> addUse(node.argument, node)
            is Node.DynamicAccess -> {
                addUse(node.receiver, node)
                node.arguments.forEachIndexed { index, edge -> addUse(edge, node, index) }
            }
            is Node.FunctionReference -> referencedFunctions += node.symbol
            is Node.Parameter -> {
                val params = parameters.getOrPut(function.symbol) {
                    arrayOfNulls(function.symbol.parameters.size)
                }
                check(node.index in params.indices) {
                    "Parameter ${node.index} is out of range for ${function.symbol}"
                }
                params[node.index] = node
            }
            else -> {}
        }
    }
}
