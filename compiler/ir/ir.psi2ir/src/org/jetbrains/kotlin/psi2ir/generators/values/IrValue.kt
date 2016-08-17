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

package org.jetbrains.kotlin.psi2ir.generators.values

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.psi2ir.createDefaultGetExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

interface IrValue {
    fun load(): IrExpression
}

class IrTemporaryVariableValue(val irVariable: IrVariable) : IrValue {
    override fun load(): IrExpression =
            irVariable.createDefaultGetExpression()
}

class IrSingleExpressionValue(val irExpression: IrExpression) : IrValue {
    override fun load() = irExpression
}

interface IrLValue : IrValue {
    fun store(irExpression: IrExpression): IrExpression
}

interface IrLValueWithAugmentedStore : IrLValue {
    fun augmentedStore(operatorCall: ResolvedCall<*>, irRhs: IrExpression): IrExpression
}

