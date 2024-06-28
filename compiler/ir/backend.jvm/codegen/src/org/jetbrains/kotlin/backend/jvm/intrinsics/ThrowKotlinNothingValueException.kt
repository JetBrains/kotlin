/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

object ThrowKotlinNothingValueException : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IntrinsicFunction =
        IntrinsicFunction.create(expression, signature, classCodegen) { mv ->
            if (classCodegen.context.config.useKotlinNothingValueException) {
                mv.anew(Type.getObjectType("kotlin/KotlinNothingValueException"))
                mv.dup()
                mv.invokespecial("kotlin/KotlinNothingValueException", "<init>", "()V", false)
            } else {
                mv.aconst(null)
            }
            mv.athrow()
        }
}
