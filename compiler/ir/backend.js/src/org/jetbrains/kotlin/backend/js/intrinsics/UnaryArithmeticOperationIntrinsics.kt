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
import org.jetbrains.kotlin.backend.js.util.toByte
import org.jetbrains.kotlin.backend.js.util.toShort
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.types.KotlinType

abstract class UnaryArithmeticOperationIntrinsic : UnaryOperationIntrinsic() {
    private val supportedOperations = mutableSetOf("unaryMinus")

    override fun isApplicable(name: String, operand: KotlinType): Boolean = isSupportedType(operand) && name in supportedOperations

    abstract fun isSupportedType(type: KotlinType): Boolean

    override fun apply(context: IrTranslationContext, call: IrCall, operand: JsExpression): JsExpression {
        val operator = when (call.descriptor.name.identifier) {
            "unaryMinus" -> JsUnaryOperator.NEG
            else -> error("unsupported function: ${call.descriptor}")
        }

        return wrap(JsPrefixOperation(operator, operand))
    }

    abstract fun wrap(value: JsExpression): JsExpression
}

object IntUnaryArithmeticIntrinsic : UnaryArithmeticOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isInt(type)

    override fun wrap(value: JsExpression): JsExpression = buildJs { numberToInt(value) }
}

object FloatUnaryArithmeticIntrinsic : UnaryArithmeticOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isFloat(type) || KotlinBuiltIns.isDouble(type)

    override fun wrap(value: JsExpression): JsExpression = value
}

object ByteUnaryArithmeticIntrinsic : UnaryArithmeticOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isByte(type)

    override fun wrap(value: JsExpression): JsExpression = buildJs { toByte(value) }
}

object ShortUnaryArithmeticIntrinsic : UnaryArithmeticOperationIntrinsic() {
    override fun isSupportedType(type: KotlinType): Boolean = KotlinBuiltIns.isShort(type)

    override fun wrap(value: JsExpression): JsExpression = buildJs { toShort(value) }
}