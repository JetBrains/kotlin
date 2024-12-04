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
import org.jetbrains.kotlin.ir.types.IrType

internal interface IntermediateValue {
    fun load(): IrExpression
    fun loadIfExists(): IrExpression? = load()
    val type: IrType
}

internal interface LValue : IntermediateValue {
    fun store(irExpression: IrExpression): IrExpression
}

internal interface AssignmentReceiver {
    fun assign(withLValue: (LValue) -> IrExpression): IrExpression
    fun assign(value: IrExpression): IrExpression = assign { it.store(value) }
}

internal fun interface CallExpressionBuilder {
    fun withReceivers(
        dispatchReceiver: IntermediateValue?,
        extensionReceiver: IntermediateValue?,
        contextReceivers: List<IntermediateValue>
    ): IrExpression
}

internal interface CallReceiver {
    fun call(builder: CallExpressionBuilder): IrExpression
}
