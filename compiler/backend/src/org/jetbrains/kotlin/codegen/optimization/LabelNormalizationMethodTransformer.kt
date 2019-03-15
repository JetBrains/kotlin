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

package org.jetbrains.kotlin.codegen.optimization

import org.jetbrains.kotlin.codegen.optimization.common.removeEmptyCatchBlocks
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.tree.*
import java.lang.IllegalStateException

class LabelNormalizationMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        TransformerForMethod(methodNode).transform()
    }

    private class TransformerForMethod(val methodNode: MethodNode) {
        val instructions = methodNode.instructions
        val newLabelNodes = hashMapOf<Label, LabelNode>()

        fun transform() {
            if (rewriteLabelInstructions()) {
                rewriteNonLabelInstructions()
                rewriteTryCatchBlocks()
                rewriteLocalVars()
                methodNode.removeEmptyCatchBlocks()
            }
        }

        private fun rewriteLabelInstructions(): Boolean {
            var removedAnyLabels = false
            var thisNode = instructions.first
            while (thisNode != null) {
                if (thisNode is LabelNode) {
                    val prevNode = thisNode.previous
                    if (prevNode is LabelNode) {
                        newLabelNodes[thisNode.label] = prevNode
                        removedAnyLabels = true
                        thisNode = instructions.removeNodeGetNext(thisNode)
                    } else {
                        newLabelNodes[thisNode.label] = thisNode
                        thisNode = thisNode.next
                    }
                } else {
                    thisNode = thisNode.next
                }
            }
            return removedAnyLabels
        }

        private fun rewriteNonLabelInstructions() {
            var thisNode = instructions.first
            while (thisNode != null) {
                thisNode = when (thisNode) {
                    is JumpInsnNode ->
                        rewriteJumpInsn(thisNode)
                    is LineNumberNode ->
                        rewriteLineNumberNode(thisNode)
                    is LookupSwitchInsnNode ->
                        rewriteLookupSwitchInsn(thisNode)
                    is TableSwitchInsnNode ->
                        rewriteTableSwitchInsn(thisNode)
                    is FrameNode ->
                        rewriteFrameNode(thisNode)
                    else ->
                        thisNode.next
                }
            }
        }

        private fun rewriteLineNumberNode(oldLineNode: LineNumberNode): AbstractInsnNode? =
            instructions.replaceNodeGetNext(oldLineNode, oldLineNode.rewriteLabels())

        private fun rewriteJumpInsn(oldJumpNode: JumpInsnNode): AbstractInsnNode? =
            instructions.replaceNodeGetNext(oldJumpNode, oldJumpNode.rewriteLabels())

        private fun rewriteLookupSwitchInsn(oldSwitchNode: LookupSwitchInsnNode): AbstractInsnNode? =
            instructions.replaceNodeGetNext(oldSwitchNode, oldSwitchNode.rewriteLabels())

        private fun rewriteTableSwitchInsn(oldSwitchNode: TableSwitchInsnNode): AbstractInsnNode? =
            instructions.replaceNodeGetNext(oldSwitchNode, oldSwitchNode.rewriteLabels())

        private fun rewriteFrameNode(oldFrameNode: FrameNode): AbstractInsnNode? =
            instructions.replaceNodeGetNext(oldFrameNode, oldFrameNode.rewriteLabels())

        private fun rewriteTryCatchBlocks() {
            methodNode.tryCatchBlocks = methodNode.tryCatchBlocks.map { oldTcb ->
                val newTcb = TryCatchBlockNode(getNew(oldTcb.start), getNew(oldTcb.end), getNew(oldTcb.handler), oldTcb.type)
                newTcb.visibleTypeAnnotations = oldTcb.visibleTypeAnnotations
                newTcb.invisibleTypeAnnotations = oldTcb.invisibleTypeAnnotations
                newTcb
            }
        }

        private fun rewriteLocalVars() {
            methodNode.localVariables = methodNode.localVariables.map { oldVar ->
                LocalVariableNode(
                    oldVar.name,
                    oldVar.desc,
                    oldVar.signature,
                    getNew(oldVar.start),
                    getNew(oldVar.end),
                    oldVar.index
                )
            }
        }

        private fun LineNumberNode.rewriteLabels(): AbstractInsnNode =
            LineNumberNode(line, getNewOrOld(start))

        private fun JumpInsnNode.rewriteLabels(): AbstractInsnNode =
            JumpInsnNode(opcode, getNew(label))

        private fun LookupSwitchInsnNode.rewriteLabels(): AbstractInsnNode {
            val switchNode = LookupSwitchInsnNode(getNew(dflt), keys.toIntArray(), emptyArray())
            switchNode.labels = labels.map { getNew(it) }
            return switchNode
        }

        private fun TableSwitchInsnNode.rewriteLabels(): AbstractInsnNode {
            val switchNode = TableSwitchInsnNode(min, max, getNew(dflt))
            switchNode.labels = labels.map { getNew(it) }
            return switchNode
        }

        private fun FrameNode.rewriteLabels(): AbstractInsnNode {
            val frameNode = FrameNode(type, 0, emptyArray(), 0, emptyArray())
            frameNode.local = local.map { if (it is LabelNode) getNewOrOld(it) else it }
            frameNode.stack = stack.map { if (it is LabelNode) getNewOrOld(it) else it }
            return frameNode
        }

        private fun getNew(oldLabelNode: LabelNode): LabelNode {
            return newLabelNodes[oldLabelNode.label]
                ?: throw IllegalStateException("Label wasn't found during iterating through instructions")
        }

        private fun getNewOrOld(oldLabelNode: LabelNode): LabelNode =
            newLabelNodes[oldLabelNode.label] ?: oldLabelNode
    }
}

fun InsnList.replaceNodeGetNext(oldNode: AbstractInsnNode, newNode: AbstractInsnNode): AbstractInsnNode? {
    insertBefore(oldNode, newNode)
    remove(oldNode)
    return newNode.next
}

fun InsnList.removeNodeGetNext(oldNode: AbstractInsnNode): AbstractInsnNode? {
    val next = oldNode.next
    remove(oldNode)
    return next
}