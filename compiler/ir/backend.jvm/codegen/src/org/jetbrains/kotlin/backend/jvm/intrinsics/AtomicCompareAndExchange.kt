/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type.INT
import org.jetbrains.org.objectweb.asm.Type.LONG
import org.jetbrains.org.objectweb.asm.Type.BOOLEAN
import org.jetbrains.org.objectweb.asm.Type.OBJECT

class AtomicCompareAndExchange(private val valueType: Int) : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression, signature: JvmMethodSignature, classCodegen: ClassCodegen,
    ): IntrinsicFunction {
        return IntrinsicFunction.create(expression, signature, classCodegen) {
            val descriptor = when (valueType) {
                INT -> "(Ljava/util/concurrent/atomic/AtomicInteger;II)I"
                LONG -> "(Ljava/util/concurrent/atomic/AtomicLong;JJ)J"
                BOOLEAN -> "(Ljava/util/concurrent/atomic/AtomicBoolean;ZZ)Z"
                OBJECT -> "(Ljava/util/concurrent/atomic/AtomicReference;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
                else -> error("Unsupported value type: $valueType")
            }
            it.invokestatic("kotlin/concurrent/internal/AtomicIntrinsicsKt", "compareAndExchange", descriptor, false)
        }
    }
}

class AtomicArrayCompareAndExchange(private val valueType: Int) : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression, signature: JvmMethodSignature, classCodegen: ClassCodegen,
    ): IntrinsicFunction {
        return IntrinsicFunction.create(expression, signature, classCodegen) {
            val descriptor = when (valueType) {
                INT -> "(Ljava/util/concurrent/atomic/AtomicIntegerArray;III)I"
                LONG -> "(Ljava/util/concurrent/atomic/AtomicLongArray;IJJ)J"
                OBJECT -> "(Ljava/util/concurrent/atomic/AtomicReferenceArray;ILjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
                else -> error("Unsupported value type: $valueType")
            }
            it.invokestatic("kotlin/concurrent/internal/AtomicIntrinsicsKt", "compareAndExchange", descriptor, false)
        }
    }
}