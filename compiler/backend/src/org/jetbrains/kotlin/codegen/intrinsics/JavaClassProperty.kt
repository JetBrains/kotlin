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
import org.jetbrains.kotlin.codegen.AsmUtil.boxType
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.UNIT_TYPE
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.getType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

object JavaClassProperty : IntrinsicPropertyGetter() {
    override fun generate(
            resolvedCall: ResolvedCall<*>?,
            codegen: ExpressionCodegen,
            returnType: Type,
            receiver: StackValue
    ): StackValue? =
            StackValue.operation(returnType) {
                val actualType = generateImpl(it, receiver)
                StackValue.coerce(actualType, returnType, it)
            }

    fun generateImpl(v: InstructionAdapter, receiver: StackValue): Type {
        val type = receiver.type
        when {
            type == Type.VOID_TYPE -> {
                receiver.put(Type.VOID_TYPE, v)
                StackValue.unit().put(UNIT_TYPE, v)
                v.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
            }
            isPrimitive(type) -> {
                if (!StackValue.couldSkipReceiverOnStaticCall(receiver)) {
                    receiver.put(type, v)
                    AsmUtil.pop(v, type)
                }
                v.getstatic(boxType(type).internalName, "TYPE", "Ljava/lang/Class;")
            }
            else -> {
                receiver.put(type, v)
                v.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
            }
        }

        return getType(Class::class.java)
    }

    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): Callable {
        val classType = codegen.state.typeMapper.mapType(resolvedCall.call.dispatchReceiver!!.type)
        return object : IntrinsicCallable(getType(Class::class.java), listOf(), classType, null) {
            override fun invokeIntrinsic(v: InstructionAdapter) {
                generateImpl(v, StackValue.none())
            }

            override fun isStaticCall() = isPrimitive(classType)
        }
    }
}
