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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInitializePropertyImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns

class PropertyInitializerLValue(
        val startOffset: Int,
        val endOffset: Int,
        val propertyDescriptor: PropertyDescriptor
) : LValue, AssignmentReceiver {
    override val type: KotlinType get() = propertyDescriptor.type

    override fun store(irExpression: IrExpression): IrExpression {
        val irInitProperty = IrInitializePropertyImpl(startOffset, endOffset, type.builtIns.unitType, propertyDescriptor)
        irInitProperty.initBlockExpression = irExpression
        return irInitProperty
    }

    override fun load(): IrExpression {
        throw AssertionError("Property initializer LValue for $propertyDescriptor should not be used in compound assignment.")
    }

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression {
        return withLValue(this)
    }
}
