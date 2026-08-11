/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.hasAssociativeOpAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.wasm.config.wasmEnableTailCalls

/**
 * Rewrites `return <other> op self(...)` into accumulator-passing form so
 * that the self-call lands in tail position for [WasmTailCallLowering].
 *
 * Eligible operators are those annotated with `@AssociativeOp` in the
 * standard library. The annotation asserts that the operation is associative,
 * which is the algebraic property required for this transformation to
 * preserve semantics.
 */
internal class WasmAccumulatorRecursionLowering(
    private val context: WasmBackendContext,
) : FileLoweringPass {
    companion object {
        /** Marks the synthesized `f$accum` accumulator-passing helpers. */
        val ACCUM_FUNCTION by IrDeclarationOriginImpl.Regular
    }

    private val enabled = context.configuration.wasmEnableTailCalls

    override fun lower(irFile: IrFile) {
        if (!enabled) return

        val allFunctions = mutableListOf<IrSimpleFunction>()
        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                allFunctions += declaration
                declaration.acceptChildrenVoid(this)
            }
        })

        for (func in allFunctions) {
            tryAccumulatorTransform(func)
        }
    }

    /**
     * A `return other op self(...)` site eligible for accumulator
     * transformation. [opCall] must carry `@AssociativeOp` and when
     * the self-call is on the left the other operand must be pure.
     */
    private class AccumSite(
        val returnExpr: IrReturn,
        val opCall: IrCall,
        val recursiveCall: IrCall,
        val recOnRight: Boolean,
    )

    private fun isAccumOp(call: IrCall, classifier: Any?): Boolean {
        val owner = call.symbol.owner
        if (!owner.hasAssociativeOpAnnotation()) return false
        if (owner.parameters.size < 2) return false
        return owner.returnType.classifierOrFail == classifier
    }

    private fun collectAccumSites(func: IrSimpleFunction): List<AccumSite>? {
        val classifier = func.returnType.classifierOrFail
        val selfSymbol = func.symbol
        val sites = mutableListOf<AccumSite>()
        var totalSelfCalls = 0
        var sawTry = false

        func.body?.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
            override fun visitFunction(declaration: IrFunction) {}
            override fun visitClass(declaration: IrClass) {}
            override fun visitTry(aTry: IrTry) {
                sawTry = true
            }

            override fun visitCall(expression: IrCall) {
                if (expression.symbol == selfSymbol) totalSelfCalls++
                super.visitCall(expression)
            }

            override fun visitReturn(expression: IrReturn) {
                if (expression.returnTargetSymbol == selfSymbol) {
                    val value = expression.value
                    if (value is IrCall && isAccumOp(value, classifier)) {
                        val lhs = value.arguments[0]
                        val rhs = value.arguments[1]
                        when {
                            rhs is IrCall && rhs.symbol == selfSymbol ->
                                sites += AccumSite(expression, value, rhs, recOnRight = true)
                            lhs is IrCall && lhs.symbol == selfSymbol && rhs.isPure(anyVariable = true) ->
                                sites += AccumSite(expression, value, lhs, recOnRight = false)
                        }
                    }
                }
                super.visitReturn(expression)
            }
        })

        if (sawTry || sites.isEmpty()) return null
        if (totalSelfCalls != sites.size) return null
        if (sites.map { it.opCall.symbol }.distinct().size != 1) return null
        if (sites.map { it.recOnRight }.distinct().size != 1) return null
        return sites
    }

    private fun tryAccumulatorTransform(func: IrSimpleFunction): Boolean {
        val container = func.parent as? IrDeclarationContainer ?: return false
        val sites = collectAccumSites(func) ?: return false
        val opSymbol = sites.first().opCall.symbol
        val accType = func.returnType
        val bodyCopy = (func.body as? IrBlockBody ?: return false).deepCopyWithSymbols()

        val fAcc = context.irFactory.stageController.restrictTo(func) {
            context.irFactory.addFunction(container) {
                name = Name.identifier(func.name.asString() + "\$accum")
                visibility = DescriptorVisibilities.PRIVATE
                modality = Modality.FINAL
                returnType = accType
                origin = ACCUM_FUNCTION
                startOffset = func.startOffset
                endOffset = func.endOffset
            }.apply {
                for (origParam in func.parameters) {
                    addValueParameter(origParam.name, origParam.type)
                }
                addValueParameter("\$acc", accType)
            }
        }

        buildAccumBody(fAcc, func, bodyCopy, opSymbol, sites.first().recOnRight)
        rewriteOriginal(func, sites, fAcc)
        return true
    }

    private fun rewriteOriginal(
        func: IrSimpleFunction,
        sites: List<AccumSite>,
        fAcc: IrSimpleFunction,
    ) {
        val body = func.body as IrBlockBody
        val builder = context.createIrBuilder(func.symbol, body.startOffset, body.endOffset)
        val siteByReturn = sites.associateBy { it.returnExpr }

        body.transform(object : IrTransformer<Nothing?>() {
            override fun visitReturn(expression: IrReturn, data: Nothing?): IrExpression {
                val site = siteByReturn[expression] ?: return super.visitReturn(expression, data)
                val other = site.opCall.arguments[if (site.recOnRight) 0 else 1]!!
                return builder.irBlock {
                    val seed = createTmpVariable(other, nameHint = "accumSeed")
                    +builder.irReturn(
                        builder.irCall(fAcc.symbol).apply {
                            for (i in site.recursiveCall.arguments.indices) {
                                arguments[i] = site.recursiveCall.arguments[i]
                            }
                            arguments[site.recursiveCall.arguments.size] = builder.irGet(seed)
                        },
                    )
                }
            }
        }, null)
    }

    private fun buildAccumBody(
        fAcc: IrSimpleFunction,
        original: IrSimpleFunction,
        bodyCopy: IrBlockBody,
        opSymbol: IrSimpleFunctionSymbol,
        recOnRight: Boolean,
    ) {
        val builder = context.createIrBuilder(fAcc.symbol, fAcc.startOffset, fAcc.endOffset)
        val accParam = fAcc.parameters.last()
        val origFuncSymbol = original.symbol

        val paramMapping: Map<IrValueSymbol, IrValueSymbol> =
            original.parameters.withIndex().associate { iv ->
                iv.value.symbol to fAcc.parameters[iv.index].symbol
            }
        remapSymbols(bodyCopy, paramMapping)

        // For right-recursive (return other op self), accumulator goes left: acc op other.
        // For left-recursive (return self op other), accumulator goes right: other op acc.
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

        bodyCopy.transform(object : IrTransformer<Nothing?>() {
            override fun visitReturn(expression: IrReturn, data: Nothing?): IrExpression {
                expression.transformChildren(this, data)
                if (expression.returnTargetSymbol != origFuncSymbol) return expression

                val value = expression.value
                val opCall = value as? IrCall
                if (opCall != null && opCall.symbol == opSymbol) {
                    val lhs = opCall.arguments[0]
                    val rhs = opCall.arguments[1]
                    val recCall: IrCall?
                    val other: IrExpression?
                    if (rhs is IrCall && rhs.symbol == origFuncSymbol) {
                        recCall = rhs; other = lhs
                    } else if (lhs is IrCall && lhs.symbol == origFuncSymbol) {
                        recCall = lhs; other = rhs
                    } else {
                        recCall = null; other = null
                    }
                    if (recCall != null && other != null) {
                        return builder.irBlock {
                            val otherTmp = createTmpVariable(other, nameHint = "accumOperand")
                            +builder.irReturn(
                                builder.irCall(fAcc.symbol).apply {
                                    for (i in recCall.arguments.indices) {
                                        arguments[i] = recCall.arguments[i]
                                    }
                                    arguments[recCall.arguments.size] =
                                        builder.irAccOp(builder.irGet(accParam), builder.irGet(otherTmp))
                                },
                            )
                        }
                    }
                }

                // Base case: fold the accumulator into the returned value.
                expression.value = builder.irAccOp(builder.irGet(accParam), value)
                expression.returnTargetSymbol = fAcc.symbol
                return expression
            }
        }, null)

        fAcc.body = bodyCopy
        bodyCopy.patchDeclarationParents(fAcc)
    }

    private fun remapSymbols(element: IrElement, mapping: Map<IrValueSymbol, IrValueSymbol>) {
        if (mapping.isEmpty()) return
        element.transform(ValueRemapper(mapping), null)
    }
}
