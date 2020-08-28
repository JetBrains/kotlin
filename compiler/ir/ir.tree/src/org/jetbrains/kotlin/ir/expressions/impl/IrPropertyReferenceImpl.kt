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

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class IrPropertyReferenceImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override val type: IrType,
    override val symbol: IrPropertySymbol,
    typeArgumentsCount: Int,
    override val field: IrFieldSymbol?,
    override val getter: IrSimpleFunctionSymbol?,
    override val setter: IrSimpleFunctionSymbol?,
    override val origin: IrStatementOrigin? = null,
) : IrPropertyReference(typeArgumentsCount) {
    override val valueArgumentsCount: Int
        get() = 0

    override val referencedName: Name
        get() = symbol.owner.name

    private fun throwNoValueArguments(): Nothing =
        throw UnsupportedOperationException("Property reference $symbol has no value arguments")

    override fun getValueArgument(index: Int): IrExpression? = throwNoValueArguments()

    override fun putValueArgument(index: Int, valueArgument: IrExpression?): Unit = throwNoValueArguments()

    override fun removeValueArgument(index: Int): Unit = throwNoValueArguments()

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitPropertyReference(this, data)
}
