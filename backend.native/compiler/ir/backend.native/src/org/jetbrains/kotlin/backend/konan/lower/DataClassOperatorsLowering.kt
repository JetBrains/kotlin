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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class DataClassOperatorsLowering(val context: Context): FunctionLoweringPass {

    private val irBuiltins = context.irModule!!.irBuiltins

    override fun lower(irFunction: IrFunction) {
        irFunction.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val descriptor = expression.descriptor.original
                if (descriptor != irBuiltins.dataClassArrayMemberToString
                        && descriptor != irBuiltins.dataClassArrayMemberHashCode)
                    return expression

                val argument = expression.getValueArgument(0)!!
                val argumentClassifier = argument.type.classifierOrFail

                val isToString = expression.symbol == irBuiltins.dataClassArrayMemberToStringSymbol
                val newCalleeSymbol = if (isToString)
                                    context.ir.symbols.arrayContentToString[argumentClassifier]!!
                                else
                                    context.ir.symbols.arrayContentHashCode[argumentClassifier]!!

                val newCallee = newCalleeSymbol.owner

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(irFunction.symbol, startOffset, endOffset)

                return irBuilder.run {
                    // TODO: use more precise type arguments.
                    val typeArguments = (0 until newCallee.typeParameters.size).map { irBuiltins.anyNType }

                    if (!argument.type.isSimpleTypeWithQuestionMark) {
                        irCall(newCallee, typeArguments).apply {
                            extensionReceiver = argument
                        }
                    } else {
                        val tmp = scope.createTemporaryVariable(argument)
                        val call = irCall(newCallee, typeArguments).apply {
                            extensionReceiver = irGet(tmp)
                        }
                        irBlock(argument) {
                            +tmp
                            +irIfThenElse(call.type,
                                    irEqeqeq(irGet(tmp), irNull()),
                                    if (isToString)
                                        irString("null")
                                    else
                                        irInt(0),
                                    call)
                        }
                    }
                }
            }
        })
    }
}