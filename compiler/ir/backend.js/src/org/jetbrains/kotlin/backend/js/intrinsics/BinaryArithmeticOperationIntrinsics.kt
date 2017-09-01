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

package org.jetbrains.kotlin.backend.js.intrinsics

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.backend.js.util.numberToInt
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.types.KotlinType

abstract class BinaryArithmeticOperationIntrinsic : BinaryOperationIntrinsic() {
    private val supportedOperations = mutableSetOf("plus", "minus", "times", "rem", "div")

    override fun isApplicable(name: String, first: KotlinType, second: KotlinType): Boolean =
            first == second && isSupportedType(first) && name in supportedOperations

    abstract fun isSupportedType(type: KotlinType): Boolean
}

object IntArithmeticIntrinsic : BinaryArithmeticOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean =
            KotlinBuiltIns.isInt(type) ||
            KotlinBuiltIns.isByte(type) ||
            KotlinBuiltIns.isShort(type)

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression {
        if (call.descriptor.name.identifier == "times") {
            return buildJs { "Kotlin".dotPure("imul").invoke(first, second).pure() }
        }
        val operator = functionNameToOperation(call.descriptor.name.identifier)
        return buildJs { numberToInt(JsBinaryOperation(operator, first, second)) }
    }
}

object FloatArithmeticIntrinsic : BinaryArithmeticOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isFloat(type) || KotlinBuiltIns.isDouble(type)

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression {
        val operator = functionNameToOperation(call.descriptor.name.identifier)
        return JsBinaryOperation(operator, first, second)
    }
}

object LongArithmeticIntrinsic : BinaryArithmeticOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isLong(type)

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression {
        val functionName = when (call.descriptor.name.identifier) {
            "plus" -> "add"
            "minus" -> "subtract"
            "times" -> "multiply"
            "rem" -> "modulo"
            "div" -> "div"
            else -> error("unsupported function: ${call.descriptor}")
        }

        return buildJs { first.dotPure(functionName).invoke(second).pure() }
    }
}

private fun functionNameToOperation(name: String) = when (name) {
    "plus" -> JsBinaryOperator.ADD
    "minus" -> JsBinaryOperator.SUB
    "times" -> JsBinaryOperator.MUL
    "rem" -> JsBinaryOperator.MOD
    "div" -> JsBinaryOperator.DIV
    else -> error("unsupported function: $name")
}