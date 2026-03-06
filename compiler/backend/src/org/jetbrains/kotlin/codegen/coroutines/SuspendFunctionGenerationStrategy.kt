/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.inline.preprocessSuspendMarkers
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodNode

// For named suspend function we generate two methods:
// 1) to use as noinline function, which have state machine
// 2) to use from inliner: private one without state machine
class SuspendForInlineCopyingMethodVisitor(
    delegate: MethodVisitor, access: Int, name: String, desc: String,
    private val newMethod: (JvmDeclarationOrigin, Int, String, String, String?, Array<String>?) -> MethodVisitor,
    private val keepAccess: Boolean = true
) : TransformationMethodVisitor(delegate, access, name, desc, null, null) {
    override fun performTransformations(methodNode: MethodNode) {
        val newMethodNode = with(methodNode) {
            val newAccess = if (keepAccess) access else
                access or Opcodes.ACC_PRIVATE and Opcodes.ACC_PUBLIC.inv() and Opcodes.ACC_PROTECTED.inv()
            MethodNode(newAccess, name + FOR_INLINE_SUFFIX, desc, signature, exceptions.toTypedArray())
        }
        val newMethodVisitor = with(newMethodNode) {
            newMethod(JvmDeclarationOrigin.NO_ORIGIN, access, name, desc, signature, exceptions.toTypedArray())
        }
        methodNode.instructions.resetLabels()
        methodNode.accept(newMethodNode)
        methodNode.preprocessSuspendMarkers(forInline = false, keepFakeContinuation = false)
        newMethodNode.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = true)
        newMethodNode.accept(newMethodVisitor)
    }
}
