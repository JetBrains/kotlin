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
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.KotlinType

abstract class ExpressionValue(override val type: KotlinType) : IntermediateValue

inline fun generateExpressionValue(type: KotlinType, crossinline generate: () -> IrExpression) =
    object : ExpressionValue(type) {
        override fun load(): IrExpression = generate()
    }

inline fun generateDelegatedValue(type: KotlinType, crossinline generateValue: () -> IntermediateValue) =
    object : ExpressionValue(type) {
        val lazyDelegate by lazy { generateValue() }
        override fun load(): IrExpression = lazyDelegate.load()
    }

class OnceExpressionValue(val irExpression: IrExpression) : LValue, AssignmentReceiver {
    private var instantiated = false

    override fun load(): IrExpression {
        if (instantiated) throw AssertionError("Single expression value for ${irExpression.render()} is already instantiated")
        instantiated = true
        return irExpression
    }

    override val type: KotlinType get() = irExpression.type

    override fun store(irExpression: IrExpression): IrExpression {
        throw AssertionError("Expression value ${irExpression.render()} can't be used in store operation")
    }

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression =
        withLValue(this)
}