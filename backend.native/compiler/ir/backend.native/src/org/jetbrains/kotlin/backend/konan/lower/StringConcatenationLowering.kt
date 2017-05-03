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
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.signature2Descriptor
import org.jetbrains.kotlin.ir.builders.irLetSequence
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolDeclaration
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType


/**
 * This lowering pass replaces [IrStringConcatenation]s with StringBuilder appends.
 */
internal class StringConcatenationLowering(val context: Context) : FileLoweringPass {
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

    private val kotlinTextFqName = FqName.fromSegments(listOf("kotlin", "text"))
    private val nameToString = Name.identifier("toString")
    private val nameStringBuilder = Name.identifier("StringBuilder")
    private val nameAppend = Name.identifier("append")

    private val classStringBuilder = builtIns.builtInsModule.getPackage(kotlinTextFqName).
            memberScope.getContributedClassifier(nameStringBuilder,
            NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    //TODO: calculate and pass string length to the constructor.
    private val constructor = classStringBuilder.constructors.firstOrNull {
        it.valueParameters.size == 0
    }!!

    private val toStringFunction = classStringBuilder.signature2Descriptor(nameToString)!!
    private val defaultAppendFunction =
            classStringBuilder.signature2Descriptor(nameAppend, arrayOf(builtIns.nullableAnyType))!!

    private val appendFunctions: Map<KotlinType, FunctionDescriptor?> =
            typesWithSpecialAppendFunction.map {
                it to classStringBuilder.signature2Descriptor(nameAppend, arrayOf(it))
            }.toMap()

    private fun typeToAppendFunction(type : KotlinType) : FunctionDescriptor {
        return appendFunctions[type]?:defaultAppendFunction
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        assert(!buildersStack.isEmpty())

        expression.transformChildrenVoid(this)
        val blockBuilder = buildersStack.last()
        return blockBuilder.irLetSequence(
                value = blockBuilder.irCall(constructor),
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                resultType = expression.type) { stringBuilderImpl ->
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