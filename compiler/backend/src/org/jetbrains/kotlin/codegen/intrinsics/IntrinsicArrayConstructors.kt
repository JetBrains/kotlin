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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.inline.ReificationArgument
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal object IntrinsicArrayConstructors {
    fun generateArrayConstructorBody(method: Method): MethodNode {
        val node = MethodNode(
            Opcodes.ASM6, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL, method.name, method.descriptor, null, null
        )

        val arrayType = method.returnType
        val elementType = AsmUtil.correctElementType(arrayType)

        val size = StackValue.local(0, Type.INT_TYPE)
        val lambda = StackValue.local(1, AsmTypes.FUNCTION1)
        val loopIndex = StackValue.local(2, Type.INT_TYPE)
        val array = StackValue.local(3, arrayType)

        /*
        inline fun <reified T> Array(size: Int, init: (Int) -> T): Array<T> {
            val result = arrayOfNulls<T>(size)
            for (i in 0 until size) {
                result[i] = init(i)
            }
            return result as Array<T>
        }
         */

        val iv = InstructionAdapter(node)
        size.put(iv)
        if (elementType.sort == Type.OBJECT) {
            ReifiedTypeInliner.putReifiedOperationMarker(ReifiedTypeInliner.OperationKind.NEW_ARRAY, ReificationArgument("T", true, 0), iv)
        }
        iv.newarray(elementType)
        array.store(StackValue.onStack(arrayType), iv)
        loopIndex.store(StackValue.constant(0), iv)
        val begin = Label()
        iv.visitLabel(begin)
        loopIndex.put(iv)
        size.put(iv)
        val end = Label()
        iv.ificmpge(end)
        array.put(iv)
        loopIndex.put(iv)
        lambda.put(iv)
        loopIndex.put(AsmUtil.boxType(Type.INT_TYPE), iv)
        iv.invokeinterface(
            AsmTypes.FUNCTION1.internalName, OperatorNameConventions.INVOKE.asString(),
            Type.getMethodDescriptor(AsmTypes.OBJECT_TYPE, AsmTypes.OBJECT_TYPE)
        )
        StackValue.coerce(AsmTypes.OBJECT_TYPE, elementType, iv)
        iv.astore(elementType)
        iv.iinc(loopIndex.index, 1)
        iv.goTo(begin)
        iv.visitLabel(end)
        array.put(iv)
        iv.areturn(arrayType)
        iv.visitMaxs(5, 4)

        return node
    }

    fun generateEmptyArrayBody(method: Method): MethodNode {
        val node = MethodNode(
            Opcodes.ASM6, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL, method.name, method.descriptor, null, null
        )

        /*
        inline fun <reified T> emptyArray(): Array<T> = arrayOfNulls<T>(0) as Array<T>
         */

        val iv = InstructionAdapter(node)
        iv.iconst(0)
        ReifiedTypeInliner.putReifiedOperationMarker(ReifiedTypeInliner.OperationKind.NEW_ARRAY, ReificationArgument("T", true, 0), iv)
        iv.newarray(AsmTypes.OBJECT_TYPE)
        iv.areturn(AsmTypes.OBJECT_TYPE)
        iv.visitMaxs(3, 1)

        return node
    }

    fun generateArrayOfBody(method: Method): MethodNode {
        val node = MethodNode(
            Opcodes.ASM6, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL, method.name, method.descriptor, null, null
        )

        /*
        inline fun <reified T> arrayOf(vararg elements: T): Array<T> = elements as Array<T>
         */

        val iv = InstructionAdapter(node)
        iv.load(0, AsmTypes.OBJECT_TYPE)
        iv.areturn(AsmTypes.OBJECT_TYPE)
        iv.visitMaxs(1, 1)

        return node
    }
}
