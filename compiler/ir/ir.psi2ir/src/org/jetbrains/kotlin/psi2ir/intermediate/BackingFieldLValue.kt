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

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.types.KotlinType

class BackingFieldLValue(
    private val context: IrGeneratorContext,
    private val startOffset: Int,
    private val endOffset: Int,
    override val type: IrType,
    private val symbol: IrFieldSymbol,
    private val receiver: IntermediateValue?,
    private val origin: IrStatementOrigin?
) : LValue, AssignmentReceiver {

    override fun store(irExpression: IrExpression): IrExpression =
        IrSetFieldImpl(startOffset, endOffset, symbol, receiver?.load(), irExpression, context.irBuiltIns.unitType, origin)

    override fun load(): IrExpression =
        IrGetFieldImpl(startOffset, endOffset, symbol, type, receiver?.load(), origin)

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression =
        withLValue(this)
}
