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

import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.allTypeParameters
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrDelegatingConstructorCallImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override val symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int
) : IrDelegatingConstructorCall(typeArgumentsCount, valueArgumentsCount) {
    override val origin: IrStatementOrigin?
        get() = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitDelegatingConstructorCall(this, data)
    }

    companion object {
        @ObsoleteDescriptorBasedAPI
        fun fromSymbolDescriptor(
            startOffset: Int,
            endOffset: Int,
            type: IrType,
            symbol: IrConstructorSymbol,
            typeArgumentsCount: Int = symbol.descriptor.typeParametersCount,
            valueArgumentsCount: Int = symbol.descriptor.valueParameters.size + symbol.descriptor.contextReceiverParameters.size
        ) = IrDelegatingConstructorCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount)

        fun fromSymbolOwner(
            startOffset: Int,
            endOffset: Int,
            type: IrType,
            symbol: IrConstructorSymbol,
            typeArgumentsCount: Int = symbol.owner.allTypeParameters.size,
            valueArgumentsCount: Int = symbol.owner.valueParameters.size
        ) = IrDelegatingConstructorCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount)
    }
}
