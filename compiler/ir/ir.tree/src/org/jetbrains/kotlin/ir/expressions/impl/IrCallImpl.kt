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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    override val target: IrFunction,
    override val descriptor: FunctionDescriptor,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
    origin: IrStatementOrigin? = null,
    override val irSuperQualifier: IrClass? = null
) :
    IrCallWithIndexedArgumentsBase(
        startOffset, endOffset, type,
        typeArgumentsCount,
        valueArgumentsCount,
        origin
    ),
    IrCall {

    init {
        if (descriptor is ConstructorDescriptor) {
            throw AssertionError("Should be IrConstructorCall: $descriptor")
        }
    }

    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        target: IrFunction,
        descriptor: FunctionDescriptor,
        origin: IrStatementOrigin? = null,
        irSuperQualifier: IrClass? = null
    ) : this(
        startOffset, endOffset, type, target, descriptor, descriptor.typeParametersCount,
        descriptor.valueParameters.size, origin, irSuperQualifier
    )

    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        target: IrFunction,
        descriptor: FunctionDescriptor,
        typeArgumentsCount: Int,
        origin: IrStatementOrigin? = null,
        irSuperQualifier: IrClass? = null
    ) : this(
        startOffset, endOffset, type, target, descriptor, typeArgumentsCount,
        descriptor.valueParameters.size, origin, irSuperQualifier
    )

    constructor(startOffset: Int, endOffset: Int, type: IrType, target: IrFunction) :
            this(startOffset, endOffset, type, target, target.descriptor)


    override val superQualifier: ClassDescriptor? = irSuperQualifier?.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)
}
