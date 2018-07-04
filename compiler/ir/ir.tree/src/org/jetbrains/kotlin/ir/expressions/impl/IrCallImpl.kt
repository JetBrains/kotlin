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
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createClassSymbolOrNull
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    override val symbol: IrFunctionSymbol,
    override val descriptor: FunctionDescriptor,
    typeArgumentsCount: Int,
    origin: IrStatementOrigin? = null,
    override val superQualifierSymbol: IrClassSymbol? = null
) :
    IrCallWithIndexedArgumentsBase(
        startOffset, endOffset, type,
        typeArgumentsCount,
        symbol.descriptor.valueParameters.size,
        origin
    ),
    IrCall {

    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        symbol: IrFunctionSymbol,
        descriptor: FunctionDescriptor,
        origin: IrStatementOrigin? = null,
        superQualifierSymbol: IrClassSymbol? = null
    ) : this(startOffset, endOffset, type, symbol, descriptor, descriptor.typeParametersCount, origin, superQualifierSymbol)

    @Deprecated("Creates unbound symbols")
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        descriptor: FunctionDescriptor,
        typeArgumentsCount: Int,
        origin: IrStatementOrigin? = null,
        superQualifierDescriptor: ClassDescriptor? = null
    ) : this(
        startOffset, endOffset,
        type,
        createFunctionSymbol(descriptor),
        descriptor,
        typeArgumentsCount,
        origin,
        createClassSymbolOrNull(superQualifierDescriptor)
    )

    constructor(startOffset: Int, endOffset: Int, type: IrType, symbol: IrFunctionSymbol) :
            this(startOffset, endOffset, type, symbol, symbol.descriptor)


    override val superQualifier: ClassDescriptor? = superQualifierSymbol?.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)
}