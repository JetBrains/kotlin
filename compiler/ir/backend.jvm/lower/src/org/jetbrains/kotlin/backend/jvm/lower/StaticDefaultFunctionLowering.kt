/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.util.createStaticFunctionWithReceivers
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal val staticDefaultFunctionPhase = makeIrFilePhase(
    ::StaticDefaultFunctionLowering,
    name = "StaticDefaultFunction",
    description = "Make function adapters for default arguments static",
    prerequisite = setOf(jvmStaticInObjectPhase),
)

private class StaticDefaultFunctionLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement = super.visitFunction(
        if (declaration.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER && declaration.dispatchReceiverParameter != null)
            getStaticFunctionWithReceivers(declaration).also {
                it.body = declaration.moveBodyTo(it)
            }
        else
            declaration
    )

    override fun visitReturn(expression: IrReturn): IrExpression {
        val irFunction = context.staticDefaultStubs[expression.returnTargetSymbol]
        return super.visitReturn(
            if (irFunction != null) {
                with(expression) {
                    IrReturnImpl(startOffset, endOffset, type, irFunction.symbol, value)
                }
            } else {
                expression
            }
        )
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        if (callee.origin !== IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER || expression.dispatchReceiver == null) {
            return super.visitCall(expression)
        }

        val newCallee = getStaticFunctionWithReceivers(callee)
        val newCall = irCall(expression, newCallee, receiversAsArguments = true)

        return super.visitCall(newCall)
    }

    private fun getStaticFunctionWithReceivers(function: IrSimpleFunction): IrSimpleFunction =
        context.staticDefaultStubs.getOrPut(function.symbol) {
            context.irFactory.createStaticFunctionWithReceivers(
                function.parent,
                function.name,
                function,
                remapMultiFieldValueClassStructure = context::remapMultiFieldValueClassStructure
            )
        }
}
