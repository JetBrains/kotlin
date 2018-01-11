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

import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionWithCopy
import org.jetbrains.kotlin.ir.expressions.impl.IrContainerExpressionBase
import org.jetbrains.kotlin.types.KotlinType

class RematerializableValue(val irExpression: IrExpressionWithCopy) : IntermediateValue {
    override val type: KotlinType get() = irExpression.type

    override fun load(): IrExpression = irExpression.copy()
}

fun Scope.createTemporaryVariableInBlock(
    irExpression: IrExpression,
    block: IrContainerExpressionBase,
    nameHint: String? = null
): IntermediateValue {
    val temporaryVariable = createTemporaryVariable(irExpression, nameHint)
    block.statements.add(temporaryVariable)
    return VariableLValue(temporaryVariable)
}