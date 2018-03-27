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

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.typeArgumentsCount
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType

class IrDelegatingConstructorCallImpl(
    startOffset: Int,
    endOffset: Int,
    override val symbol: IrConstructorSymbol,
    override val descriptor: ClassConstructorDescriptor,
    typeArgumentsCount: Int
) :
    IrCallWithIndexedArgumentsBase(
        startOffset,
        endOffset,
        symbol.descriptor.builtIns.unitType,
        typeArgumentsCount = typeArgumentsCount,
        valueArgumentsCount = symbol.descriptor.valueParameters.size
    ),
    IrDelegatingConstructorCall {

    @Deprecated("Use constructor with typeArgumentsCount and fill type arguments explicitly or with copyTypeArgumentsFrom")
    constructor(
        startOffset: Int,
        endOffset: Int,
        symbol: IrConstructorSymbol,
        descriptor: ClassConstructorDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null
    ) : this(startOffset, endOffset, symbol, descriptor, descriptor.typeArgumentsCount) {
        copyTypeArgumentsFrom(typeArguments)
    }

    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int,
        endOffset: Int,
        descriptor: ClassConstructorDescriptor,
        typeArgumentsCount: Int
    ) : this(
        startOffset, endOffset,
        IrConstructorSymbolImpl(descriptor.original),
        descriptor,
        typeArgumentsCount
    )

    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int,
        endOffset: Int,
        descriptor: ClassConstructorDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null
    ) : this(
        startOffset, endOffset,
        IrConstructorSymbolImpl(descriptor.original),
        descriptor,
        typeArguments
    )

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitDelegatingConstructorCall(this, data)
    }
}