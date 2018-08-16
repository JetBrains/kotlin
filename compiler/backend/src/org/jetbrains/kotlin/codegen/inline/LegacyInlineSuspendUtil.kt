/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.inline

import jdk.internal.org.objectweb.asm.Opcodes
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.getContainingKotlinJvmBinaryClass
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

// KLUDGE: Inline suspend function built with compiler version less than 1.1.4/1.2-M1 did not contain proper
// before/after suspension point marks, so we detect those functions here and insert the corresponding marks

fun insertLegacySuspendInlineMarks(node: MethodNode) {
    with (node.instructions) {
        // look for return instruction before the end and insert "afterSuspendMarker" there
        insertBefore(findLastReturn(last) ?: return, produceSuspendMarker(false).instructions)
        // insert "beforeSuspendMarker" at the beginning
        insertBefore(first, produceSuspendMarker(true).instructions)
    }
    node.maxStack = node.maxStack.coerceAtLeast(2) // min stack need for suspend marker before return
}

fun findLastReturn(node: AbstractInsnNode?): AbstractInsnNode? {
    var cur = node
    while (cur != null && cur.opcode != Opcodes.ARETURN) cur = cur.previous
    return cur
}

private fun produceSuspendMarker(isStartNotEnd: Boolean): MethodNode =
    MethodNode().also { addSuspendMarker(InstructionAdapter(it), isStartNotEnd) }

fun isLegacySuspendInlineFunction(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor !is FunctionDescriptor) return false
    if (!descriptor.isSuspend || !descriptor.isInline) return false
    val jvmBytecodeVersion = descriptor.getContainingKotlinJvmBinaryClass()?.classHeader?.bytecodeVersion ?: return false
    return !jvmBytecodeVersion.isAtLeast(1, 0, 2)
}
