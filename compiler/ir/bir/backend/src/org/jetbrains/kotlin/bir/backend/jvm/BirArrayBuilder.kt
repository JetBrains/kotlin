/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.bir.builders.build
import org.jetbrains.kotlin.bir.builders.setCall
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.utils.getArrayElementType

/*inline fun irArray(arrayType: BirType, block: BirArrayBuilder.() -> Unit): BirExpression =
    BirArrayBuilder(arrayType).apply { block() }.build()

fun irArrayOf(arrayType: BirType, elements: List<BirExpression> = listOf()): BirExpression =
    irArray(arrayType) { elements.forEach { +it } }*/

private class BirArrayElement(val expression: BirExpression, val isSpread: Boolean)

context(JvmBirBackendContext)
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

    /*fun build(): BirExpression {
        val array = when {
            elements.isEmpty() -> newArray(0)
            !hasSpread -> buildSimpleArray()
            elements.size == 1 -> copyArray(elements.single().expression)
            else -> buildComplexArray()
        }
        return coerce(array, arrayType)
    }*/

    // Construct a new array of the specified size
    /*private fun newArray(size: Int) = newArray(BirConst.int(value = size))

    private fun newArray(size: BirExpression): BirExpression {
        val arrayConstructor = if (unwrappedArrayType.isBoxedArray)
            builtInSymbols.arrayOfNulls
        else
            unwrappedArrayType.classOrNull!!.constructors.single { it.owner.valueParameters.size == 1 }

        return builder.irCall(arrayConstructor, unwrappedArrayType).apply {
            if (typeArguments.size != 0)
                putTypeArgument(0, elementType)
            putValueArgument(0, size)
        }
    }

    // Build an array without spreads
    private fun buildSimpleArray(): BirExpression =
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
    private fun copyArray(spread: BirExpression): BirExpression {
        if (spread is BirConstructorCall ||
            (spread is BirFunctionAccessExpression && spread.symbol == birBuiltIns.arrayOfNulls))
            return spread

        return builder.irBlock {
            val spreadVar = if (spread is BirGetValue) spread.symbol.owner else irTemporary(spread)
            val size = unwrappedArrayType.classOrNull!!.getPropertyGetter("size")!!
            val arrayCopyOf = birBuiltIns.getArraysCopyOfFunction(unwrappedArrayType as BirSimpleType)
            // TODO consider using System.arraycopy if the requested array type is non-generic.
            +irCall(arrayCopyOf).apply {
                putValueArgument(0, coerce(irGet(spreadVar), unwrappedArrayType))
                putValueArgument(1, irCall(size).apply { dispatchReceiver = irGet(spreadVar) })
            }
        }
    }
*/

/*
     // Build an array containing spread expressions.
     private fun buildComplexArray(): BirExpression {
         val spreadBuilder = if (unwrappedArrayType.isBoxedArray)
             builtInSymbols.spreadBuilder.owner
         else
             builtInSymbols.primitiveSpreadBuilders.getValue(elementType).owner

         val addElement = spreadBuilder.functions.single { it.owner.name.asString() == "add" }
         val addSpread = spreadBuilder.functions.single { it.owner.name.asString() == "addSpread" }
         val toArray = spreadBuilder.functions.single { it.owner.name.asString() == "toArray" }

        return builder.irBlock {
             val spreadBuilderVar = irTemporary(irCallConstructor(spreadBuilder.constructors.single(), listOf()).apply {
                 putValueArgument(0, irInt(elements.size))
             })

             for (element in elements) {
                 +irCall(if (element.isSpread) addSpread else addElement).apply {
                     dispatchReceiver = irGet(spreadBuilderVar)
                     putValueArgument(0, coerce(element.expression, if (element.isSpread) unwrappedArrayType else elementType))
                 }
             }

             val toArrayCall = irCall(toArray).apply {
                 dispatchReceiver = irGet(spreadBuilderVar)
                 if (unwrappedArrayType.isBoxedArray) {
                     val size = spreadBuilder.functions.single { it.owner.name.asString() == "size" }
                     putValueArgument(0, irCall(birBuiltIns.arrayOfNulls, arrayType).apply {
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
     }*/


    // Coerce expression to irType if we are working with an inline class array type
    private fun coerce(expression: BirExpression, irType: BirType): BirExpression =
        if (isUnboxedInlineClassArray)
            BirCall.build {
                setCall(builtInSymbols.unsafeCoerceIntrinsic!!.owner)
                type = irType
                typeArguments += expression.type
                typeArguments += irType
                valueArguments += expression
            }
        else expression
}
