/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import java.lang.AssertionError

abstract class IrCallWithIndexedArgumentsBase(
    startOffset: Int, endOffset: Int, type: KotlinType,
    numArguments: Int,
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    override val origin: IrStatementOrigin? = null
) : IrMemberAccessExpressionBase(startOffset, endOffset, type, typeArguments) {
    private val argumentsByParameterIndex =
        arrayOfNulls<IrExpression>(numArguments)

    override fun getValueArgument(index: Int): IrExpression? =
        argumentsByParameterIndex[index]

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= argumentsByParameterIndex.size) {
            throw AssertionError("$this: No such argument slot: $index")
        }
        argumentsByParameterIndex[index] = valueArgument
    }

    override fun removeValueArgument(index: Int) {
        argumentsByParameterIndex[index] = null
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        super.acceptChildren(visitor, data)
        argumentsByParameterIndex.forEach { it?.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        super.transformChildren(transformer, data)
        argumentsByParameterIndex.forEachIndexed { i, irExpression ->
            argumentsByParameterIndex[i] = irExpression?.transform(transformer, data)
        }
    }
}