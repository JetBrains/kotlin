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

import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createValueSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrGetValueImpl(
    startOffset: Int, endOffset: Int,
    symbol: IrValueSymbol,
    override val origin: IrStatementOrigin? = null
) : IrGetValue,
    IrTerminalDeclarationReferenceBase<IrValueSymbol, ValueDescriptor>(
        startOffset,
        endOffset,
        symbol.descriptor.type,
        symbol,
        symbol.descriptor
    ) {
    @Deprecated("Creates unbound reference")
    constructor(
        startOffset: Int, endOffset: Int,
        descriptor: ValueDescriptor,
        origin: IrStatementOrigin? = null
    ) : this(startOffset, endOffset, createValueSymbol(descriptor), origin)

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitGetValue(this, data)

    override fun copy(): IrGetValue =
        IrGetValueImpl(startOffset, endOffset, symbol, origin)
}