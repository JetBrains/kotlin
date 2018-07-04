/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class NewArray : IntrinsicMethod() {

    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        val ktType = expression.type
        return object : IrIntrinsicFunction(expression, signature, context) {
            override fun invoke(v: InstructionAdapter, codegen: ExpressionCodegen, data: BlockInfo): StackValue {
                super.invoke(v, codegen, data)
                codegen.newArrayInstruction(ktType.toKotlinType())
                return StackValue.onStack(returnType)
            }

            override fun genInvokeInstruction(v: InstructionAdapter) {
            }
        }
    }
}
