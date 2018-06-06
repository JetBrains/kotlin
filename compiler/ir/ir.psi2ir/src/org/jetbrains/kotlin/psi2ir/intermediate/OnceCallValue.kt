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
import org.jetbrains.kotlin.psi2ir.generators.CallGenerator
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator
import org.jetbrains.kotlin.types.KotlinType

class OnceCallValue(
    val startOffset: Int,
    val endOffset: Int,
    val statementGenerator: StatementGenerator,
    val call: CallBuilder,
    val origin: IrStatementOrigin? = null
) : IntermediateValue {
    private var instantiated = false

    override fun load(): IrExpression {
        if (instantiated) throw AssertionError("Value for call ${call.descriptor} has already been instantiated")
        instantiated = true
        return CallGenerator(statementGenerator).generateCall(startOffset, endOffset, call, origin)
    }

    override val type: KotlinType get() = call.descriptor.returnType!!
}
