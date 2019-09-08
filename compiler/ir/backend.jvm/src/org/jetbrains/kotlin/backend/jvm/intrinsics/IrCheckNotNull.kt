/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

object IrCheckNotNull : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val arg0 = expression.getValueArgument(0)!!.accept(codegen, data)
        if (AsmUtil.isPrimitive(arg0.type)) return arg0

        return object : PromisedValue(codegen, arg0.type, expression.type) {
            override fun materialize() {
                arg0.materialize()
                mv.dup()
                val ifNonNullLabel = Label()
                mv.ifnonnull(ifNonNullLabel)
                mv.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "throwNpe", Type.getMethodDescriptor(Type.VOID_TYPE), false)
                mv.mark(ifNonNullLabel)
            }
        }
    }
}