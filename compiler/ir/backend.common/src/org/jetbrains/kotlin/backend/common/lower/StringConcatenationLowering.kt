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
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.type
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType


/**
 * This lowering pass replaces [IrStringConcatenation]s with StringBuilder appends.
 */
internal class StringConcatenationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(StringConcatenationTransformer(this))
    }
}

private class StringConcatenationTransformer(val lower: StringConcatenationLowering) : IrElementTransformerVoid() {

    private val buildersStack = mutableListOf<IrBuilderWithScope>()
    private val context = lower.context
    private val builtIns = context.builtIns

    private val typesWithSpecialAppendFunction =
            PrimitiveType.values().map { builtIns.getPrimitiveKotlinType(it) } + builtIns.stringType

    private val nameToString = Name.identifier("toString")
    private val nameAppend = Name.identifier("append")

    private val stringBuilder = context.ir.symbols.stringBuilder

    //TODO: calculate and pass string length to the constructor.
    private val constructor = stringBuilder.constructors.single {
        it.owner.valueParameters.size == 0
    }

    private val toStringFunction = stringBuilder.functions.single {
        it.owner.valueParameters.size == 0 && it.descriptor.name == nameToString
    }
    private val defaultAppendFunction = stringBuilder.functions.single {
        it.descriptor.name == nameAppend &&
                it.owner.valueParameters.size == 1 &&
                it.owner.valueParameters.single().type == builtIns.nullableAnyType
    }


    private val appendFunctions: Map<KotlinType, IrFunctionSymbol?> =
            typesWithSpecialAppendFunction.map { type ->
                type to stringBuilder.functions.toList().atMostOne {
                    it.descriptor.name == nameAppend &&
                            it.owner.valueParameters.size == 1 &&
                            it.owner.valueParameters.single().type == type
                }
            }.toMap()

    private fun typeToAppendFunction(type : KotlinType) : IrFunctionSymbol {
        return appendFunctions[type]?:defaultAppendFunction
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        assert(!buildersStack.isEmpty())

        expression.transformChildrenVoid(this)
        val blockBuilder = buildersStack.last()
        return blockBuilder.irBlock(expression) {
            val stringBuilderImpl = irTemporary(irCall(constructor)).symbol
            expression.arguments.forEach { arg ->
                val appendFunction = typeToAppendFunction(arg.type)
                +irCall(appendFunction).apply {
                    dispatchReceiver = irGet(stringBuilderImpl)
                    putValueArgument(0, arg)
                }
            }
            +irCall(toStringFunction).apply {
                dispatchReceiver = irGet(stringBuilderImpl)
            }
        }
    }

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
        if (declaration !is IrSymbolDeclaration<*>) {
            return super.visitDeclaration(declaration)
        }

        with(declaration) {
            buildersStack.add(
                    context.createIrBuilder(declaration.symbol, startOffset, endOffset)
            )
            transformChildrenVoid(this@StringConcatenationTransformer)
            buildersStack.removeAt(buildersStack.lastIndex)
            return this@with
        }
    }
}