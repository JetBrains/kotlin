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
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public class RangeTo : IntrinsicMethod() {

    private fun nameToPrimitive(name: String) : Type {
        return when (name) {
            "Double" -> DOUBLE_TYPE;
            "Float" -> FLOAT_TYPE
            "Long" -> LONG_TYPE
            "Int" -> INT_TYPE
            "Short" -> SHORT_TYPE
            "Char" -> CHAR_TYPE
            "Byte" -> BYTE_TYPE
            else -> throw IllegalStateException("RangeTo intrinsic can only work for primitive types: " + name)
        }
    }

    override fun toCallable(method: CallableMethod): Callable {
        val argType = nameToPrimitive(method.returnType.getInternalName().substringAfter("kotlin/").substringBefore("Range"))
        return object : IntrinsicCallable(method.returnType, method.valueParameterTypes.map { argType }, nullOr(method.dispatchReceiverType, argType), nullOr(method.extensionReceiverType, argType)) {
            override fun beforeParameterGeneration(v: InstructionAdapter, value: StackValue?) {
                v.anew(returnType)
                v.dup()
                value?.moveToTopOfStack(value!!.type, v, 2)
            }

            override fun invokeIntrinsic(v: InstructionAdapter) {
                v.invokespecial(returnType.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, argType, argType), false)
            }
        }
    }
}