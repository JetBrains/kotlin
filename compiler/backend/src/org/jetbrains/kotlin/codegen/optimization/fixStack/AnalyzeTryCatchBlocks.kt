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

import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.inline.isCatchStoreInstruction
import org.jetbrains.kotlin.codegen.optimization.common.findNextOrNull
import org.jetbrains.kotlin.codegen.optimization.common.hasOpcode
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.TryCatchBlockNode
import java.util.*

private class DecompiledTryDescriptor(val tryStartLabel: LabelNode) {
    // Only used for assertions
    var defaultHandlerTcb: TryCatchBlockNode? = null
    val handlerStartLabels = hashSetOf<LabelNode>()
}

private fun TryCatchBlockNode.isDefaultHandlerNode(): Boolean =
    start == handler

private fun MethodNode.debugString(tcb: TryCatchBlockNode): String =
    "TCB<${instructions.indexOf(tcb.start)}, ${instructions.indexOf(tcb.end)}, ${instructions.indexOf(tcb.handler)}>"

internal fun insertTryCatchBlocksMarkers(methodNode: MethodNode): Map<AbstractInsnNode, AbstractInsnNode> {
    if (methodNode.tryCatchBlocks.isEmpty()) return emptyMap()

    val decompiledTryDescriptorForStart = collectDecompiledTryDescriptors(methodNode)

    val newTryStartLabels = hashMapOf<LabelNode, LabelNode>()
    val restoreStackToSaveStackMarker = insertSaveRestoreStackMarkers(decompiledTryDescriptorForStart, methodNode, newTryStartLabels)

    transformTryCatchBlocks(methodNode, newTryStartLabels)

    return restoreStackToSaveStackMarker
}

private fun transformTryCatchBlocks(methodNode: MethodNode, newTryStartLabels: HashMap<LabelNode, LabelNode>) {
    methodNode.tryCatchBlocks = methodNode.tryCatchBlocks.map { tcb ->
        val newTryStartLabel = newTryStartLabels[tcb.start]
        if (newTryStartLabel == null)
            tcb
        else
            TryCatchBlockNode(newTryStartLabel, tcb.end, tcb.handler, tcb.type)
    }
}

private fun insertSaveRestoreStackMarkers(
    decompiledTryDescriptorForStart: Map<LabelNode, DecompiledTryDescriptor>,
    methodNode: MethodNode,
    newTryStartLabels: MutableMap<LabelNode, LabelNode>
): Map<AbstractInsnNode, AbstractInsnNode> {
    val restoreStackToSaveMarker = hashMapOf<AbstractInsnNode, AbstractInsnNode>()
    val saveStackMarkerByTryLabel = hashMapOf<LabelNode, AbstractInsnNode>()
    val doneHandlerLabels = hashSetOf<LabelNode>()

    for (decompiledTryDescriptor in decompiledTryDescriptorForStart.values) {
        with(decompiledTryDescriptor) {
            val saveStackMarker: AbstractInsnNode

            if (tryStartLabel !in saveStackMarkerByTryLabel) {
                val nopNode = tryStartLabel.findNextOrNull { it.hasOpcode() }!!
                assert(nopNode.opcode == Opcodes.NOP) { "${methodNode.instructions.indexOf(nopNode)}: try block should start with NOP" }

                val newTryStartLabel = LabelNode(Label())
                newTryStartLabels[tryStartLabel] = newTryStartLabel

                saveStackMarker = PseudoInsn.SAVE_STACK_BEFORE_TRY.createInsnNode()
                val restoreStackMarker = PseudoInsn.RESTORE_STACK_IN_TRY_CATCH.createInsnNode()

                saveStackMarkerByTryLabel[tryStartLabel] = saveStackMarker
                restoreStackToSaveMarker[restoreStackMarker] = saveStackMarker

                methodNode.instructions.insertBefore(nopNode, saveStackMarker)
                methodNode.instructions.insertBefore(nopNode, newTryStartLabel)
                methodNode.instructions.insert(nopNode, restoreStackMarker)
            } else {
                saveStackMarker = saveStackMarkerByTryLabel[tryStartLabel]!!
            }

            for (handlerStartLabel in handlerStartLabels) {
                if (!doneHandlerLabels.contains(handlerStartLabel)) {
                    doneHandlerLabels.add(handlerStartLabel)

                    val firstInstruction = handlerStartLabel.findNextOrNull { it.hasOpcode() }!!
                    assert(isCatchStoreInstruction(firstInstruction)) {
                        "${methodNode.instructions.indexOf(firstInstruction)}: handler should start with ASTORE"
                    }

                    val restoreStackMarker = PseudoInsn.RESTORE_STACK_IN_TRY_CATCH.createInsnNode()
                    restoreStackToSaveMarker[restoreStackMarker] = saveStackMarker
                    methodNode.instructions.insert(firstInstruction, restoreStackMarker)
                }
            }
        }
    }

    return restoreStackToSaveMarker
}

private fun collectDecompiledTryDescriptors(methodNode: MethodNode): Map<LabelNode, DecompiledTryDescriptor> {
    val decompiledTryDescriptorForStart: MutableMap<LabelNode, DecompiledTryDescriptor> = hashMapOf()
    val decompiledTryDescriptorForHandler: MutableMap<LabelNode, DecompiledTryDescriptor> = hashMapOf()

    val defaultHandlers = methodNode.tryCatchBlocks.mapNotNullTo(SmartSet.create()) {
        if (it.isDefaultHandlerNode()) it.handler else null
    }

    for (tcb in methodNode.tryCatchBlocks) {
        if (tcb.isDefaultHandlerNode()) {
            assert(decompiledTryDescriptorForHandler.containsKey(tcb.start)) { "${methodNode.debugString(tcb)}: default handler should occur after some regular handler" }
        }

        val decompiledTryDescriptor = decompiledTryDescriptorForHandler.getOrPut(tcb.handler) {
            decompiledTryDescriptorForStart.getOrPut(tcb.start) {
                DecompiledTryDescriptor(tcb.start)
            }
        }

        with(decompiledTryDescriptor) {
            if (tcb.isDefaultHandlerNode()) {
                assert(defaultHandlerTcb == null) {
                    "${methodNode.debugString(tcb)}: default handler is already found: ${methodNode.debugString(defaultHandlerTcb!!)}"
                }

                defaultHandlerTcb = tcb
            }

            if (tcb.handler !in defaultHandlers) {
                handlerStartLabels.add(tcb.handler)
            }
        }
    }

    return decompiledTryDescriptorForStart
}
