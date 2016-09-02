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

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.expressions.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.types.KotlinType

class DelegatedLocalPropertyLValue(
        val startOffset: Int,
        val endOffset: Int,
        val descriptor: VariableDescriptorWithAccessors,
        val irOperator: IrOperator? = null
) : LValue, AssignmentReceiver {
    override val type: KotlinType get() = descriptor.type

    override fun load(): IrExpression =
            IrCallImpl(startOffset, endOffset, descriptor.type, descriptor.getter!!, irOperator)

    override fun store(irExpression: IrExpression): IrExpression =
            IrCallImpl(startOffset, endOffset, descriptor.type, descriptor.setter!!, irOperator).apply {
                putArgument(0, irExpression)
            }

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression =
            withLValue(this)
}
