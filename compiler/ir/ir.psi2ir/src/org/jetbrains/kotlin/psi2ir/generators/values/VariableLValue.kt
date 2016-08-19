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

package org.jetbrains.kotlin.psi2ir.generators.values

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetVariableExpressionImpl
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.expressions.IrSetVariableExpressionImpl
import org.jetbrains.kotlin.psi2ir.toExpectedType
import org.jetbrains.kotlin.types.KotlinType

class VariableLValue(
        val startOffset: Int,
        val endOffset: Int,
        val descriptor: VariableDescriptor,
        val irOperator: IrOperator? = null
) : IrLValue {
    constructor(
            irVariable: IrVariable,
            irOperator: IrOperator? = null
    ) : this(irVariable.startOffset, irVariable.endOffset, irVariable.descriptor, irOperator)

    override val type: KotlinType? get() = descriptor.type

    override fun load(): IrExpression =
            IrGetVariableExpressionImpl(startOffset, endOffset, descriptor, irOperator)

    override fun store(irExpression: IrExpression): IrExpression =
            IrSetVariableExpressionImpl(startOffset, endOffset, descriptor,
                                        irExpression.toExpectedType(descriptor.type), irOperator)
}