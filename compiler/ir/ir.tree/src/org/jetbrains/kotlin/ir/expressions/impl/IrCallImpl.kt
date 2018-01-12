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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createClassSymbolOrNull
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

class IrCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: KotlinType,
    override val symbol: IrFunctionSymbol,
    override val descriptor: FunctionDescriptor,
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    override val origin: IrStatementOrigin? = null,
    override val superQualifierSymbol: IrClassSymbol? = null
) : IrCall,
    IrCallWithIndexedArgumentsBase(startOffset, endOffset, type, symbol.descriptor.valueParameters.size, typeArguments) {
    @Deprecated("Creates unbound symbols")
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        calleeDescriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null,
        origin: IrStatementOrigin? = null,
        superQualifierDescriptor: ClassDescriptor? = null
    ) : this(
        startOffset, endOffset,
        type,
        createFunctionSymbol(calleeDescriptor),
        calleeDescriptor,
        typeArguments, origin,
        createClassSymbolOrNull(superQualifierDescriptor)
    )

    @Deprecated("Creates unbound symbols")
    constructor(
        startOffset: Int,
        endOffset: Int,
        calleeDescriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null,
        origin: IrStatementOrigin? = null,
        superQualifierDescriptor: ClassDescriptor? = null
    ) : this(
        startOffset, endOffset,
        calleeDescriptor.returnType!!,
        createFunctionSymbol(calleeDescriptor),
        calleeDescriptor,
        typeArguments, origin,
        createClassSymbolOrNull(superQualifierDescriptor)
    )

    constructor(
        startOffset: Int, endOffset: Int,
        symbol: IrFunctionSymbol,
        descriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null,
        origin: IrStatementOrigin? = null,
        superQualifierSymbol: IrClassSymbol? = null
    ) : this(startOffset, endOffset, descriptor.returnType!!, symbol, descriptor, typeArguments, origin, superQualifierSymbol)

    constructor(startOffset: Int, endOffset: Int, symbol: IrFunctionSymbol) :
            this(startOffset, endOffset, symbol, symbol.descriptor)


    override val superQualifier: ClassDescriptor? = superQualifierSymbol?.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)
}