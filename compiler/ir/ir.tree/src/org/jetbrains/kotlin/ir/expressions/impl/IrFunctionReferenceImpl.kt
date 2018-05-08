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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.typeArgumentsCount
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

class IrFunctionReferenceImpl(
    startOffset: Int,
    endOffset: Int,
    type: KotlinType,
    override val symbol: IrFunctionSymbol,
    override val descriptor: FunctionDescriptor,
    typeArgumentsCount: Int,
    origin: IrStatementOrigin? = null
) :
    IrCallWithIndexedArgumentsBase(
        startOffset,
        endOffset,
        type,
        typeArgumentsCount,
        symbol.descriptor.valueParameters.size,
        origin
    ),
    IrFunctionReference {

    constructor(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        symbol: IrFunctionSymbol,
        descriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null,
        origin: IrStatementOrigin? = null
    ) : this(startOffset, endOffset, type, symbol, descriptor, descriptor.typeArgumentsCount, origin) {
        copyTypeArgumentsFrom(typeArguments)
    }

    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        descriptor: FunctionDescriptor,
        typeArgumentsCount: Int,
        origin: IrStatementOrigin? = null
    ) : this(startOffset, endOffset, type, createFunctionSymbol(descriptor.original), descriptor, typeArgumentsCount, origin)

    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        descriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null,
        origin: IrStatementOrigin? = null
    ) : this(
        startOffset, endOffset, type, createFunctionSymbol(descriptor.original), descriptor, descriptor.typeArgumentsCount, origin
    ) {
        copyTypeArgumentsFrom(typeArguments)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitFunctionReference(this, data)
}