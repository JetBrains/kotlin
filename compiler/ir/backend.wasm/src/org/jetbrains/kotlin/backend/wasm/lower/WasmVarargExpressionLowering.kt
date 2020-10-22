/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irComposite
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrVarargElement
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class WasmVarargExpressionLowering(
    private val context: WasmBackendContext
) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    val symbols = context.wasmSymbols

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitVararg(expression: IrVararg): IrExpression {
        val irVararg = super.visitVararg(expression) as IrVararg
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol)
        val arrayClass = irVararg.type.classOrNull!!.owner
        val primaryConstructor = arrayClass.primaryConstructor!!
        val setMethod = arrayClass.declarations.filterIsInstance<IrSimpleFunction>().find {
            it.name == Name.identifier("set")
        }!!
        return builder.irComposite(irVararg) {
            val arrayTempVariable = irTemporaryVar(
                value = irCall(primaryConstructor).apply {
                    putValueArgument(0, irInt(irVararg.elements.size))
                    if (primaryConstructor.typeParameters.isNotEmpty()) {
                        check(primaryConstructor.typeParameters.size == 1)
                        putTypeArgument(0, irVararg.varargElementType)
                    }
                },
                nameHint = "array_tmp"
            )
            for ((index: Int, element: IrVarargElement) in irVararg.elements.withIndex()) {
                check(element is IrExpression) {
                    "TODO: Support $element as vararg elements"
                }

                +irCall(setMethod).apply {
                    dispatchReceiver = irGet(arrayTempVariable)
                    putValueArgument(0, irInt(index))
                    putValueArgument(1, element)
                }
            }
            +irGet(arrayTempVariable)
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) =
        transformFunctionAccessExpression(expression)

    private fun transformFunctionAccessExpression(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid()
        val builder by lazy { context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol) }

        // Replace empty vararg arguments with empty array construction
        for (argumentIdx in 0 until expression.valueArgumentsCount) {
            val argument = expression.getValueArgument(argumentIdx)
            val parameter = expression.symbol.owner.valueParameters[argumentIdx]
            val varargElementType = parameter.varargElementType
            if (argument == null && varargElementType != null) {
                val arrayClass = parameter.type.classOrNull!!.owner
                val primaryConstructor = arrayClass.primaryConstructor!!
                val emptyArrayCall = with(builder) {
                    irCall(primaryConstructor).apply {
                        putValueArgument(0, irInt(0))
                        if (primaryConstructor.typeParameters.isNotEmpty()) {
                            check(primaryConstructor.typeParameters.size == 1)
                            putTypeArgument(0, parameter.varargElementType)
                        }
                    }
                }
                expression.putValueArgument(argumentIdx, emptyArrayCall)
            }
        }
        return expression
    }
}