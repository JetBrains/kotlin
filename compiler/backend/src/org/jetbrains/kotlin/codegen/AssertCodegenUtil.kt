/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.optimization.common.findPreviousOrNull
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

const val ASSERTIONS_DISABLED_FIELD_NAME = "\$assertionsDisabled"

fun FieldInsnNode.isCheckAssertionsStatus() =
    opcode == Opcodes.GETSTATIC && name == ASSERTIONS_DISABLED_FIELD_NAME && desc == Type.BOOLEAN_TYPE.descriptor

fun generateAssertionsDisabledFieldInitialization(classBuilder: ClassBuilder, clInitBuilder: MethodVisitor, className: String) {
    classBuilder.newField(
        JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC, ASSERTIONS_DISABLED_FIELD_NAME,
        "Z", null, null
    )
    val thenLabel = Label()
    val elseLabel = Label()
    with(InstructionAdapter(clInitBuilder)) {
        mark(Label())
        aconst(Type.getObjectType(className))
        invokevirtual("java/lang/Class", "desiredAssertionStatus", "()Z", false)
        ifne(thenLabel)
        iconst(1)
        goTo(elseLabel)

        mark(thenLabel)
        iconst(0)

        mark(elseLabel)
        putstatic(classBuilder.thisName, ASSERTIONS_DISABLED_FIELD_NAME, "Z")
    }
}

fun rewriteAssertionsDisabledFieldInitialization(methodNode: MethodNode, className: String) {
    val node = InsnSequence(methodNode.instructions).firstOrNull {
        it is FieldInsnNode && it.opcode == Opcodes.PUTSTATIC && it.name == ASSERTIONS_DISABLED_FIELD_NAME
    }?.findPreviousOrNull {
        it is MethodInsnNode && it.opcode == Opcodes.INVOKEVIRTUAL
                && it.owner == "java/lang/Class" && it.name == "desiredAssertionStatus" && it.desc == "()Z"
    }?.previous
    (node as? LdcInsnNode)?.cst = Type.getObjectType(className)
}
