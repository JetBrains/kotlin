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
import org.jetbrains.kotlin.codegen.FrameMap
import org.jetbrains.kotlin.codegen.generateNewInstanceDupAndPlaceBeforeStackTop
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class RangeTo : IntrinsicMethod() {
    private fun rangeTypeToPrimitiveType(rangeType: Type): Type {
        val fqName = rangeType.internalName
        val name = fqName.substringAfter("kotlin/ranges/").substringBefore("Range")
        return when (name) {
            "Double" -> DOUBLE_TYPE
            "Float" -> FLOAT_TYPE
            "Long" -> LONG_TYPE
            "Int" -> INT_TYPE
            "Short" -> SHORT_TYPE
            "Char" -> CHAR_TYPE
            "Byte" -> BYTE_TYPE
            else -> throw IllegalStateException("RangeTo intrinsic can only work for primitive types: $fqName")
        }
    }

    override fun toCallable(method: CallableMethod): Callable {
        val argType = rangeTypeToPrimitiveType(method.returnType)
        return object : IntrinsicCallable(
                method.returnType,
                method.valueParameterTypes.map { argType },
                nullOr(method.dispatchReceiverType, argType),
                nullOr(method.extensionReceiverType, argType)
        ) {
            override fun afterReceiverGeneration(v: InstructionAdapter, frameMap: FrameMap, state: GenerationState) {
                v.generateNewInstanceDupAndPlaceBeforeStackTop(frameMap, argType, returnType.internalName)
            }

            override fun invokeIntrinsic(v: InstructionAdapter) {
                v.invokespecial(returnType.internalName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, argType, argType), false)
            }
        }
    }
}
