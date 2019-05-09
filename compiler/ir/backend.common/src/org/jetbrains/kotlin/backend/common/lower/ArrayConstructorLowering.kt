/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

class ArrayConstructorLowering(val context: CommonBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    // Array(size, init) -> Array(size)
    private val arrayInlineToSizeCtor: Map<IrFunctionSymbol, IrFunctionSymbol> =
        (context.irBuiltIns.primitiveArrays + context.irBuiltIns.arrayClass).associate { arrayClass ->
            val fromInit = arrayClass.constructors.single { it.owner.valueParameters.size == 2 }
            val fromSize = arrayClass.constructors.find { it.owner.valueParameters.size == 1 }
                ?: context.ir.symbols.arrayOfNulls // Array<T> has no unary constructor: it can only exist for Array<T?>
            fromInit to fromSize
        }

    // Generate `array[index] = value`.
    private fun IrBuilderWithScope.setItem(array: IrVariable, index: IrVariable, value: IrExpression) =
        irCall(array.type.getClass()!!.functions.single { it.name.toString() == "set" }).apply {
            dispatchReceiver = irGet(array)
            putValueArgument(0, irGet(index))
            putValueArgument(1, value)
        }

    // Generate `for (index in 0 until end) { element }`.
    private fun IrBlockBuilder.fromZeroTo(end: IrVariable, element: IrBlockBuilder.(IrLoop, IrVariable) -> Unit) {
        val index = irTemporaryVar(irInt(0))
        +irWhile().apply {
            condition = irCall(context.irBuiltIns.lessFunByOperandType[index.type.toKotlinType()]!!).apply {
                putValueArgument(0, irGet(index))
                putValueArgument(1, irGet(end))
            }
            body = irBlock {
                val currentIndex = irTemporary(irGet(index))
                val inc = index.type.getClass()!!.functions.single { it.name.asString() == "inc" }
                +irSetVar(index.symbol, irCall(inc).apply { dispatchReceiver = irGet(index) })
                element(this@apply, currentIndex)
            }
        }
    }

    private fun IrExpression.asSingleArgumentLambda(): IrSimpleFunction? {
        // A lambda is represented as a block with a function declaration and a reference to it.
        if (this !is IrBlock || statements.size != 2)
            return null
        val (function, reference) = statements
        if (function !is IrSimpleFunction || reference !is IrFunctionReference || function.symbol != reference.symbol)
            return null
        // Only match the one that has exactly one non-vararg argument, as the code below
        // does not handle defaults or varargs.
        if (function.valueParameters.size != 1 || function.valueParameters[0].isVararg || reference.getValueArgument(0) != null)
            return null
        return function
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val sizeConstructor = arrayInlineToSizeCtor[expression.symbol]
            ?: return super.visitConstructorCall(expression)
        val arrayOfReferences = sizeConstructor == context.ir.symbols.arrayOfNulls

        // inline fun <reified T> Array(size: Int, invokable: (Int) -> T): Array<T> {
        //     val result = arrayOfNulls<T>(size)
        //     for (i in 0 until size) {
        //         result[i] = invokable(i)
        //     }
        //     return result as Array<T>
        // }
        // (and similar for primitive arrays)
        val loweringContext = context
        val size = expression.getValueArgument(0)!!.transform(this, null)
        val invokable = expression.getValueArgument(1)!!.transform(this, null)
        return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).irBlock(expression.startOffset, expression.endOffset) {
            val sizeVar = irTemporary(size)
            val result = irTemporary(irCall(sizeConstructor, expression.type).apply {
                if (arrayOfReferences) {
                    putTypeArgument(0, expression.getTypeArgument(0))
                }
                putValueArgument(0, irGet(sizeVar))
            })

            val lambda = invokable.asSingleArgumentLambda()
            if (lambda == null) {
                val invokableVar = irTemporary(invokable)
                val invoke = invokable.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INVOKE }
                fromZeroTo(sizeVar) { _, index ->
                    +setItem(result, index, irCall(invoke).apply {
                        dispatchReceiver = irGet(invokableVar)
                        putValueArgument(0, irGet(index))
                    })
                }
            } else {
                // Inline `invokable` by replacing the argument with `i` and `return x` with `result[i] = x; continue`.
                fromZeroTo(sizeVar) { loop, index ->
                    val body = lambda.body!!.transform(object : IrElementTransformerVoidWithContext() {
                        override fun visitGetValue(expression: IrGetValue) =
                            if (expression.symbol == lambda.valueParameters[0].symbol)
                                IrGetValueImpl(expression.startOffset, expression.endOffset, index.symbol)
                            else
                                super.visitGetValue(expression)

                        override fun visitReturn(expression: IrReturn) =
                            if (expression.returnTargetSymbol == lambda.symbol) {
                                val value = expression.value.transform(this, null)
                                val scope = currentScope?.scope?.scopeOwnerSymbol ?: lambda.symbol
                                loweringContext.createIrBuilder(scope).irBlock(expression.startOffset, expression.endOffset) {
                                    +setItem(result, index, value)
                                    +irContinue(loop)
                                }
                            } else {
                                super.visitReturn(expression)
                            }
                    }, null)

                    when (body) {
                        is IrExpressionBody -> +setItem(result, index, body.expression)
                        is IrBlockBody -> body.statements.forEach { +it }
                        else -> throw AssertionError("unexpected function body type: $body")
                    }
                }
            }
            +irGet(result)
        }
    }
}
