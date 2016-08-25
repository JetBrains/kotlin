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

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi2ir.generators.Scope
import org.jetbrains.kotlin.types.KotlinType

class RematerializableValue(val irExpression: IrExpressionWithCopy) : Value {
    override val type: KotlinType?
        get() = irExpression.type

    override fun load(): IrExpression = irExpression.copy()
}

fun createRematerializableValue(irExpression: IrExpression): Value? =
        (irExpression as? IrExpressionWithCopy)?.let { RematerializableValue(it) }

inline fun createRematerializableOrTemporary(
        scope: Scope,
        irExpression: IrExpression,
        nameHint: String? = null,
        addVariable: (IrVariable) -> Unit
): Value {
    val rematerializable = createRematerializableValue(irExpression)
    if (rematerializable != null) {
        return rematerializable
    }

    val temporaryVariable = scope.createTemporaryVariable(irExpression, nameHint)
    addVariable(temporaryVariable)
    return VariableLValue(temporaryVariable)
}

fun createRematerializableOrTemporary(scope: Scope, irExpression: IrExpression, block: IrBlockImpl, nameHint: String? = null): Value =
        createRematerializableOrTemporary(scope, irExpression, nameHint) {
            block.addStatement(it)
        }