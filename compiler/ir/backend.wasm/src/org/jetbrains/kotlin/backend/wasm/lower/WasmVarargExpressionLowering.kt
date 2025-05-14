/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irComposite
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.getArrayElementType
import org.jetbrains.kotlin.ir.util.isBoxedArray
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class WasmVarargExpressionLowering(
    private val context: WasmBackendContext
) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    val symbols = context.wasmSymbols

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    // Helper which wraps an array class and allows to access it's commonly used methods.
    private class ArrayDescr(val arrayType: IrType, val context: WasmBackendContext) {
        val arrayClass =
            arrayType.getClass() ?: throw IllegalArgumentException("Argument ${arrayType.render()} must have a class")

        init {
            check(arrayClass.symbol in context.wasmSymbols.arrays) { "Argument ${ir2string(arrayClass)} must be an array" }
        }

        val isUnsigned
            get() = arrayClass.symbol in context.wasmSymbols.unsignedTypesToUnsignedArrays.values

        val primaryConstructor: IrConstructor
            get() =
                if (isUnsigned)
                    arrayClass.constructors.find { it.parameters.singleOrNull()?.type == context.irBuiltIns.intType }!!
                else arrayClass.primaryConstructor!!

        val constructors
            get() = arrayClass.constructors

        val setMethod
            get() = arrayClass.getSimpleFunction("set")!!.owner
        val getMethod
            get() = arrayClass.getSimpleFunction("get")!!.owner
        val sizeMethod
            get() = arrayClass.getPropertyGetter("size")!!.owner
        val elementType: IrType
            get() {
                if (arrayType.isBoxedArray)
                    return arrayType.getArrayElementType(context.irBuiltIns)
                // getArrayElementType doesn't work on unsigned arrays, use workaround instead
                return getMethod.returnType
            }

        val copyInto: IrSimpleFunction
            get() {
                val func = context.wasmSymbols.arraysCopyInto.find { symbol ->
                    symbol.owner.let {
                        it.hasShape(extensionReceiver = true, regularParameters = 4) &&
                                it.parameters[0].type.classOrNull?.owner == arrayClass
                    }
                }

                return func?.owner ?: throw IllegalArgumentException("copyInto is not found for ${arrayType.render()}")
            }
    }

    private fun IrBlockBuilder.irCreateArray(size: IrExpression, arrDescr: ArrayDescr) =
        irCall(arrDescr.primaryConstructor).apply {
            arguments[0] = size
            if (typeArguments.isNotEmpty()) {
                check(typeArguments.size == 1 && arrDescr.arrayClass.typeParameters.size == 1)
                typeArguments[0] = arrDescr.elementType
            }
            type = arrDescr.arrayType
        }

    // Represents single contiguous sequence of vararg arguments. It can generate IR for various operations on this
    // segments. It's used to handle spreads and normal vararg arguments in a uniform manner.
    private sealed class VarargSegmentBuilder(val wasmContext: WasmBackendContext) {
        // Returns an expression which calculates size of this spread.
        abstract fun IrBlockBuilder.irSize(): IrExpression

        // Adds code into the current block which copies this spread into destArr.
        // If indexVar is present uses it as a start index in the destination array.
        abstract fun IrBlockBuilder.irCopyInto(destArr: IrVariable, indexVar: IrVariable?)

        class Plain(val exprs: List<IrVariable>, wasmContext: WasmBackendContext) :
            VarargSegmentBuilder(wasmContext) {

            override fun IrBlockBuilder.irSize() = irInt(exprs.size)

            override fun IrBlockBuilder.irCopyInto(destArr: IrVariable, indexVar: IrVariable?) {
                val destArrDescr = ArrayDescr(destArr.type, wasmContext)

                // An infinite sequence of natural numbers possibly shifted by the indexVar when it's available
                val indexes = generateSequence(0) { it + 1 }
                    .map { irInt(it) }
                    .let { seq ->
                        if (indexVar != null) seq.map { irIntPlus(irGet(indexVar), it, wasmContext) }
                        else seq
                    }

                for ((element, index) in exprs.asSequence().zip(indexes)) {
                    +irCall(destArrDescr.setMethod).apply {
                        dispatchReceiver = irGet(destArr)
                        arguments[1] = index
                        arguments[2] = irGet(element)
                    }
                }
            }
        }

        class Spread(val exprVar: IrVariable, wasmContext: WasmBackendContext) :
            VarargSegmentBuilder(wasmContext) {

            val srcArrDescr = ArrayDescr(exprVar.type, wasmContext) // will check that exprVar is an array

            override fun IrBlockBuilder.irSize(): IrExpression =
                irCall(srcArrDescr.sizeMethod).apply {
                    dispatchReceiver = irGet(exprVar)
                }

            override fun IrBlockBuilder.irCopyInto(destArr: IrVariable, indexVar: IrVariable?) {
                assert(srcArrDescr.arrayClass == destArr.type.getClass()) { "type checker failure?" }

                val destIdx = indexVar?.let { irGet(it) } ?: irInt(0)

                +irCall(srcArrDescr.copyInto).apply {
                    if (typeArguments.isNotEmpty()) {
                        check(typeArguments.size == 1 && srcArrDescr.arrayClass.typeParameters.size == 1)
                        typeArguments[0] = srcArrDescr.elementType
                    }
                    arguments[0] = irGet(exprVar) // source
                    arguments[1] = irGet(destArr) // destination
                    arguments[2] = destIdx        // destinationOffset
                    arguments[3] = irInt(0)       // startIndex
                    arguments[4] = irSize()       // endIndex
                }
            }
        }
    }

    // This is needed to setup proper extension and dispatch receivers for the VarargSegmentBuilder.
    private fun IrBlockBuilder.irCopyInto(destArr: IrVariable, indexVar: IrVariable?, segment: VarargSegmentBuilder) =
        with(segment) {
            this@irCopyInto.irCopyInto(destArr, indexVar)
        }

    private fun IrBlockBuilder.irSize(segment: VarargSegmentBuilder) =
        with(segment) {
            this@irSize.irSize()
        }

    private fun tryVisitWithNoSpread(irVararg: IrVararg, builder: DeclarationIrBuilder): IrExpression {
        val irVarargType = irVararg.type
        if (!irVarargType.isUnsignedArray()) return irVararg

        val unsignedConstructor = irVarargType.getClass()!!.primaryConstructor!!
        val constructorParameterType = unsignedConstructor.parameters[0].type
        val signedElementType = constructorParameterType.getArrayElementType(context.irBuiltIns)

        irVararg.type = constructorParameterType
        irVararg.varargElementType = signedElementType
        return builder.irCall(unsignedConstructor.symbol, irVarargType).also {
            it.arguments[0] = irVararg
        }
    }

    override fun visitVararg(expression: IrVararg): IrExpression {
        // Optimization in case if we have a single spread element
        val singleSpreadElement = expression.elements.singleOrNull() as? IrSpreadElement
        if (singleSpreadElement != null) {
            val spreadExpr = singleSpreadElement.expression
            if (isImmediatelyCreatedArray(spreadExpr))
                return spreadExpr.transform(this, null)
        }

        // Lower nested varargs
        val irVararg = super.visitVararg(expression) as IrVararg
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol)

        if (irVararg.elements.none { it is IrSpreadElement }) {
            return tryVisitWithNoSpread(irVararg, builder)
        }

        // Create temporary variable for each element and emit them all at once to preserve
        // argument evaluation order as per kotlin language spec.
        val elementVars = irVararg.elements
            .map {
                val exp = if (it is IrSpreadElement) it.expression else (it as IrExpression)
                currentScope!!.scope.createTemporaryVariable(exp, "vararg_temp")
            }

        val segments: List<VarargSegmentBuilder> = sequence {
            val currentElements = mutableListOf<IrVariable>()

            for ((el, tempVar) in irVararg.elements.zip(elementVars)) {
                when (el) {
                    is IrExpression -> currentElements.add(tempVar)
                    is IrSpreadElement -> {
                        if (currentElements.isNotEmpty()) {
                            yield(VarargSegmentBuilder.Plain(currentElements.toList(), context))
                            currentElements.clear()
                        }
                        yield(VarargSegmentBuilder.Spread(tempVar, context))
                    }
                }
            }
            if (currentElements.isNotEmpty())
                yield(VarargSegmentBuilder.Plain(currentElements.toList(), context))
        }.toList()

        val destArrayDescr = ArrayDescr(irVararg.type, context)
        return builder.irComposite(irVararg) {
            // Emit all of the variables first so that all vararg expressions
            // are evaluated only once and in order of their appearance.
            elementVars.forEach { +it }

            val arrayLength = segments
                .map { irSize(it) }
                .reduceOrNull { acc, exp -> irIntPlus(acc, exp) }
                ?: irInt(0)
            val arrayTempVariable = irTemporary(
                value = irCreateArray(arrayLength, destArrayDescr),
                nameHint = "vararg_array")
            val indexVar = if (segments.size >= 2) irTemporary(irInt(0), "vararg_idx") else null

            segments.forEach {
                irCopyInto(arrayTempVariable, indexVar, it)

                if (indexVar != null)
                    +irSet(indexVar, irIntPlus(irGet(indexVar), irSize(it)))
            }

            +irGet(arrayTempVariable)
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) =
        transformFunctionAccessExpression(expression)

    private fun transformFunctionAccessExpression(expression: IrFunctionAccessExpression): IrExpression {
        // Replace not-existing vararg arguments with vararg arguments without elements
        expression.arguments.forEachIndexed { argumentIdx, argument ->
            val parameter = expression.symbol.owner.parameters[argumentIdx]
            val varargElementType = parameter.varargElementType
            if (argument == null && varargElementType != null) {
                expression.arguments[argumentIdx] =
                    IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.type, varargElementType)
            }
        }

        expression.transformChildrenVoid()
        return expression
    }

    private fun IrBlockBuilder.irIntPlus(rhs: IrExpression, lhs: IrExpression): IrExpression =
        irIntPlus(rhs, lhs, this@WasmVarargExpressionLowering.context)

    private fun isImmediatelyCreatedArray(expr: IrExpression): Boolean =
        when (expr) {
            is IrFunctionAccessExpression -> {
                val arrDescr = ArrayDescr(expr.type, context)
                expr.symbol.owner in arrDescr.constructors || expr.symbol == context.wasmSymbols.arrayOfNulls
            }
            is IrTypeOperatorCall -> isImmediatelyCreatedArray(expr.argument)
            is IrComposite ->
                expr.statements.size == 1 &&
                        expr.statements[0] is IrExpression &&
                        isImmediatelyCreatedArray(expr.statements[0] as IrExpression)
            is IrVararg -> true // Vararg always produces a fresh array
            else -> false
        }
}

private fun IrBlockBuilder.irIntPlus(rhs: IrExpression, lhs: IrExpression, wasmContext: WasmBackendContext): IrExpression {
    val plusOp = wasmContext.wasmSymbols.getBinaryOperator(
        OperatorNameConventions.PLUS, context.irBuiltIns.intType, context.irBuiltIns.intType
    ).owner

    return irCall(plusOp).apply {
        dispatchReceiver = rhs
        arguments[1] = lhs
    }
}
