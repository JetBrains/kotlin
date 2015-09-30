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

import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode

object CheckCast {
    private val INTRINSICS_CLASS = "kotlin/jvm/internal/Intrinsics"

    private val CHECKCAST_METHOD_NAME = hashMapOf(
            "kotlin.MutableIterator" to "asMutableIterator",
            "kotlin.MutableIterable" to "asMutableIterable",
            "kotlin.MutableCollection" to "asMutableCollection",
            "kotlin.MutableList" to "asMutableList",
            "kotlin.MutableListIterator" to "asMutableListIterator",
            "kotlin.MutableSet" to "asMutableSet",
            "kotlin.MutableMap" to "asMutableMap",
            "kotlin.MutableMap.MutableEntry" to "asMutableMapEntry"
    )

    private val SAFE_CHECKCAST_METHOD_NAME = hashMapOf(
            "kotlin.MutableIterator" to "safeAsMutableIterator",
            "kotlin.MutableIterable" to "safeAsMutableIterable",
            "kotlin.MutableCollection" to "safeAsMutableCollection",
            "kotlin.MutableList" to "safeAsMutableList",
            "kotlin.MutableListIterator" to "safeAsMutableListIterator",
            "kotlin.MutableSet" to "safeAsMutableSet",
            "kotlin.MutableMap" to "safeAsMutableMap",
            "kotlin.MutableMap.MutableEntry" to "safeAsMutableMapEntry"
    )

    public @JvmStatic fun checkcast(v: InstructionAdapter, jetType: JetType, boxedAsmType: Type, safe: Boolean) {
        val intrinsicMethodName = getCheckcastIntrinsicMethodName(jetType, safe)
        if (intrinsicMethodName == null) {
            v.checkcast(boxedAsmType)
        }
        else {
            val signature = getCheckcastIntrinsicMethodSignature(boxedAsmType)
            v.invokestatic(INTRINSICS_CLASS, intrinsicMethodName, signature, false)
        }
    }

    public @JvmStatic fun checkcast(checkcastInsn: TypeInsnNode, instructions: InsnList, jetType: JetType, asmType: Type, safe: Boolean) {
        val intrinsicMethodName = getCheckcastIntrinsicMethodName(jetType, safe)
        if (intrinsicMethodName == null) {
            checkcastInsn.desc = asmType.internalName
        }
        else {
            val signature = getCheckcastIntrinsicMethodSignature(asmType)
            val invokeNode = MethodInsnNode(Opcodes.INVOKESTATIC, INTRINSICS_CLASS, intrinsicMethodName, signature, false)
            instructions.insertBefore(checkcastInsn, invokeNode)
            instructions.remove(checkcastInsn)
        }
    }

    private fun getCheckcastIntrinsicMethodName(jetType: JetType, safe: Boolean): String? {
        val classDescriptor = TypeUtils.getClassDescriptor(jetType) ?: return null
        val classFqName = DescriptorUtils.getFqName(classDescriptor).asString()
        return if (safe) SAFE_CHECKCAST_METHOD_NAME[classFqName] else CHECKCAST_METHOD_NAME[classFqName]
    }

    private fun getCheckcastIntrinsicMethodSignature(asmType: Type): String =
            "(Ljava/lang/Object;)${asmType.descriptor}"

}