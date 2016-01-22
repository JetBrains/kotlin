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

val PSEUDO_INSN_CALL_OWNER: String = "kotlin/jvm/internal/\$PseudoInsn"

enum class PseudoInsn(val signature: String = "()V") {
    FIX_STACK_BEFORE_JUMP(),
    FAKE_ALWAYS_TRUE_IFEQ("()I"),
    FAKE_ALWAYS_FALSE_IFEQ("()I"),
    SAVE_STACK_BEFORE_TRY(),
    RESTORE_STACK_IN_TRY_CATCH()
    ;

    fun emit(iv: InstructionAdapter) {
        iv.invokestatic(PSEUDO_INSN_CALL_OWNER, toString(), signature, false)
    }

    fun createInsnNode(): MethodInsnNode =
            MethodInsnNode(Opcodes.INVOKESTATIC, PSEUDO_INSN_CALL_OWNER, toString(), signature, false)

    fun isa(node: AbstractInsnNode): Boolean =
            this == parsePseudoInsnOrNull(node)
}

fun isPseudoInsn(insn: AbstractInsnNode): Boolean =
        insn is MethodInsnNode && insn.getOpcode() == Opcodes.INVOKESTATIC && insn.owner == PSEUDO_INSN_CALL_OWNER

fun parsePseudoInsnOrNull(insn: AbstractInsnNode): PseudoInsn? =
        if (isPseudoInsn(insn))
            PseudoInsn.valueOf((insn as MethodInsnNode).name)
        else null

fun InstructionAdapter.fixStackAndJump(label: Label) {
    PseudoInsn.FIX_STACK_BEFORE_JUMP.emit(this)
    this.goTo(label)
}

fun InstructionAdapter.fakeAlwaysTrueIfeq(label: Label) {
    PseudoInsn.FAKE_ALWAYS_TRUE_IFEQ.emit(this)
    this.ifeq(label)
}

fun InstructionAdapter.fakeAlwaysFalseIfeq(label: Label) {
    PseudoInsn.FAKE_ALWAYS_FALSE_IFEQ.emit(this)
    this.ifeq(label)
}
