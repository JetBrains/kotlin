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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.psi2ir.generators.CallGenerator

class LValueWithGetterAndSetterCalls(
    val callGenerator: CallGenerator,
    val descriptor: CallableDescriptor,
    val getterCall: () -> CallBuilder?,
    val setterCall: (IrExpression) -> CallBuilder?,
    override val type: IrType,
    val startOffset: Int,
    val endOffset: Int,
    val origin: IrStatementOrigin? = null
) : LValue {

    override fun load(): IrExpression {
        val call = getterCall() ?: throw AssertionError("No getter call for $descriptor")
        return callGenerator.generateCall(startOffset, endOffset, call, origin)
    }

    override fun store(irExpression: IrExpression): IrExpression {
        val call = setterCall(irExpression) ?: throw AssertionError("No setter call for $descriptor")
        return callGenerator.generateCall(startOffset, endOffset, call, origin)
    }

}
