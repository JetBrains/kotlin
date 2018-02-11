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

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.types.KotlinType

class DelegatedLocalPropertyLValue(
    val startOffset: Int,
    val endOffset: Int,
    override val type: KotlinType,
    val getterSymbol: IrSimpleFunctionSymbol?,
    val setterSymbol: IrSimpleFunctionSymbol?,
    val origin: IrStatementOrigin? = null
) : LValue, AssignmentReceiver {
    override fun load(): IrExpression =
        IrCallImpl(startOffset, endOffset, type, getterSymbol!!, getterSymbol.descriptor, null, origin)

    override fun store(irExpression: IrExpression): IrExpression =
        IrCallImpl(startOffset, endOffset, type, setterSymbol!!, setterSymbol.descriptor, null, origin).apply {
            putValueArgument(0, irExpression)
        }

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression =
        withLValue(this)
}
