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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.IrBodyGenerator
import org.jetbrains.kotlin.psi2ir.generators.toExpectedType
import org.jetbrains.kotlin.types.KotlinType

class PropertyLValue(
        val irBodyGenerator: IrBodyGenerator,
        val ktElement: KtElement,
        val irOperator: IrOperator?,
        val descriptor: PropertyDescriptor,
        val dispatchReceiver: IrExpression?,
        val extensionReceiver: IrExpression?,
        val isSafe: Boolean
) : IrLValue {
    override val type: KotlinType?
        get() = descriptor.type

    override fun load(): IrExpression {
        val getter = descriptor.getter!!
        return IrGetterCallImpl(
                ktElement.startOffset, ktElement.endOffset,
                getter.returnType, getter, isSafe,
                dispatchReceiver, extensionReceiver, IrOperator.GET_PROPERTY
        )
    }

    override fun store(irExpression: IrExpression): IrExpression {
        val setter = descriptor.setter!!
        val irArgument = irBodyGenerator.toExpectedType(irExpression, descriptor.type)
        val irCall = IrSetterCallImpl(ktElement.startOffset, ktElement.endOffset,
                                      setter.returnType, setter, isSafe,
                                      dispatchReceiver, extensionReceiver, irArgument, irOperator)
        return irCall
    }
}