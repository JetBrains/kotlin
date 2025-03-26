/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.inline.findVirtualFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.Method

fun classFileContainsMethod(classId: ClassId, state: GenerationState, method: Method): Boolean? {
    val bytes = findVirtualFile(state, classId)?.contentsToByteArray() ?: return null
    var found = false
    ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            if (name == method.name && descriptor == method.descriptor) {
                found = true
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }, ClassReader.SKIP_FRAMES)
    return found
}
