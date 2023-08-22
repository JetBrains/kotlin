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

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.types.isStringClassType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * This lowering pass replaces [IrStringConcatenation]s with StringBuilder appends.
 */
class StringConcatenationLowering(context: CommonBackendContext) : FileLoweringPass, IrBuildingTransformer(context) {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private val irBuiltIns = context.irBuiltIns
    private val symbols = context.ir.symbols

    private val typesWithSpecialAppendFunction = irBuiltIns.primitiveIrTypes + irBuiltIns.stringType

    private val nameAppend = Name.identifier("append")

    private val stringBuilder = context.ir.symbols.stringBuilder.owner

    //TODO: calculate and pass string length to the constructor.
    private val constructor = stringBuilder.constructors.single {
        it.valueParameters.isEmpty()
    }

    private val defaultAppendFunction = stringBuilder.functions.single {
        it.name == nameAppend &&
                it.valueParameters.size == 1 &&
                it.valueParameters.single().type.isNullableAny()
    }

    private val appendFunctions: Map<IrType, IrSimpleFunction?> =
        typesWithSpecialAppendFunction.map { type ->
            type to stringBuilder.functions.toList().atMostOne {
                it.name == nameAppend && it.valueParameters.singleOrNull()?.type == type
            }
        }.toMap()

    private fun typeToAppendFunction(type: IrType): IrSimpleFunction {
        return appendFunctions[type] ?: defaultAppendFunction
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression.transformChildrenVoid(this)

        builder.at(expression)
        val arguments = expression.arguments
        return when {
            arguments.isEmpty() -> builder.irString("")

            arguments.size == 1 -> {
                val argument = arguments[0]
                if (argument.type.isNullable())
                    builder.irCall(symbols.extensionToString).apply {
                        extensionReceiver = argument
                    }
                else builder.irCall(symbols.memberToString).apply {
                    dispatchReceiver = argument
                }
            }

            arguments.size == 2 && arguments[0].type.isStringClassType() ->
                if (arguments[0].type.isNullable())
                    builder.irCall(symbols.extensionStringPlus).apply {
                        extensionReceiver = arguments[0]
                        putValueArgument(0, arguments[1])
                    }
                else
                    builder.irCall(symbols.memberStringPlus).apply {
                        dispatchReceiver = arguments[0]
                        putValueArgument(0, arguments[1])
                    }

            else -> builder.irBlock(expression) {
                val stringBuilderImpl = createTmpVariable(irCall(constructor))
                expression.arguments.forEach { arg ->
                    val appendFunction = typeToAppendFunction(arg.type)
                    +irCall(appendFunction).apply {
                        dispatchReceiver = irGet(stringBuilderImpl)
                        putValueArgument(0, arg)
                    }
                }
                +irCall(symbols.memberToString).apply {
                    dispatchReceiver = irGet(stringBuilderImpl)
                }
            }
        }
    }
}
