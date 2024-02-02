/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.bir.backend.builders.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirFunctionAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirGetValue
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.utils.classOrNull
import org.jetbrains.kotlin.bir.types.utils.getArrayElementType
import org.jetbrains.kotlin.bir.types.utils.isBoxedArray
import org.jetbrains.kotlin.bir.util.constructors
import org.jetbrains.kotlin.bir.util.functions
import org.jetbrains.kotlin.bir.util.getProperty
import org.jetbrains.kotlin.bir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator

context(JvmBirBackendContext, BirStatementBuilderScope)
inline fun birArray(arrayType: BirType, block: BirArrayBuilder.() -> Unit): BirExpression =
    BirArrayBuilder(arrayType).apply(block).build()

context(JvmBirBackendContext, BirStatementBuilderScope)
fun birArrayOf(arrayType: BirType, elements: List<BirExpression>): BirExpression =
    birArray(arrayType) { elements.forEach { +it } }

private class BirArrayElement(val expression: BirExpression, val isSpread: Boolean)

context(JvmBirBackendContext, BirStatementBuilderScope)
class BirArrayBuilder(val arrayType: BirType) {
    // We build unboxed arrays for inline classes (UIntArray, etc) by first building
    // an unboxed array of the underlying primitive type, then coercing the result
    // to the correct type.
    val unwrappedArrayType = arrayType.unboxInlineClass()

    // Check if the array type is an inline class wrapper (UIntArray, etc.)
    val isUnboxedInlineClassArray
        get() = arrayType !== unwrappedArrayType

    // The unwrapped element type
    val elementType = unwrappedArrayType.getArrayElementType(birBuiltIns)

    private val elements: MutableList<BirArrayElement> = mutableListOf()

    private val hasSpread
        get() = elements.any { it.isSpread }

    operator fun BirExpression.unaryPlus() = add(this)
    fun add(expression: BirExpression) = elements.add(BirArrayElement(expression, false))

    fun addSpread(expression: BirExpression) = elements.add(BirArrayElement(expression, true))

    fun build(): BirExpression {
        val array = when {
            elements.isEmpty() -> newArray(0)
            !hasSpread -> buildSimpleArray()
            elements.size == 1 -> copyArray(elements.single().expression)
            else -> buildComplexArray()
        }
        return coerce(array, arrayType)
    }

    // Construct a new array of the specified size
    private fun newArray(size: Int) = newArray(birConst(size))

    private fun newArray(size: BirExpression): BirExpression {
        val arrayConstructor = if (unwrappedArrayType.isBoxedArray)
            builtInSymbols.arrayOfNulls
        else
            unwrappedArrayType.classOrNull!!.owner.constructors.single { it.valueParameters.size == 1 }.symbol

        return birCallFunctionOrConstructor(arrayConstructor, unwrappedArrayType) {
            if (arrayConstructor.owner.typeParameters.isNotEmpty())
                typeArguments = listOf(elementType)
            valueArguments[0] = size
        }
    }

    // Build an array without spreads
    private fun buildSimpleArray(): BirExpression = birBlock {
        val result = +birTemporaryVariable(newArray(elements.size))

        val set = unwrappedArrayType.classOrNull!!.owner.getSimpleFunction("set")!!

        for ((index, element) in elements.withIndex()) {
            +birCall(set) {
                dispatchReceiver = birGet(result)
                valueArguments[0] = birConst(index)
                valueArguments[1] = coerce(element.expression, elementType)
            }
        }

        +birGet(result)
    }


    // Copy a single spread expression, unless it refers to a newly constructed array.
    private fun copyArray(spread: BirExpression): BirExpression {
        if (spread is BirConstructorCall ||
            (spread is BirFunctionAccessExpression && spread.symbol == birBuiltIns.arrayOfNulls)
        ) return spread

        return birBlock {
            val spreadVar = if (spread is BirGetValue) spread.symbol.owner else +birTemporaryVariable(spread)
            val size = unwrappedArrayType.classOrNull!!.owner.getProperty("size")!!
            val arrayCopyOf = builtInSymbols.arraysClass.owner.functions
                .single { it.name.asString() == "copyOf" && it.valueParameters.getOrNull(0)?.type?.classOrNull?.owner == unwrappedArrayType.classOrNull!! }
            +birCall(arrayCopyOf) {
                valueArguments[0] = coerce(birGet(spreadVar), unwrappedArrayType)
                valueArguments[1] = birCallGetter(size) { dispatchReceiver = birGet(spreadVar) }
            }
        }
    }

    // Build an array containing spread expressions.
    private fun buildComplexArray(): BirExpression {
        val spreadBuilder = if (unwrappedArrayType.isBoxedArray)
            builtInSymbols.spreadBuilder.owner
        else
            builtInSymbols.primitiveSpreadBuilders.getValue(elementType).owner

        val addElement = spreadBuilder.getSimpleFunction("add")!!
        val addSpread = spreadBuilder.getSimpleFunction("addSpread")!!
        val toArray = spreadBuilder.getSimpleFunction("toArray")!!

        return birBlock {
            val spreadBuilderVar = +birTemporaryVariable(
                birCall(spreadBuilder.constructors.single(), typeArguments = emptyList()) {
                    valueArguments[0] = birConst(elements.size)
                }
            )

            for (element in elements) {
                +birCall(if (element.isSpread) addSpread else addElement) {
                    dispatchReceiver = birGet(spreadBuilderVar)
                    valueArguments[0] = coerce(element.expression, if (element.isSpread) unwrappedArrayType else elementType)
                }
            }

            val toArrayCall = birCall(toArray) {
                dispatchReceiver = birGet(spreadBuilderVar)
                if (unwrappedArrayType.isBoxedArray) {
                    val size = spreadBuilder.functions.single { it.name.asString() == "size" }
                    valueArguments[0] = birCall(birBuiltIns.arrayOfNulls.owner, arrayType, listOf(elementType)) {
                        valueArguments[0] = birCall(size) {
                            dispatchReceiver = birGet(spreadBuilderVar)
                        }
                    }
                }
            }

            if (unwrappedArrayType.isBoxedArray)
                +birCast(toArrayCall, unwrappedArrayType, IrTypeOperator.IMPLICIT_CAST)
            else
                +toArrayCall
        }
    }


    // Coerce expression to irType if we are working with an inline class array type
    private fun coerce(expression: BirExpression, irType: BirType): BirExpression =
        if (isUnboxedInlineClassArray)
            birCall(builtInSymbols.unsafeCoerceIntrinsic!!.owner, irType) {
                typeArguments = listOf(expression.type, irType)
                valueArguments[0] = expression
            }
        else expression
}
