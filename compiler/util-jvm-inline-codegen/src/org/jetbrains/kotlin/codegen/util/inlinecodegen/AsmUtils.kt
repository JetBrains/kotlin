/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*

val InvokeDynamicInsnNode.isSpecBootstrapCall: Boolean
    get() = this.bsm.owner == "kotlin/jvm/specialization/BootstrapMethods" &&
            this.bsm.name == "bootstrapSpecializedGeneric"

/**
 * Generates the instruction to push the given value on the stack. (Adapted from from InstructionAdapter.)
 *
 * @param intValue the constant to be pushed on the stack.
 */
fun iconstInsnNode(intValue: Int): AbstractInsnNode =
    if (intValue >= -1 && intValue <= 5) {
        InsnNode(Opcodes.ICONST_0 + intValue)
    } else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
        IntInsnNode(Opcodes.BIPUSH, intValue)
    } else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
        IntInsnNode(Opcodes.SIPUSH, intValue)
    } else {
        LdcInsnNode(intValue)
    }

val AbstractInsnNode.intConstant: Int?
    get() = when (opcode) {
        in Opcodes.ICONST_M1..Opcodes.ICONST_5 -> opcode - Opcodes.ICONST_0
        Opcodes.BIPUSH, Opcodes.SIPUSH -> (this as IntInsnNode).operand
        Opcodes.LDC -> (this as LdcInsnNode).cst as? Int
        else -> null
    }

val MethodInsnNode.reifiedOperationKind: ReifiedOperationKind?
    get() = previous?.previous?.intConstant?.let { ReifiedOperationKind.entries.getOrNull(it) }

val MethodInsnNode.reificationArgument: ReificationArgument?
    get() {
        val prev = previous!!

        val reificationArgumentRaw = when (prev.opcode) {
            Opcodes.LDC -> (prev as LdcInsnNode).cst as String
            else -> return null
        }

        val arrayDepth = reificationArgumentRaw.indexOfFirst { it != '[' }
        val parameterName = reificationArgumentRaw.substring(arrayDepth).removeSuffix("?")
        val nullable = reificationArgumentRaw.endsWith('?')

        return ReificationArgument(parameterName, nullable, arrayDepth)
    }

inline fun AbstractInsnNode.findPreviousOrNull(predicate: (AbstractInsnNode) -> Boolean): AbstractInsnNode? {
    var finger = this.previous
    while (finger != null && !predicate(finger)) {
        finger = finger.previous
    }
    return finger
}
