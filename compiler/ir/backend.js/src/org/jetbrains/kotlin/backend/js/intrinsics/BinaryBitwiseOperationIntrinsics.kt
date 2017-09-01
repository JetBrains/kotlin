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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.types.KotlinType

abstract class BinaryBitwiseOperationIntrinsic : BinaryOperationIntrinsic() {
    private val supportedOperations = mutableSetOf("and", "or", "xor")

    override fun isApplicable(name: String, first: KotlinType, second: KotlinType): Boolean =
            first == second && isSupportedType(first) && name in supportedOperations

    abstract fun isSupportedType(type: KotlinType): Boolean
}

abstract class ShiftOperationIntrinsic : BinaryOperationIntrinsic() {
    private val supportedOperations = mutableSetOf("shl", "shr", "ushr")

    override fun isApplicable(name: String, first: KotlinType, second: KotlinType): Boolean =
            isSupportedType(first) && KotlinBuiltIns.isInt(second) && name in supportedOperations

    abstract fun isSupportedType(type: KotlinType): Boolean
}

object IntBinaryBitwiseIntrinsic : BinaryBitwiseOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isInt(type)

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression {
        val operator = when (call.descriptor.name.asString()) {
            "and" -> JsBinaryOperator.BIT_AND
            "or" -> JsBinaryOperator.BIT_OR
            "xor" -> JsBinaryOperator.BIT_XOR
            else -> error("Unexpected function: ${call.descriptor}")
        }

        return JsBinaryOperation(operator, first, second)
    }
}

object LongBinaryBitwiseIntrinsic : BinaryBitwiseOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isLong(type)

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression =
            buildJs { first.dotPure(call.descriptor.name.asString()).invoke(second).pure() }
}

object IntShiftOperationIntrinsic : ShiftOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isInt(type)

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression {
        val operator = when (call.descriptor.name.asString()) {
            "shl" -> JsBinaryOperator.SHL
            "shr" -> JsBinaryOperator.SHR
            "ushr" -> JsBinaryOperator.SHRU
            else -> error("Unexpected function: ${call.descriptor}")
        }

        return JsBinaryOperation(operator, first, second)
    }
}

object LongShiftOperationIntrinsic : ShiftOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isLong(type)

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression {
        val functionName = when (call.descriptor.name.asString()) {
            "shl" -> "shiftLeft"
            "shr" -> "shiftRight"
            "ushr" -> "shiftRightUnsigned"
            else -> error("Unexpected function: ${call.descriptor}")
        }

        return buildJs { first.dotPure(functionName).invoke(second).pure() }
    }
}