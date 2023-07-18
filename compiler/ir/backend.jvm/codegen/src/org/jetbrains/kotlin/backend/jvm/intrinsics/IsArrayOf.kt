/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature

object IsArrayOf : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IntrinsicFunction = IntrinsicFunction.create(expression, signature, classCodegen) { v ->
        val arrayType = classCodegen.context.irBuiltIns.arrayClass.typeWith(expression.getTypeArgument(0)!!)
        v.instanceOf(classCodegen.typeMapper.mapType(arrayType))
    }
}
