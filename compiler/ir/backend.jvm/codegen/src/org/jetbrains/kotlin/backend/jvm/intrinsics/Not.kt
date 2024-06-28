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

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.BooleanValue
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.coerceToBoolean
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.org.objectweb.asm.Label
import kotlin.math.exp

object Not : IntrinsicMethod() {
    class BooleanNegation(val expression: IrFunctionAccessExpression, val value: BooleanValue) : BooleanValue(value.codegen) {
        override fun jumpIfFalse(target: Label) {
            markLineNumber(expression)
            value.jumpIfTrue(target)
        }

        override fun jumpIfTrue(target: Label) {
            markLineNumber(expression)
            value.jumpIfFalse(target)
        }

        override fun discard() {
            markLineNumber(expression)
            value.discard()
        }
    }

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo) =
        BooleanNegation(expression, expression.dispatchReceiver!!.accept(codegen, data).coerceToBoolean())
}
