/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * Used to detect the usage of lambdas in bytecode
 *
 * It doesn't look perfectly universal, but it passes tests
 */
class InstanceOwnerRecordingClassVisitor(
    delegateClassVisitor: ClassVisitor?,
    private val methodToUsedClassesMap: MutableMap<JvmMemberSignature.Method, MutableSet<JvmClassName>>? = null,
    private val allUsedClassesSet: MutableSet<JvmClassName>? = null,
) : ClassVisitor(Opcodes.ASM9, delegateClassVisitor) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String?>?
    ): MethodVisitor? {
        val methodSignature = name?.let { descriptor?.let { JvmMemberSignature.Method(name, descriptor) } } ?: return null

        return object : MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
                if (owner != null && opcode == Opcodes.GETSTATIC && name == "INSTANCE") {
                    val jvmClassName = JvmClassName.byInternalName(owner)
                    methodToUsedClassesMap?.getOrPut(methodSignature) { mutableSetOf() }?.add(jvmClassName)
                    allUsedClassesSet?.add(jvmClassName)
                }
                super.visitFieldInsn(opcode, owner, name, descriptor)
            }
        }
    }
}
