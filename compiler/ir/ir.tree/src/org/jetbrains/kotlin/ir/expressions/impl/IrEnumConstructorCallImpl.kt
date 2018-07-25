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
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrEnumConstructorCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    override val symbol: IrConstructorSymbol,
    typeArgumentsCount: Int
) :
    IrCallWithIndexedArgumentsBase(
        startOffset,
        endOffset,
        type,
        typeArgumentsCount = typeArgumentsCount,
        valueArgumentsCount = symbol.descriptor.valueParameters.size
    ),
    IrEnumConstructorCall {

    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        symbol: IrConstructorSymbol
    ) : this(startOffset, endOffset, type, symbol, symbol.descriptor.typeParametersCount)

    @Deprecated("Creates unbound symbols")
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        descriptor: ClassConstructorDescriptor,
        typeArgumentsCount: Int
    ) : this(startOffset, endOffset, type, IrConstructorSymbolImpl(descriptor), typeArgumentsCount)

    override val descriptor: ClassConstructorDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitEnumConstructorCall(this, data)
    }
}