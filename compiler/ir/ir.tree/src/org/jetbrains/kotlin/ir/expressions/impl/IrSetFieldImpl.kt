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

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.IrType

class IrSetFieldImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var symbol: IrFieldSymbol,
    override var type: IrType,
    override var origin: IrStatementOrigin? = null,
    override var superQualifierSymbol: IrClassSymbol? = null,
) : IrSetField() {
    constructor(
        startOffset: Int, endOffset: Int,
        symbol: IrFieldSymbol,
        receiver: IrExpression?,
        value: IrExpression,
        type: IrType,
        origin: IrStatementOrigin? = null,
        superQualifierSymbol: IrClassSymbol? = null
    ) : this(startOffset, endOffset, symbol, type, origin, superQualifierSymbol) {
        this.receiver = receiver
        this.value = value
    }

    override lateinit var value: IrExpression
}
