/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.asSimpleLambda
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

class ArrayConstructorLowering(val context: CommonBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    // Array(size, init) -> Array(size)
    private fun arrayInlineToSizeConstructor(irConstructor: IrConstructor): IrFunctionSymbol? {
        val clazz = irConstructor.constructedClass.symbol
        return when {
            irConstructor.valueParameters.size != 2 -> null
            clazz == context.irBuiltIns.arrayClass -> context.ir.symbols.arrayOfNulls // Array<T> has no unary constructor: it can only exist for Array<T?>
            context.irBuiltIns.primitiveArrays.contains(clazz) -> clazz.constructors.single { it.owner.valueParameters.size == 1 }
            else -> null
        }
    }

    private fun IrExpression.asSingleArgumentLambda(): IrSimpleFunction? {
        val function = asSimpleLambda() ?: return null
        // Only match the one that has exactly one non-vararg argument, as the code below
        // does not handle defaults or varargs.
        if (function.valueParameters.size != 1)
            return null
        return function
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val sizeConstructor = arrayInlineToSizeConstructor(expression.symbol.owner)
            ?: return super.visitConstructorCall(expression)
        // inline fun <reified T> Array(size: Int, invokable: (Int) -> T): Array<T> {
        //     val result = arrayOfNulls<T>(size)
        //     for (i in 0 until size) {
        //         result[i] = invokable(i)
        //     }
        //     return result as Array<T>
        // }
        // (and similar for primitive arrays)
        val size = expression.getValueArgument(0)!!.transform(this, null)
        val invokable = expression.getValueArgument(1)!!.transform(this, null)
        val scope = currentScope!!.scope
        return context.createIrBuilder(scope.scopeOwnerSymbol).irBlock(expression.startOffset, expression.endOffset) {
            val index = irTemporaryVar(irInt(0))
            val sizeVar = irTemporary(size)
            val result = irTemporary(irCall(sizeConstructor, expression.type).apply {
                copyTypeArgumentsFrom(expression)
                putValueArgument(0, irGet(sizeVar))
            })

            val lambda = invokable.asSingleArgumentLambda()
            val invoke = invokable.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INVOKE }
            val invokableVar = if (lambda == null) irTemporary(invokable) else null
            +irWhile().apply {
                condition = irCall(context.irBuiltIns.lessFunByOperandType[index.type.classifierOrFail]!!).apply {
                    putValueArgument(0, irGet(index))
                    putValueArgument(1, irGet(sizeVar))
                }
                body = irBlock {
                    val tempIndex = irTemporary(irGet(index))
                    val value = lambda?.inline(listOf(tempIndex)) ?: irCallOp(
                        invoke.symbol,
                        invoke.returnType,
                        irGet(invokableVar!!),
                        irGet(tempIndex)
                    )
                    +irCall(result.type.getClass()!!.functions.single { it.name == OperatorNameConventions.SET }).apply {
                        dispatchReceiver = irGet(result)
                        putValueArgument(0, irGet(tempIndex))
                        putValueArgument(1, value)
                    }
                    val inc = index.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INC }
                    +irSetVar(index.symbol, irCallOp(inc.symbol, index.type, irGet(index)))
                }
            }
            +irGet(result)
        }.also {
            // Some parents of local declarations are not updated during ad-hoc inlining
            // TODO: Remove when generic inliner is used
            it.patchDeclarationParents(scope.getLocalDeclarationParent())
        }
    }
}
