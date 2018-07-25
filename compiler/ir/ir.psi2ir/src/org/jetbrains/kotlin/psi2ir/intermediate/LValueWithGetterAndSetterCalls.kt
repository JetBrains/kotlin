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
import org.jetbrains.kotlin.types.KotlinType

class LValueWithGetterAndSetterCalls(
    val callGenerator: CallGenerator,
    val getterCall: CallBuilder?,
    val setterCall: CallBuilder?,
    override val type: IrType,
    val startOffset: Int,
    val endOffset: Int,
    val origin: IrStatementOrigin? = null
) : LValue {

    private val descriptor: CallableDescriptor =
        getterCall?.descriptor ?: setterCall?.descriptor
        ?: throw AssertionError("Call-based LValue should have either a getter or a setter call")

    override fun load(): IrExpression {
        if (getterCall == null) throw AssertionError("No getter call for $descriptor")
        return callGenerator.generateCall(startOffset, endOffset, getterCall, origin)
    }

    override fun store(irExpression: IrExpression): IrExpression {
        if (setterCall == null) throw AssertionError("No setter call for $descriptor")
        setterCall.irValueArgumentsByIndex[setterCall.argumentsCount - 1] = irExpression
        return callGenerator.generateCall(startOffset, endOffset, setterCall, origin)
    }

}
