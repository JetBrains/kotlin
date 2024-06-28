/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

object StringPlus : IntrinsicMethod() {

    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IntrinsicFunction =
        IntrinsicFunction.create(expression, signature, classCodegen, listOf(AsmTypes.JAVA_STRING_TYPE, AsmTypes.OBJECT_TYPE)) {
            it.invokestatic(
                IntrinsicMethods.INTRINSICS_CLASS_NAME,
                "stringPlus",
                Type.getMethodDescriptor(AsmTypes.JAVA_STRING_TYPE, AsmTypes.JAVA_STRING_TYPE, AsmTypes.OBJECT_TYPE),
                false
            )
        }
}