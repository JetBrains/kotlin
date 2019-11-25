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

import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType

abstract class IrNoArgumentsCallableReferenceBase(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    typeArgumentsCount: Int,
    origin: IrStatementOrigin? = null
) :
    IrMemberAccessExpressionBase(startOffset, endOffset, type, typeArgumentsCount, 0, origin),
    IrCallableReference {

    private fun throwNoValueArguments(): Nothing {
        throw UnsupportedOperationException("Property reference ${symbol.descriptor} has no value arguments")
    }

    override fun getValueArgument(index: Int): IrExpression? = throwNoValueArguments()

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) = throwNoValueArguments()

    override fun removeValueArgument(index: Int) = throwNoValueArguments()
}

