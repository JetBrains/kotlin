/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.ir.normalizeReturnWhen
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.copyFunctionSignatureFrom
import org.jetbrains.kotlin.ir.util.isOverridable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.wasm.config.wasmEnableTailCalls

/**
 * Rewrites `return <other> op self(...)` into accumulator-passing form so
 * that the self-call lands in tail position for [WasmTailCallLowering].
 *
 * Eligible operators are the monoids in [WasmMonoids].
 *
 * The body of `f` moves into a synthesized `f$accum(params..., $acc)`, and
 * `f` itself becomes `return f$accum(params..., identity)`.
 */
internal class WasmAssociativeReductionLowering(
    private val context: WasmBackendContext,
) : BodyLoweringPass {
    companion object {
        val ACCUM_FUNCTION by IrDeclarationOriginImpl.Regular
    }

    override fun lower(irModule: IrModuleFragment) {
        if (context.configuration.wasmEnableTailCalls) {
            super.lower(irModule)
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val function = container as? IrSimpleFunction ?: return
        val body = irBody as? IrBlockBody ?: return
        tryAccumulatorTransform(function, body)
    }

    // A `return other op self(...)` site eligible for accumulator transformation.
    private class AccumSite(
        val opCall: IrCall,
        val recursiveCall: IrCall,
        val recOnRight: Boolean,
        val identity: IrBuilder.() -> IrExpression,
        // True when reassociateRight must reorder the tree before the site can be transformed.
        val reassociate: Boolean = false,
    )

    // All accumulator-eligible sites collected from one function.
    private data class Reduction(
        val sites: List<AccumSite>,
        val opSymbol: IrSimpleFunctionSymbol,
        val recOnRight: Boolean,
        val identity: IrBuilder.() -> IrExpression,
    )

    private fun accumIdentityOf(call: IrCall, accumulatorType: IrType): (IrBuilder.() -> IrExpression)? {
        if (call.symbol.owner.returnType != accumulatorType) return null
        return WasmMonoids.identities[WasmMonoids.of(call) ?: return null]
    }

    // isPure rejects all IrCall nodes, but monoid operators are known to be
    // side-effect-free. This extends the check to accept them, provided every
    // argument has the operator's return type. The type constraint excludes
    // cases where the operator calls user code on a wider parameter type,
    // such as toString() in String.plus(other: Any?).
    private fun IrExpression.isPureOperand(): Boolean =
        isPure(anyVariable = true, checkFields = false) ||
                (this is IrCall && WasmMonoids.of(this) != null && arguments.all { it != null && it.type == type && it.isPureOperand() })

    // Finds the self-call at the end of `lhs op (x op ... op self(...))`, or null.
    private fun rightChainSelfCall(outerCall: IrCall, recursiveFunctionSymbol: IrSimpleFunctionSymbol): IrCall? {
        val rhs = outerCall.arguments[1] as? IrCall ?: return null
        if (rhs.symbol == recursiveFunctionSymbol) return rhs
        if (rhs.symbol != outerCall.symbol) return null
        return rightChainSelfCall(rhs, recursiveFunctionSymbol)
    }

    // Rewrites `lhs op (x op ... op self(...))` into `(lhs op x op ...) op self(...)`,
    // moving the self-call to the top level. Every level must use the same operator,
    // because reordering across different operators changes the result.
    private fun reassociateRight(outerCall: IrCall, recursiveFunctionSymbol: IrSimpleFunctionSymbol): IrCall? {
        val rhs = outerCall.arguments[1] as? IrCall ?: return null
        if (rhs.symbol != outerCall.symbol) return null
        val innerRhs = rhs.arguments[1]
        val selfCall = if (innerRhs is IrCall && innerRhs.symbol == recursiveFunctionSymbol) {
            innerRhs
        } else {
            reassociateRight(rhs, recursiveFunctionSymbol) ?: return null
        }
        val x = rhs.arguments[0]
        rhs.arguments[0] = outerCall.arguments[0]
        rhs.arguments[1] = x
        outerCall.arguments[0] = rhs
        outerCall.arguments[1] = selfCall
        return selfCall
    }

    // Checks whether a `return other op self(...)` expression is eligible for
    // accumulator transformation, and returns an AccumSite if so.
    private fun accumSiteOf(
        returnExpr: IrReturn,
        recursiveFunctionSymbol: IrSimpleFunctionSymbol,
        accumulatorType: IrType,
    ): AccumSite? {
        if (returnExpr.returnTargetSymbol != recursiveFunctionSymbol) return null
        val opCall = returnExpr.value as? IrCall ?: return null
        val identity = accumIdentityOf(opCall, accumulatorType) ?: return null
        val lhs = opCall.arguments[0]
        val rhs = opCall.arguments[1]
        return when {
            rhs is IrCall && rhs.symbol == recursiveFunctionSymbol ->
                AccumSite(opCall, rhs, recOnRight = true, identity)
            // Left-recursive: the other operand must be pure and of the accumulator type,
            // because the transformation moves its evaluation before the recursive call.
            // String is excluded because `self(...) + s` would become `s + acc`, prepending to
            // the growing accumulator on every step instead of appending a short operand.
            lhs is IrCall && lhs.symbol == recursiveFunctionSymbol && rhs != null &&
                    rhs.type == accumulatorType && rhs.isPureOperand() && !accumulatorType.isString() ->
                AccumSite(opCall, lhs, recOnRight = false, identity)
            // The self-call is nested: `lhs op (x op ... op self(...))`. Reassociation
            // can move it to the top level so it becomes a direct operand of opCall.
            else ->
                rightChainSelfCall(opCall, recursiveFunctionSymbol)?.let { selfCall ->
                    AccumSite(opCall, selfCall, recOnRight = true, identity, reassociate = true)
                }
        }
    }

    private fun IrSimpleFunction.hasSelfCall(): Boolean {
        var found = false
        body?.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (!found) element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                if (expression.symbol == this@hasSelfCall.symbol) found = true else super.visitCall(expression)
            }
        })
        return found
    }

    private fun collectAccumSites(function: IrSimpleFunction): Reduction? {
        val accumulatorType = function.returnType
        val sites = mutableListOf<AccumSite>()

        function.body?.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            // Self-calls inside `try` cannot become tail calls. Their returns are
            // left as-is, and visitReturn's fallback wraps the value with the accumulator.
            override fun visitTry(aTry: IrTry) {}

            override fun visitReturn(expression: IrReturn) {
                accumSiteOf(expression, function.symbol, accumulatorType)?.let { sites += it }
                super.visitReturn(expression)
            }
        })

        val first = sites.firstOrNull() ?: return null
        // All sites must use the same operator and place the self-call on the same side,
        // otherwise a single accumulator cannot serve them all.
        if (sites.any { it.opCall.symbol != first.opCall.symbol || it.recOnRight != first.recOnRight }) return null
        return Reduction(sites, first.opCall.symbol, first.recOnRight, first.identity)
    }

    private fun tryAccumulatorTransform(function: IrSimpleFunction, body: IrBlockBody) {
        // Transforming an overridable function would replace virtual self-calls
        // with direct calls to the private helper, silently ignoring subclass overrides.
        if (function.isOverridable) return
        // Functions with type parameters are skipped. The helper gets copies of the type
        // parameters, but the moved body and both helper calls would still have to be
        // remapped to the copies.
        if (function.typeParameters.isNotEmpty()) return
        val container = function.parent as? IrDeclarationContainer ?: return
        if (!function.hasSelfCall()) return
        // An expression body with `if` is `return when { ... }`, which hides the
        // branch results from the site search. If the function turns out not to be
        // transformable, the pushed-in returns are semantically equivalent.
        normalizeReturnWhen(function)
        val reduction = collectAccumSites(function) ?: return

        val accumFunction = context.irFactory.stageController.restrictTo(function) {
            context.irFactory.addFunction(container) {
                name = Name.identifier(function.name.asString() + "\$accum")
                visibility = DescriptorVisibilities.PRIVATE
                modality = Modality.FINAL
                origin = ACCUM_FUNCTION
                startOffset = function.startOffset
                endOffset = function.endOffset
            }.apply {
                copyFunctionSignatureFrom(function)
                addValueParameter("\$acc", returnType)
            }
        }

        buildAccumBody(accumFunction, function, reduction)

        function.body = context.createIrBuilder(function.symbol, body.startOffset, body.endOffset).irBlockBody {
            +irReturn(
                irCall(accumFunction.symbol).apply {
                    function.parameters.forEachIndexed { index, parameter ->
                        arguments[index] = irGet(parameter)
                    }
                    arguments[function.parameters.size] = reduction.identity(this@irBlockBody)
                }
            )
        }
    }

    private fun buildAccumBody(accumFunction: IrSimpleFunction, original: IrSimpleFunction, reduction: Reduction) {
        val (sites, opSymbol, recOnRight, identity) = reduction
        val accParam = accumFunction.parameters.last()
        // New nodes take the offsets of the return or call they replace, so that
        // stack traces and source maps keep pointing at that line.
        fun builderAt(element: IrElement) = context.createIrBuilder(accumFunction.symbol, element.startOffset, element.endOffset)
        // moveBodyTo re-creates the IrReturn nodes, so sites are looked up by their operator call.
        val siteByOpCall = sites.associateBy { it.opCall }

        val parameterMapping: Map<IrValueParameter, IrValueDeclaration> =
            original.parameters.zip(accumFunction.parameters).toMap()
        val body = original.moveBodyTo(accumFunction, parameterMapping) as IrBlockBody

        // The accumulator and the new operand must be on the same sides as in the
        // original call, because the operator may not be commutative.
        fun IrBuilderWithScope.irAccOp(acc: IrExpression, other: IrExpression): IrExpression =
            irCall(opSymbol).apply {
                if (recOnRight) {
                    arguments[0] = acc
                    arguments[1] = other
                } else {
                    arguments[0] = other
                    arguments[1] = acc
                }
            }

        fun IrBuilderWithScope.irAccumCall(recursiveCall: IrCall, acc: IrExpression): IrExpression =
            irCall(accumFunction.symbol).apply {
                recursiveCall.arguments.forEachIndexed { index, argument ->
                    arguments[index] = argument
                }
                arguments[recursiveCall.arguments.size] = acc
            }

        body.transform(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()
                if (expression.symbol != original.symbol) return expression
                val builder = builderAt(expression)
                return builder.irAccumCall(expression, builder.identity())
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                if (expression.returnTargetSymbol != accumFunction.symbol) return super.visitReturn(expression)
                val value = expression.value
                val builder = builderAt(expression)

                val site = siteByOpCall[value]
                if (site != null) {
                    if (site.reassociate) reassociateRight(site.opCall, original.symbol)
                    // Rewrite only the self-call's arguments and the other operand. The
                    // self-call itself becomes the tail call below; passing it through
                    // visitCall would turn it into a non-tail helper call.
                    site.recursiveCall.transformChildrenVoid()
                    val other = site.opCall.arguments[if (site.recOnRight) 0 else 1]!!.transform(this, null)
                    return if (site.recOnRight) {
                        // The source evaluates `other` before the self-call's arguments.
                        builder.irBlock {
                            val otherTmp = createTmpVariable(other, nameHint = "accumOperand")
                            +irReturn(irAccumCall(site.recursiveCall, irAccOp(irGet(accParam), irGet(otherTmp))))
                        }
                    } else {
                        // `other` is pure, so it may follow the self-call's arguments as in the source.
                        builder.irReturn(builder.irAccumCall(site.recursiveCall, builder.irAccOp(builder.irGet(accParam), other)))
                    }
                }

                if (value is IrCall && value.symbol == original.symbol) {
                    value.transformChildrenVoid()
                    return builder.irReturn(builder.irAccumCall(value, builder.irGet(accParam)))
                }

                expression.transformChildrenVoid()
                expression.value = builder.irAccOp(builder.irGet(accParam), expression.value)
                return expression
            }
        }, null)

        accumFunction.body = body
    }
}
