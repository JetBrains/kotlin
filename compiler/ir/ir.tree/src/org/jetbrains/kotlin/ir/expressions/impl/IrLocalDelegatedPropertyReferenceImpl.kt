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
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrLocalDelegatedPropertyReferenceImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override val symbol: IrLocalDelegatedPropertySymbol,
    override val delegate: IrVariableSymbol,
    override val getter: IrSimpleFunctionSymbol,
    override val setter: IrSimpleFunctionSymbol?,
    override val origin: IrStatementOrigin? = null,
) : IrLocalDelegatedPropertyReference() {
    override val typeArgumentsByIndex: Array<IrType?> = emptyArray()

    override val argumentsByParameterIndex: Array<IrExpression?>
        get() = throw UnsupportedOperationException("Property reference $symbol has no value arguments")

    override val valueArgumentsCount: Int
        get() = 0

    override val referencedName: Name
        get() = symbol.owner.name
}
