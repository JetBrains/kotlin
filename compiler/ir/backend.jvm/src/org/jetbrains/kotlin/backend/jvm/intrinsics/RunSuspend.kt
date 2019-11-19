/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature

object RunSuspend : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        return IrIntrinsicFunction.create(expression, signature, context) {
            it.invokestatic("kotlin/coroutines/jvm/internal/RunSuspendKt", "runSuspend", "(Lkotlin/jvm/functions/Function1;)V", false)
        }
    }
}