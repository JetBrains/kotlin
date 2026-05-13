/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.specialization

import org.jetbrains.kotlin.codegen.util.inlinecodegen.isSpecBootstrapCall
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import java.util.IdentityHashMap

internal class AdjustSpecializedCallsInterpreter : BasicInterpreter(API_VERSION) {
    val specializedCalls = IdentityHashMap<InvokeDynamicInsnNode, SpecializedCall>()

    override fun naryOperation(
        insn: AbstractInsnNode,
        values: List<BasicValue>,
    ): BasicValue? {
        if (insn is MethodInsnNode && insn.isSpecializedArgumentMarker) {
            return SpecializedArgumentValue(insn)
        }

        if (insn is InvokeDynamicInsnNode && insn.isSpecBootstrapCall) {
            specializedCalls[insn] = SpecializedCall(insn, values)
        }

        return super.naryOperation(insn, values)
    }
}

internal val MethodInsnNode.isSpecializedArgumentMarker: Boolean
    get() = opcode == Opcodes.INVOKESTATIC &&
            owner == "kotlin/jvm/internal/Intrinsics" &&
            name == "specializedArgumentMarker"

internal data class SpecializedArgumentValue(val insn: MethodInsnNode) : BasicValue(Type.getReturnType(insn.desc))

internal data class SpecializedCall(val insn: InvokeDynamicInsnNode, val args: List<BasicValue>)
