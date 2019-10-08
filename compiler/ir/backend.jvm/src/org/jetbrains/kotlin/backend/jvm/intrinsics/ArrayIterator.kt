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

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.mapClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature

object ArrayIterator : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        val owner = context.typeMapper.mapClass(expression.symbol.owner.parentAsClass)
        return IrIntrinsicFunction.create(expression, signature, context, owner) {
            val methodSignature = "(${owner.descriptor})${signature.returnType.descriptor}"
            val intrinsicOwner =
                if (AsmUtil.isPrimitive(owner.elementType))
                    "kotlin/jvm/internal/ArrayIteratorsKt"
                else
                    "kotlin/jvm/internal/ArrayIteratorKt"
            it.invokestatic(intrinsicOwner, "iterator", methodSignature, false)
        }
    }
}
