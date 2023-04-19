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
import org.jetbrains.kotlin.ir.util.initializeParameterArguments
import org.jetbrains.kotlin.ir.util.initializeTypeArguments

class IrLocalDelegatedPropertyReferenceImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override val symbol: IrLocalDelegatedPropertySymbol,
    override var delegate: IrVariableSymbol,
    override var getter: IrSimpleFunctionSymbol,
    override var setter: IrSimpleFunctionSymbol?,
    override var origin: IrStatementOrigin? = null,
) : IrLocalDelegatedPropertyReference() {
    override val typeArguments: Array<IrType?> = initializeTypeArguments(0)

    override val valueArguments: Array<IrExpression?> = initializeParameterArguments(0)
}
