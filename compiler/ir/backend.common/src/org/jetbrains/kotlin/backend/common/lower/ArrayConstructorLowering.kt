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
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
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
                condition = irCall(context.irBuiltIns.lessFunByOperandType[index.type.toKotlinType()]!!).apply {
                    putValueArgument(0, irGet(index))
                    putValueArgument(1, irGet(sizeVar))
                }
                body = irBlock {
                    val value =
                        lambda?.inline(listOf(index)) ?: irCallOp(invoke.symbol, invoke.returnType, irGet(invokableVar!!), irGet(index))
                    +irCall(result.type.getClass()!!.functions.single { it.name == OperatorNameConventions.SET }).apply {
                        dispatchReceiver = irGet(result)
                        putValueArgument(0, irGet(index))
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

    // TODO use a generic inliner (e.g. JS/Native's FunctionInlining.Inliner)
    private fun IrFunction.inline(arguments: List<IrValueDeclaration>): IrReturnableBlock {
        val argumentMap = valueParameters.zip(arguments).toMap()
        val blockSymbol = IrReturnableBlockSymbolImpl(descriptor)
        val block = IrReturnableBlockImpl(startOffset, endOffset, returnType, blockSymbol, null, symbol)
        val remapper = object : AbstractVariableRemapper() {
            override fun remapVariable(value: IrValueDeclaration): IrValueDeclaration? =
                argumentMap[value]

            override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
                if (expression.returnTargetSymbol == symbol)
                    IrReturnImpl(expression.startOffset, expression.endOffset, expression.type, blockSymbol, expression.value)
                else
                    expression
            )
        }
        when (val transformed = body?.transform(remapper, null)) {
            is IrBlockBody -> block.statements += transformed.statements
            is IrExpressionBody -> block.statements += transformed.expression
            else -> throw AssertionError("unexpected body type: $this")
        }
        return block
    }
}
