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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.types.KotlinType

class BackingFieldLValue(
        val startOffset: Int,
        val endOffset: Int,
        val descriptor: PropertyDescriptor,
        val receiver: IntermediateValue?,
        val operator: IrOperator?
) : LValue, AssignmentReceiver {
    override val type: KotlinType get() = descriptor.type

    override fun store(irExpression: IrExpression): IrExpression =
            IrSetBackingFieldImpl(startOffset, endOffset, descriptor, receiver?.load(), irExpression, operator)

    override fun load(): IrExpression =
            IrGetBackingFieldImpl(startOffset, endOffset, descriptor, receiver?.load(), operator)

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression =
            withLValue(this)
}
