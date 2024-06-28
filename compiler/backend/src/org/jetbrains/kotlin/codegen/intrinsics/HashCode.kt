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

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class HashCode : IntrinsicMethod() {
    override fun toCallable(method: CallableMethod): Callable {
        val receiverType = method.dispatchReceiverType ?: method.extensionReceiverType ?: error("No receiver for callable: $method")
        val useObjectHashCode = !AsmUtil.isPrimitive(receiverType)
        return object : IntrinsicCallable(
                Type.INT_TYPE,
                emptyList(),
                if (useObjectHashCode) nullOrObject(method.dispatchReceiverType) else method.dispatchReceiverType,
                if (useObjectHashCode) nullOrObject(method.extensionReceiverType) else method.extensionReceiverType
        ) {
            override fun invokeIntrinsic(v: InstructionAdapter) {
                v.invokeHashCode(if (useObjectHashCode) AsmTypes.OBJECT_TYPE else receiverType)
            }
        }
    }

    companion object {
        fun InstructionAdapter.invokeHashCode(type: Type) {
            if (AsmUtil.isPrimitive(type)) {
                val boxedType = AsmUtil.boxType(type)
                visitMethodInsn(Opcodes.INVOKESTATIC, boxedType.internalName, "hashCode", Type.getMethodDescriptor(Type.INT_TYPE, type), false)
            }
            else {
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false)
            }
        }
    }
}
