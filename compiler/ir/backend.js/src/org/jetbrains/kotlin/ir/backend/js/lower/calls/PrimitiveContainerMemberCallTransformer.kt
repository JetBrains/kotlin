/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol

class PrimitiveContainerMemberCallTransformer(private val context: JsIrBackendContext) : CallsTransformer {
    private val intrinsics = context.intrinsics

    private val symbolToTransformer: SymbolToTransformer = mutableMapOf()

    init {
        symbolToTransformer.run {
            // Arrays
            add(context.intrinsics.array.sizeProperty, context.intrinsics.jsArrayLength, true)
            add(context.intrinsics.array.getFunction, context.intrinsics.jsArrayGet, true)
            add(context.intrinsics.array.setFunction, context.intrinsics.jsArraySet, true)
            add(context.intrinsics.array.iterator, context.intrinsics.jsArrayIteratorFunction.owner, true)
            for ((key, elementType) in context.intrinsics.primitiveArrays) {
                add(key.sizeProperty, context.intrinsics.jsArrayLength, true)
                add(key.getFunction, context.intrinsics.jsArrayGet, true)
                add(key.setFunction, context.intrinsics.jsArraySet, true)
                add(key.iterator, context.intrinsics.jsPrimitiveArrayIteratorFunctions[elementType]!!.owner, true)

                // TODO irCall?
                add(key.sizeConstructor) { call ->
                    IrCallImpl(
                        call.startOffset,
                        call.endOffset,
                        call.type,
                        context.intrinsics.primitiveToSizeConstructor[elementType]!!
                    ).apply {
                        putValueArgument(0, call.getValueArgument(0))
                    }
                }
            }

            add(context.irBuiltIns.stringClass.lengthProperty, context.intrinsics.jsArrayLength, true)
            add(context.irBuiltIns.stringClass.getFunction, intrinsics.jsCharSequenceGet.owner, true)
            add(context.irBuiltIns.stringClass.subSequence, intrinsics.jsCharSequenceSubSequence.owner, true)
            add(intrinsics.charSequenceLengthPropertyGetterSymbol, intrinsics.jsCharSequenceLength.owner, true)
            add(intrinsics.charSequenceGetFunctionSymbol, intrinsics.jsCharSequenceGet.owner, true)
            add(intrinsics.charSequenceSubSequenceFunctionSymbol, intrinsics.jsCharSequenceSubSequence.owner, true)
        }
    }

    override fun transformCall(call: IrCall): IrExpression {
        val symbol = call.symbol
        symbolToTransformer[symbol]?.let {
            return it(call)
        }

        return call
    }
}

private val IrClassSymbol.sizeProperty
    get() = owner.declarations.filterIsInstance<IrProperty>().first { it.name.asString() == "size" }.getter!!.symbol

private val IrClassSymbol.getFunction
    get() = owner.declarations.filterIsInstance<IrFunction>().first { it.name.asString() == "get" }.symbol

private val IrClassSymbol.setFunction
    get() = owner.declarations.filterIsInstance<IrFunction>().first { it.name.asString() == "set" }.symbol

private val IrClassSymbol.iterator
    get() = owner.declarations.filterIsInstance<IrFunction>().first { it.name.asString() == "iterator" }.symbol

private val IrClassSymbol.sizeConstructor
    get() = owner.declarations.filterIsInstance<IrConstructor>().first { it.valueParameters.size == 1 }.symbol

private val IrClassSymbol.lengthProperty
    get() = owner.declarations.filterIsInstance<IrProperty>().first { it.name.asString() == "length" }.getter!!.symbol

private val IrClassSymbol.subSequence
    get() = owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "subSequence" }.symbol
