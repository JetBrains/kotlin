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

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes

class MonitorInstruction private constructor(private val opcode: Int) : IntrinsicMethod() {
    companion object {
        @JvmField
        val MONITOR_ENTER: MonitorInstruction = MonitorInstruction(Opcodes.MONITORENTER)

        @JvmField
        val MONITOR_EXIT: MonitorInstruction = MonitorInstruction(Opcodes.MONITOREXIT)
    }

    override fun toCallable(
        expression: IrFunctionAccessExpression, signature: JvmMethodSignature, classCodegen: ClassCodegen,
    ): IntrinsicFunction {
        return IntrinsicFunction.create(expression, signature, classCodegen, listOf(OBJECT_TYPE)) {
            it.visitInsn(opcode)
        }
    }
}
