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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

abstract class IrCallableReferenceBase(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        numValueArguments: Int,
        override val origin: IrStatementOrigin? = null
) : IrCallableReference,
        IrCallWithIndexedArgumentsBase(
                startOffset, endOffset,
                type,
                numValueArguments,
                typeArguments,
                origin
        )

class IrFunctionReferenceImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val symbol: IrFunctionSymbol,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin? = null
) : IrFunctionReference,
        IrCallableReferenceBase(
                startOffset, endOffset, type, typeArguments,
                symbol.descriptor.valueParameters.size,
                origin
        )
{
    @Deprecated("Creates unbound symbol")
    constructor(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType,
            descriptor: FunctionDescriptor,
            typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
            origin: IrStatementOrigin? = null
    ) : this(startOffset, endOffset, type, createFunctionSymbol(descriptor), typeArguments, origin)

    override val descriptor: FunctionDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitFunctionReference(this, data)
}

class IrPropertyReferenceImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val descriptor: PropertyDescriptor,
        override val field: IrFieldSymbol?,
        override val getter: IrFunctionSymbol?,
        override val setter: IrFunctionSymbol?,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        override val origin: IrStatementOrigin? = null
) : IrPropertyReference,
        IrMemberAccessExpressionBase(startOffset, endOffset, type, typeArguments)
{
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitPropertyReference(this, data)

    private fun throwNoValueArguments(): Nothing {
        throw UnsupportedOperationException("Property reference $descriptor has no value arguments")
    }

    override fun getValueArgument(index: Int): IrExpression? = throwNoValueArguments()

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) = throwNoValueArguments()

    override fun removeValueArgument(index: Int) = throwNoValueArguments()
}