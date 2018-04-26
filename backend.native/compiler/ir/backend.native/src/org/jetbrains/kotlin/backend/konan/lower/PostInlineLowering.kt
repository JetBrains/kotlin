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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWith
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWithStarProjections
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWithoutArguments
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This pass runs after inlining and performs the following additional transformations over some operations:
 *     - Convert immutableBinaryBlobOf() arguments to special IrConst.
 *     - Convert `obj::class` and `Class::class` to calls.
 */
internal class PostInlineLowering(val context: Context) : FileLoweringPass {

    private val symbols get() = context.ir.symbols

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrBuildingTransformer(context) {

            override fun visitClassReference(expression: IrClassReference): IrExpression {
                expression.transformChildrenVoid()
                builder.at(expression)

                val typeArgument = expression.symbol.typeWithStarProjections

                return builder.irCall(symbols.kClassImplConstructor, listOf(typeArgument)).apply {
                    putValueArgument(0, builder.irCall(symbols.getClassTypeInfo, listOf(typeArgument)))
                }
            }

            override fun visitGetClass(expression: IrGetClass): IrExpression {
                expression.transformChildrenVoid()
                builder.at(expression)

                val typeArgument = expression.argument.type
                return builder.irCall(symbols.kClassImplConstructor, listOf(typeArgument)).apply {
                    val typeInfo = builder.irCall(symbols.getObjectTypeInfo).apply {
                        putValueArgument(0, expression.argument)
                    }

                    putValueArgument(0, typeInfo)
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.symbol == context.ir.symbols.immutableBinaryBlobOf) {
                    // Convert arguments of the binary blob to special IrConst<String> structure, so that
                    // vararg lowering will not affect it.
                    val args = expression.getValueArgument(0) as? IrVararg
                    if (args == null) throw Error("varargs shall not be lowered yet")
                    if (args.elements.any { it is IrSpreadElement }) {
                        context.reportCompilationError("no spread elements allowed here", irFile, args)
                    }
                    val builder = StringBuilder()
                    args.elements.forEach {
                        if (it !is IrConst<*>) {
                            context.reportCompilationError(
                                    "all elements of binary blob must be constants", irFile, it)
                        }
                        val value = when (it.kind) {
                            IrConstKind.Short ->  (it.value as Short).toInt()
                            else ->
                                context.reportCompilationError("incorrect value for binary data: $it.value", irFile, it)
                        }
                        if (value < 0 || value > 0xff)
                            context.reportCompilationError("incorrect value for binary data: $value", irFile, it)
                        // Luckily, all values in range 0x00 .. 0xff represent valid UTF-16 symbols,
                        // block 0 (Basic Latin) and block 1 (Latin-1 Supplement) in
                        // Basic Multilingual Plane, so we could just append data "as is".
                        builder.append(value.toChar())
                    }
                    expression.putValueArgument(0, IrConstImpl<String>(
                            expression.startOffset, expression.endOffset,
                            context.ir.symbols.immutableBinaryBlob.typeWithoutArguments,
                            IrConstKind.String, builder.toString()))
                }

                return expression
            }
        })
    }
}