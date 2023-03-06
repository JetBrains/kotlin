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
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType

class IrEnumConstructorCallImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override val symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int
) : IrEnumConstructorCall() {
    override var origin: IrStatementOrigin? = null

    override val typeArguments: Array<IrType?> = arrayOfNulls(typeArgumentsCount)

    override val valueArguments: Array<IrExpression?> = arrayOfNulls(valueArgumentsCount)

    override var contextReceiversCount = 0

    companion object {
        @ObsoleteDescriptorBasedAPI
        fun fromSymbolDescriptor(
            startOffset: Int,
            endOffset: Int,
            type: IrType,
            symbol: IrConstructorSymbol,
            typeArgumentsCount: Int
        ) = IrEnumConstructorCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, symbol.descriptor.valueParameters.size)
    }
}
