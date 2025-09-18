/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasShape

class PrimitiveContainerMemberCallTransformer(private val context: JsIrBackendContext) : CallsTransformer {
    private val symbols = context.symbols

    private val symbolToTransformer: SymbolToTransformer = hashMapOf()

    init {
        symbolToTransformer.run {
            // Arrays
            add(context.symbols.array.sizeProperty, context.symbols.jsArrayLength)
            add(context.symbols.array.getFunction, context.symbols.jsArrayGet)
            add(context.symbols.array.setFunction, context.symbols.jsArraySet)
            add(context.symbols.array.iterator, context.symbols.jsArrayIteratorFunction)
            for ((key, elementType) in context.symbols.primitiveArrays) {
                add(key.sizeProperty, context.symbols.jsArrayLength)
                add(key.getFunction, context.symbols.jsArrayGet)
                add(key.setFunction, context.symbols.jsArraySet)
                add(key.iterator, context.symbols.jsPrimitiveArrayIteratorFunctions[elementType]!!)

                // TODO irCall?
                add(key.sizeConstructor) { call ->
                    IrCallImpl(
                        call.startOffset,
                        call.endOffset,
                        call.type,
                        context.symbols.primitiveToSizeConstructor[elementType]!!,
                        typeArgumentsCount = 0
                    ).apply {
                        arguments[0] = call.arguments[0]
                    }
                }
            }

            add(context.irBuiltIns.stringClass.hashCodeFunction, symbols.jsGetStringHashCode)
            add(context.irBuiltIns.stringClass.lengthProperty, context.symbols.jsArrayLength)
            add(context.irBuiltIns.stringClass.getFunction, symbols.jsCharCodeAt)
            add(context.irBuiltIns.stringClass.subSequence, context.symbols.subStringFunction)
            add(symbols.charSequenceLengthPropertyGetterSymbol, symbols.jsCharSequenceLength)
            add(symbols.charSequenceGetFunctionSymbol, symbols.jsCharSequenceGet)
            add(symbols.charSequenceSubSequenceFunctionSymbol, symbols.jsCharSequenceSubSequence)
            add(context.irBuiltIns.dataClassArrayMemberHashCodeSymbol, context.symbols.jsHashCode)
            add(context.irBuiltIns.dataClassArrayMemberToStringSymbol, context.symbols.jsToString)
        }
    }

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        if (doNotIntrinsify) return call
        symbolToTransformer[call.symbol]?.let {
            return it(call)
        }

        return call
    }
}

private val IrClassSymbol.sizeProperty
    get() = getPropertyGetter("size")!!

private val IrClassSymbol.getFunction
    get() = getSimpleFunction("get")!!

private val IrClassSymbol.setFunction
    get() = getSimpleFunction("set")!!

private val IrClassSymbol.iterator
    get() = getSimpleFunction("iterator")!!

private val IrClassSymbol.sizeConstructor
    get() = owner.declarations.asSequence().filterIsInstance<IrConstructor>().first { it.hasShape(regularParameters = 1) }.symbol

private val IrClassSymbol.lengthProperty
    get() = getPropertyGetter("length")!!

private val IrClassSymbol.subSequence
    get() = getSimpleFunction("subSequence")!!

private val IrClassSymbol.hashCodeFunction
    get() = getSimpleFunction("hashCode")!!
