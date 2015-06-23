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

package org.jetbrains.kotlin.codegen.pseudoInsns

import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import kotlin.platform.platformStatic

public val PSEUDO_INSN_CALL_OWNER: String = "kotlin.jvm.\$PseudoInsn"
public val PSEUDO_INSN_PARTS_SEPARATOR: String = ":"

public enum class PseudoInsnOpcode(val signature: String = "()V") {
    FIX_STACK_BEFORE_JUMP(),
    FAKE_ALWAYS_TRUE_IFEQ("()I"),
    FAKE_ALWAYS_FALSE_IFEQ("()I")
    ;

    public fun insnOf(): PseudoInsn = PseudoInsn(this, emptyList())
    public fun insnOf(args: List<String>): PseudoInsn = PseudoInsn(this, args)

    public fun parseOrNull(insn: AbstractInsnNode): PseudoInsn? {
        val pseudo = parseOrNull(insn)
        return if (pseudo?.opcode == this) pseudo else null
    }

    public fun isa(insn: AbstractInsnNode): Boolean =
            if (isPseudoInsn(insn)) {
                val methodName = (insn as MethodInsnNode).name
                methodName == this.toString() || methodName.startsWith(this.toString() + PSEUDO_INSN_PARTS_SEPARATOR)
            }
            else false

    public fun emit(iv: InstructionAdapter) {
        insnOf().emit(iv)
    }
}

public class PseudoInsn(public val opcode: PseudoInsnOpcode, public val args: List<String>) {
    public val encodedMethodName: String =
            if (args.isEmpty())
                opcode.toString()
            else
                opcode.toString() + PSEUDO_INSN_PARTS_SEPARATOR + args.join(PSEUDO_INSN_PARTS_SEPARATOR)

    public fun emit(iv: InstructionAdapter) {
        iv.invokestatic(PSEUDO_INSN_CALL_OWNER, encodedMethodName, opcode.signature, false)
    }
}

public fun InstructionAdapter.fixStackAndJump(label: Label) {
    PseudoInsnOpcode.FIX_STACK_BEFORE_JUMP.emit(this)
    this.goTo(label)
}

public fun InstructionAdapter.fakeAlwaysTrueIfeq(label: Label) {
    PseudoInsnOpcode.FAKE_ALWAYS_TRUE_IFEQ.emit(this)
    this.ifeq(label)
}

public fun InstructionAdapter.fakeAlwaysFalseIfeq(label: Label) {
    PseudoInsnOpcode.FAKE_ALWAYS_FALSE_IFEQ.emit(this)
    this.ifeq(label)
}

public fun parseOrNull(insn: AbstractInsnNode): PseudoInsn? =
        if (isPseudoInsn(insn))
            parseParts(getPseudoInsnParts(insn as MethodInsnNode))
        else null

private fun isPseudoInsn(insn: AbstractInsnNode) =
        insn is MethodInsnNode && insn.getOpcode() == Opcodes.INVOKESTATIC && insn.owner == PSEUDO_INSN_CALL_OWNER

private fun getPseudoInsnParts(insn: MethodInsnNode): List<String> =
        insn.name.splitBy(PSEUDO_INSN_PARTS_SEPARATOR)

private fun parseParts(parts: List<String>): PseudoInsn? {
    try {
        return PseudoInsnOpcode.valueOf(parts[0]).insnOf(parts.subList(1, parts.size()))
    }
    catch (e: IllegalArgumentException) {
        return null
    }
}
