/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * [ClassVisitor] which visits only members satisfying the given criteria (`[shouldVisitField] == true` or `[shouldVisitMethod] == true`).
 */
class SelectiveClassVisitor(
    cv: ClassVisitor,
    private val shouldVisitField: (JvmMemberSignature.Field, isPrivate: Boolean, isConstant: Boolean) -> Boolean,
    private val shouldVisitMethod: (JvmMemberSignature.Method, isPrivate: Boolean) -> Boolean,
) : ClassVisitor(Opcodes.API_VERSION, cv) {

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
        // Note: A constant's value must be not-null. A static final field with a `null` value at the bytecode declaration is not a constant
        // (whether the value is initialized later in the static initializer or not, it won't be inlined by the compiler).
        val isConstant = access.isStaticFinal() && value != null

        return if (shouldVisitField(JvmMemberSignature.Field(name, desc), access.isPrivate(), isConstant)) {
            cv.visitField(access, name, desc, signature, value)
        } else null
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return if (shouldVisitMethod(JvmMemberSignature.Method(name, desc), access.isPrivate())) {
            cv.visitMethod(access, name, desc, signature, exceptions)
        } else null
    }

    private fun Int.isPrivate() = (this and Opcodes.ACC_PRIVATE) != 0

    private fun Int.isStaticFinal() = (this and (Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)
}
