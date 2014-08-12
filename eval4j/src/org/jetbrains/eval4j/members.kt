/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode

open data class MemberDescription(
        val ownerInternalName: String,
        val name: String,
        val desc: String,
        val isStatic: Boolean
)

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
            insn.getOpcode() == INVOKESTATIC
        )

val MethodDescription.returnType: Type
    get() = Type.getReturnType(desc)

val MethodDescription.parameterTypes: List<Type>
    get() = Type.getArgumentTypes(desc).toList()


public class FieldDescription(
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
                insn.getOpcode() in setOf(GETSTATIC, PUTSTATIC)
        )

val FieldDescription.fieldType: Type
    get() = Type.getType(desc)
