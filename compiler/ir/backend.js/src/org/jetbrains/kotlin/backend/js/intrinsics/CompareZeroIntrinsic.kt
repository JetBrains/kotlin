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

import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral

object CompareZeroIntrinsic : BaseIrIntrinsic() {
    override fun names() = listOf(
            "kotlin.internal.ir.GT0",
            "kotlin.internal.ir.LT0",
            "kotlin.internal.ir.GTEQ0",
            "kotlin.internal.ir.LTEQ0"
    )

    override fun apply(name: String, arguments: List<JsExpression>): JsExpression {
        val operator = when (name) {
            "GT0" -> JsBinaryOperator.GT
            "LT0" -> JsBinaryOperator.LT
            "GTEQ0" -> JsBinaryOperator.GTE
            "LTEQ0" -> JsBinaryOperator.LTE
            else -> error("Unsupported intrinsic")
        }
        return JsBinaryOperation(operator, arguments[0], JsIntLiteral(0))
    }
}