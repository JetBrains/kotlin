/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIR.Node
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIndex
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsDataFlowIndex.InputRole
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsModuleDFG
import org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow.JsValueFlowAnalysis
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Removes a K-callable wrapper when its observable behavior is equivalent to the raw function.
 *
 * The pass follows each `constructCallableReference(fn, ...)` result through the program-wide
 * [JsModuleDFG] with [JsValueFlowAnalysis]. A wrapper used only as an invocation receiver can be
 * replaced with `fn`. A use that can observe K-callable metadata, wrapper identity, or an
 * unmodeled escape keeps the wrapper.
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
        val policy = KFunctionWrapperPolicy(context, dfg)
        val analysis = JsValueFlowAnalysis(dfg, policy).apply { run() }
        val toUnwrap = policy.seeds.mapNotNullTo(mutableSetOf()) { wrapper ->
            wrapper.irCall.takeUnless { analysis.isObserved(wrapper) }
        }
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
        return toUnwrap.size == policy.seeds.size
    }
}

/**
 * Tracks [Node.CallableReferenceWrapper] values for [JsValueFlowAnalysis].
 *
 * Invocation with the wrapper as receiver is transparent, because it observes the underlying
 * function rather than the wrapper. Equality, hashing, and `typeof` observe the wrapper's identity
 * or K-callable metadata. Reading any property of a wrapper, including its `length`, is already
 * an observation by default.
 */
private class KFunctionWrapperPolicy(
    private val context: JsIrBackendContext,
    dfg: JsModuleDFG,
) : JsValueFlowAnalysis.Policy {
    override val seeds: List<Node.CallableReferenceWrapper> =
        dfg.index.nodes.filterIsInstance<Node.CallableReferenceWrapper>()

    override fun effectOf(value: Node, use: JsDataFlowIndex.Use): JsValueFlowAnalysis.Effect {
        val consumer = use.consumer
        val isWrapper = value is Node.CallableReferenceWrapper
        return when {
            consumer is Node.Call && (consumer.isEqualsOrHashCode() || consumer.isTypeOf()) ->
                JsValueFlowAnalysis.Effect.Observe
            isWrapper && consumer is Node.Call && use.role.isArgument(0) && consumer.isInvoke() ->
                JsValueFlowAnalysis.Effect.Transparent
            isWrapper && consumer is Node.DynamicAccess &&
                    use.role == InputRole.Receiver && consumer.operator == IrDynamicOperator.INVOKE ->
                JsValueFlowAnalysis.Effect.Transparent
            else -> JsValueFlowAnalysis.Effect.Flow
        }
    }

    private fun InputRole.isArgument(index: Int): Boolean = this is InputRole.Argument && this.index == index

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
