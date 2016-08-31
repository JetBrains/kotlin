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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor


interface IrEnumConstructorCall : IrGeneralCall {
    override val descriptor: ConstructorDescriptor
    val enumEntryDescriptor: ClassDescriptor?
}

val IrEnumConstructorCall.isSuper: Boolean
    get() = enumEntryDescriptor == null

class IrEnumConstructorCallImpl(
        startOffset: Int,
        endOffset: Int,
        override val descriptor: ConstructorDescriptor,
        override val enumEntryDescriptor: ClassDescriptor? = null
) : IrGeneralCallBase(startOffset, endOffset, descriptor.returnType, descriptor.valueParameters.size), IrEnumConstructorCall {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitEnumConstructorCall(this, data)
    }
}