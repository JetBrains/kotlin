/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class MonitorInstruction private constructor(private val opcode: Int) : IntrinsicMethod() {
    companion object {
        @JvmField
        val MONITOR_ENTER: MonitorInstruction = MonitorInstruction(Opcodes.MONITORENTER)

        @JvmField
        val MONITOR_EXIT: MonitorInstruction = MonitorInstruction(Opcodes.MONITOREXIT)
    }

    override fun toCallable(method: CallableMethod): Callable {
        return object : IntrinsicCallable(Type.VOID_TYPE, listOf(OBJECT_TYPE), null, null) {
            override fun invokeIntrinsic(v: InstructionAdapter) {
                v.visitInsn(opcode)
            }
        }
    }
}
