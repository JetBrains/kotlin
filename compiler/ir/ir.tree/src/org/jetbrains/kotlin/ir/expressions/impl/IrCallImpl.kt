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
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.render

class IrCallImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override val symbol: IrSimpleFunctionSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
    override var origin: IrStatementOrigin? = null,
    override var superQualifierSymbol: IrClassSymbol? = null
) : IrCall() {

    override val typeArguments: Array<IrType?> = arrayOfNulls(typeArgumentsCount)

    override val valueArguments: Array<IrExpression?> = arrayOfNulls(valueArgumentsCount)

    override var contextReceiversCount = 0

    init {
        if (symbol is IrConstructorSymbol) {
            throw AssertionError("Should be IrConstructorCall: ${this.render()}")
        }
    }

    companion object {
        @ObsoleteDescriptorBasedAPI
        fun fromSymbolDescriptor(
            startOffset: Int,
            endOffset: Int,
            type: IrType,
            symbol: IrSimpleFunctionSymbol,
            typeArgumentsCount: Int = symbol.descriptor.typeParametersCount,
            valueArgumentsCount: Int = symbol.descriptor.valueParameters.size + symbol.descriptor.contextReceiverParameters.size,
            origin: IrStatementOrigin? = null,
            superQualifierSymbol: IrClassSymbol? = null,
        ) =
            IrCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount, origin, superQualifierSymbol)

        fun fromSymbolOwner(
            startOffset: Int,
            endOffset: Int,
            type: IrType,
            symbol: IrSimpleFunctionSymbol,
            typeArgumentsCount: Int = symbol.owner.typeParameters.size,
            valueArgumentsCount: Int = symbol.owner.valueParameters.size,
            origin: IrStatementOrigin? = null,
            superQualifierSymbol: IrClassSymbol? = null,
        ) =
            IrCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount, origin, superQualifierSymbol)

        fun fromSymbolOwner(
            startOffset: Int,
            endOffset: Int,
            symbol: IrSimpleFunctionSymbol
        ) =
            IrCallImpl(
                startOffset,
                endOffset,
                symbol.owner.returnType,
                symbol,
                typeArgumentsCount = symbol.owner.typeParameters.size,
                valueArgumentsCount = symbol.owner.valueParameters.size,
                origin = null,
                superQualifierSymbol = null
            )

    }
}
