/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.AsmUtil.boxType
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

object JavaClassProperty : IntrinsicMethod() {

    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        return object: IrIntrinsicFunction(expression, signature, context) {

            override fun invoke(v: InstructionAdapter, codegen: org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen, data: BlockInfo): StackValue {
                val value = codegen.gen(expression.extensionReceiver!!, data)
                val type = value.type
                when {
                    type == Type.VOID_TYPE -> {
                        StackValue.unit().put(v)
                        v.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
                    }
                    isPrimitive(type) -> {
                        AsmUtil.pop(v, type)
                        v.getstatic(boxType(type).internalName, "TYPE", "Ljava/lang/Class;")
                    }
                    else -> v.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
                }

                return with(codegen) {
                    expression.onStack
                }
            }
        }
    }
}
