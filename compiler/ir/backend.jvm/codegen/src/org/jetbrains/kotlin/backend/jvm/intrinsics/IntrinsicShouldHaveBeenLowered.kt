/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature

object IntrinsicShouldHaveBeenLowered : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IntrinsicFunction {
        error("Intrinsic should have been lowered: '${expression.symbol.owner.render()}'")
    }
}