/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.Type

object RangeUntil : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext
    ): IrIntrinsicFunction {
        return object : IrIntrinsicFunction(expression, signature, context) {
            override fun genInvokeInstruction(v: InstructionAdapter) {
                v.invokestatic(
                    "kotlin/ranges/RangesKt", "until",
                    Type.getMethodDescriptor(signature.returnType, *argsTypes.toTypedArray()),
                    false
                )
            }
        }
    }
}
