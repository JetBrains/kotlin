/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type

class ThrowException(private val exceptionClass: Type) : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        with(codegen) {
            mv.anew(exceptionClass)
            mv.dup()
            gen(expression.getValueArgument(0)!!, AsmTypes.JAVA_STRING_TYPE, codegen.context.irBuiltIns.stringType, data)
            mv.invokespecial(exceptionClass.internalName, "<init>", "(Ljava/lang/String;)V", false)
            mv.athrow()
            return null
        }
    }
}
