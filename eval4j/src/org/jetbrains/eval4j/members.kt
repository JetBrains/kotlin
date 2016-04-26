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

package org.jetbrains.eval4j

import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode

open class MemberDescription protected constructor(
        val ownerInternalName: String,
        val name: String,
        val desc: String,
        val isStatic: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is MemberDescription
                && ownerInternalName == other.ownerInternalName
                && name == other.name
                && desc == other.desc
                && isStatic == other.isStatic
               )
    }

    override fun hashCode(): Int {
        var result = 13
        result = result * 23 + ownerInternalName.hashCode()
        result = result * 23 + name.hashCode()
        result = result * 23 + desc.hashCode()
        result = result * 23 + isStatic.hashCode()
        return result
    }

    override fun toString() = "MemberDescription(ownerInternalName = $ownerInternalName, name = $name, desc = $desc, isStatic = $isStatic)"
}

val MemberDescription.ownerType: Type
    get() = Type.getObjectType(ownerInternalName)

class MethodDescription(
        ownerInternalName: String,
        name: String,
        desc: String,
        isStatic: Boolean
) : MemberDescription(ownerInternalName, name, desc, isStatic)

fun MethodDescription(insn: MethodInsnNode): MethodDescription =
        MethodDescription(
            insn.owner,
            insn.name,
            insn.desc,
            insn.opcode == INVOKESTATIC
        )

val MethodDescription.returnType: Type
    get() = Type.getReturnType(desc)

val MethodDescription.parameterTypes: List<Type>
    get() = Type.getArgumentTypes(desc).toList()


class FieldDescription(
        ownerInternalName: String,
        name: String,
        desc: String,
        isStatic: Boolean
) : MemberDescription(ownerInternalName, name, desc, isStatic)

fun FieldDescription(insn: FieldInsnNode): FieldDescription =
        FieldDescription(
                insn.owner,
                insn.name,
                insn.desc,
                insn.opcode in setOf(GETSTATIC, PUTSTATIC)
        )

val FieldDescription.fieldType: Type
    get() = Type.getType(desc)
