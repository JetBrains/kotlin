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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.types.KotlinType

abstract class IrNoArgumentsCallableReferenceBase(
    startOffset: Int,
    endOffset: Int,
    type: KotlinType,
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    override val origin: IrStatementOrigin? = null
) : IrCallableReference,
    IrMemberAccessExpressionBase(startOffset, endOffset, type, typeArguments) {
    private fun throwNoValueArguments(): Nothing {
        throw UnsupportedOperationException("Property reference $descriptor has no value arguments")
    }

    override fun getValueArgument(index: Int): IrExpression? = throwNoValueArguments()

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) = throwNoValueArguments()

    override fun removeValueArgument(index: Int) = throwNoValueArguments()
}

