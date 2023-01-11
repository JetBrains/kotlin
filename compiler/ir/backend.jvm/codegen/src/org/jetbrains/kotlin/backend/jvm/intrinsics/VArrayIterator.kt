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

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isVArray
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

object VArrayIterator : IntrinsicMethod() {

    private val arrayTypeToIteratorType = mapOf(
        "Z" to "Boolean",
        "C" to "Char",
        "B" to "Byte",
        "S" to "Short",
        "I" to "Int",
        "F" to "Float",
        "J" to "Long",
        "D" to "Double"
    ).mapKeys { "[${it.key}" }.mapValues { "Lkotlin/collections/${it.value}Iterator;" }

    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IrIntrinsicFunction {
        val receiverTypeMapped = classCodegen.typeMapper.mapType(expression.dispatchReceiver!!.type)
        val owner = classCodegen.typeMapper.mapClass(expression.symbol.owner.parentAsClass)
        return IrIntrinsicFunction.create(expression, signature, classCodegen, owner) {
            if (AsmUtil.isPrimitive(receiverTypeMapped.elementType)) {
                it.invokestatic(
                    "kotlin/jvm/internal/ArrayIteratorsKt",
                    "iterator",
                    "(${receiverTypeMapped.descriptor})${arrayTypeToIteratorType[receiverTypeMapped.descriptor]!!}",
                    false
                )
            } else {
                it.invokestatic(
                    "kotlin/jvm/internal/ArrayIteratorKt",
                    "vArrayIterator",
                    "(${owner.descriptor})${signature.returnType.descriptor}",
                    false
                )
            }
        }
    }
}
