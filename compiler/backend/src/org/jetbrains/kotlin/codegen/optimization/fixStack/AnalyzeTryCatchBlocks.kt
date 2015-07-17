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

package org.jetbrains.kotlin.codegen.optimization.fixStack

import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes
import org.jetbrains.kotlin.codegen.optimization.common.findNextOrNull
import org.jetbrains.kotlin.codegen.optimization.common.hasOpcode
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.Printer

private class DecompiledTryDescriptor(val tryStartLabel: LabelNode) {
    var defaultHandlerTcb : TryCatchBlockNode? = null
    val handlerStartLabels = hashSetOf<LabelNode>()
}

private fun TryCatchBlockNode.isDefaultHandlerNode(): Boolean =
        start == handler

private fun MethodNode.debugString(tcb: TryCatchBlockNode): String =
        "TCB<${instructions.indexOf(tcb.start)}, ${instructions.indexOf(tcb.end)}, ${instructions.indexOf(tcb.handler)}>"

internal fun insertTryCatchBlocksMarkers(methodNode: MethodNode) {
    if (methodNode.tryCatchBlocks.isEmpty()) return

    val decompiledTryDescriptorForStart = linkedMapOf<LabelNode, DecompiledTryDescriptor>()
    val decompiledTryDescriptorForHandler = hashMapOf<LabelNode, DecompiledTryDescriptor>()

    for (tcb in methodNode.tryCatchBlocks) {
        if (tcb.isDefaultHandlerNode()) {
            assert(decompiledTryDescriptorForHandler.containsKey(tcb.start),
                   "${methodNode.debugString(tcb)}: default handler should occur after some regular handler")
        }

        val decompiledTryDescriptor = decompiledTryDescriptorForHandler.getOrPut(tcb.handler) {
            decompiledTryDescriptorForStart.getOrPut(tcb.start) {
                DecompiledTryDescriptor(tcb.start)
            }
        }
        with(decompiledTryDescriptor) {
            handlerStartLabels.add(tcb.handler)

            if (tcb.isDefaultHandlerNode()) {
                assert(defaultHandlerTcb == null) {
                    "${methodNode.debugString(tcb)}: default handler is already found: ${methodNode.debugString(defaultHandlerTcb!!)}"
                }

                defaultHandlerTcb = tcb
            }
        }
    }

    val doneTryStartLabels = hashSetOf<LabelNode>()
    val doneHandlerLabels = hashSetOf<LabelNode>()

    for (decompiledTryDescriptor in decompiledTryDescriptorForStart.values()) {
        with(decompiledTryDescriptor) {
            if (!doneTryStartLabels.contains(tryStartLabel)) {
                doneTryStartLabels.add(tryStartLabel)

                val nopNode = tryStartLabel.findNextOrNull { it.hasOpcode() }!!
                assert(nopNode.getOpcode() == Opcodes.NOP,
                       "${methodNode.instructions.indexOf(nopNode)}: try block should start with NOP")

                methodNode.instructions.insertBefore(tryStartLabel, PseudoInsn.SAVE_STACK_BEFORE_TRY.createInsnNode())
                methodNode.instructions.insert(nopNode, PseudoInsn.RESTORE_STACK_IN_TRY_CATCH.createInsnNode())
            }

            for (handlerStartLabel in handlerStartLabels) {
                if (!doneHandlerLabels.contains(handlerStartLabel)) {
                    doneHandlerLabels.add(handlerStartLabel)

                    val storeNode = handlerStartLabel.findNextOrNull { it.hasOpcode() }!!
                    assert(storeNode.getOpcode() == Opcodes.ASTORE,
                           "${methodNode.instructions.indexOf(storeNode)}: handler should start with ASTORE")

                    methodNode.instructions.insert(storeNode, PseudoInsn.RESTORE_STACK_IN_TRY_CATCH.createInsnNode())
                }
            }
        }
    }
}