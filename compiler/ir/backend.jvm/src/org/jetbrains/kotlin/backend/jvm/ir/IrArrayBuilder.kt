/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.unboxInlineClass
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPropertyGetter

inline fun JvmIrBuilder.irArray(arrayType: IrType, block: IrArrayBuilder.() -> Unit): IrExpression =
    IrArrayBuilder(this, arrayType).apply { block() }.build()

fun JvmIrBuilder.irArrayOf(arrayType: IrType, elements: List<IrExpression> = listOf()): IrExpression =
    irArray(arrayType) { elements.forEach { +it } }

private class IrArrayElement(val expression: IrExpression, val isSpread: Boolean)

class IrArrayBuilder(val builder: JvmIrBuilder, val arrayType: IrType) {
    // We build unboxed arrays for inline classes (UIntArray, etc) by first building
    // an unboxed array of the underlying primitive type, then coercing the result
    // to the correct type.
    val unwrappedArrayType = arrayType.unboxInlineClass()

    // Check if the array type is an inline class wrapper (UIntArray, etc.)
    val isUnboxedInlineClassArray
        get() = arrayType !== unwrappedArrayType

    // The unwrapped element type
    val elementType = unwrappedArrayType.getArrayElementType(builder.context.irBuiltIns)

    private val elements: MutableList<IrArrayElement> = mutableListOf()

    private val hasSpread
        get() = elements.any { it.isSpread }

    operator fun IrExpression.unaryPlus() = add(this)
    fun add(expression: IrExpression) = elements.add(IrArrayElement(expression, false))

    fun addSpread(expression: IrExpression) = elements.add(IrArrayElement(expression, true))

    fun build(): IrExpression {
        val array = when {
            elements.isEmpty() -> newArray(0)
            !hasSpread -> buildSimpleArray()
            elements.size == 1 -> copyArray(elements.single().expression)
            else -> buildComplexArray()
        }
        return coerce(array, arrayType)
    }

    // Construct a new array of the specified size
    private fun newArray(size: Int) = newArray(builder.irInt(size))

    private fun newArray(size: IrExpression): IrExpression {
        val arrayConstructor = if (unwrappedArrayType.isBoxedArray)
            builder.backendContext.ir.symbols.arrayOfNulls
        else
            unwrappedArrayType.classOrNull!!.constructors.single { it.owner.valueParameters.size == 1 }

        return builder.irCall(arrayConstructor, unwrappedArrayType).apply {
            if (typeArgumentsCount != 0)
                putTypeArgument(0, elementType)
            putValueArgument(0, size)
        }
    }

    // Build an array without spreads
    private fun buildSimpleArray(): IrExpression =
        builder.irBlock {
            val result = irTemporary(newArray(elements.size))

            val set = unwrappedArrayType.classOrNull!!.functions.single {
                it.owner.name.asString() == "set"
            }

            for ((index, element) in elements.withIndex()) {
                +irCall(set).apply {
                    dispatchReceiver = irGet(result)
                    putValueArgument(0, irInt(index))
                    putValueArgument(1, coerce(element.expression, elementType))
                }
            }

            +irGet(result)
        }

    // Copy a single spread expression, unless it refers to a newly constructed array.
    private fun copyArray(spread: IrExpression): IrExpression {
        if (spread is IrConstructorCall ||
            (spread is IrFunctionAccessExpression && spread.symbol == builder.backendContext.ir.symbols.arrayOfNulls))
            return spread

        return builder.irBlock {
            val spreadVar = if (spread is IrGetValue) spread.symbol.owner else irTemporary(spread)
            val size = unwrappedArrayType.classOrNull!!.getPropertyGetter("size")!!
            fun getSize() = irCall(size).apply { dispatchReceiver = irGet(spreadVar) }
            val result = irTemporary(newArray(getSize()))
            +irCall(builder.backendContext.ir.symbols.systemArraycopy).apply {
                putValueArgument(0, irGet(spreadVar))
                putValueArgument(1, irInt(0))
                putValueArgument(2, irGet(result))
                putValueArgument(3, irInt(0))
                putValueArgument(4, getSize())
            }
            +irGet(result)
        }
    }

    // Build an array containing spread expressions.
    private fun buildComplexArray(): IrExpression {
        val spreadBuilder = if (unwrappedArrayType.isBoxedArray)
            builder.backendContext.ir.symbols.spreadBuilder
        else
            builder.backendContext.ir.symbols.primitiveSpreadBuilders.getValue(elementType)

        val addElement = spreadBuilder.functions.single { it.name.asString() == "add" }
        val addSpread = spreadBuilder.functions.single { it.name.asString() == "addSpread" }
        val toArray = spreadBuilder.functions.single { it.name.asString() == "toArray" }

        return builder.irBlock {
            val spreadBuilderVar = irTemporary(irCallConstructor(spreadBuilder.constructors.single().symbol, listOf()).apply {
                putValueArgument(0, irInt(elements.size))
            })

            for (element in elements) {
                +irCall(if (element.isSpread) addSpread else addElement).apply {
                    dispatchReceiver = irGet(spreadBuilderVar)
                    putValueArgument(0, if (element.isSpread) element.expression else coerce(element.expression, elementType))
                }
            }

            val toArrayCall = irCall(toArray).apply {
                dispatchReceiver = irGet(spreadBuilderVar)
                if (unwrappedArrayType.isBoxedArray) {
                    val size = spreadBuilder.functions.single { it.name.asString() == "size" }
                    putValueArgument(0, irCall(builder.backendContext.ir.symbols.arrayOfNulls, arrayType).apply {
                        putTypeArgument(0, elementType)
                        putValueArgument(0, irCall(size).apply {
                            dispatchReceiver = irGet(spreadBuilderVar)
                        })
                    })
                }
            }

            if (unwrappedArrayType.isBoxedArray)
                +builder.irImplicitCast(toArrayCall, unwrappedArrayType)
            else
                +toArrayCall
        }
    }

    // Coerce expression to irType if we are working with an inline class array type
    private fun coerce(expression: IrExpression, irType: IrType): IrExpression =
        if (isUnboxedInlineClassArray)
            builder.irCall(builder.backendContext.ir.symbols.unsafeCoerceIntrinsicSymbol, irType).apply {
                putTypeArgument(0, expression.type)
                putTypeArgument(1, irType)
                putValueArgument(0, expression)
            }
        else expression
}
