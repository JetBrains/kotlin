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

object StringPlusIntrinsic : BinaryOperationIntrinsic() {
    override fun isApplicable(name: String, first: KotlinType, second: KotlinType): Boolean =
            name == "plus" &&
            KotlinBuiltIns.isString(first)

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression =
            JsBinaryOperation(JsBinaryOperator.ADD, first, buildJs { "Kotlin".dotPure("toString").invoke(second) })
}