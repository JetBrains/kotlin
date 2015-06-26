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

import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.tree.*

public object LabelNormalizationMethodTransformer : MethodTransformer() {
    val newLabelNodes = hashMapOf<LabelNode, LabelNode>()
    val removedLabelNodes = hashSetOf<LabelNode>()

    public override fun transform(internalClassName: String, methodNode: MethodNode) {
        newLabelNodes.clear()
        removedLabelNodes.clear()

        with(methodNode.instructions) {
            insertBefore(getFirst(), LabelNode(Label()))
            insert(getLast(), LabelNode(Label()))
        }

        rewriteLabelInsns(methodNode)
        if (removedLabelNodes.isEmpty()) return

        rewriteInsns(methodNode)
        rewriteTryCatchBlocks(methodNode)
        rewriteLocalVars(methodNode)
    }

    private fun rewriteLabelInsns(methodNode: MethodNode) {
        var prevLabelNode: LabelNode? = null
        var thisNode = methodNode.instructions.getFirst()
        while (thisNode != null) {
            if (thisNode is LabelNode) {
                if (prevLabelNode != null) {
                    newLabelNodes[thisNode] = prevLabelNode
                    removedLabelNodes.add(thisNode)
                    thisNode = methodNode.instructions.removeNodeGetNext(thisNode)
                }
                else {
                    prevLabelNode = thisNode
                    newLabelNodes[thisNode] = thisNode
                    thisNode = thisNode.getNext()
                }
            }
            else {
                prevLabelNode = null
                thisNode = thisNode.getNext()
            }
        }
    }

    private fun rewriteInsns(methodNode: MethodNode) {
        var thisNode = methodNode.instructions.getFirst()
        while (thisNode != null) {
            thisNode = when (thisNode) {
                is JumpInsnNode ->
                    rewriteJumpInsn(methodNode, thisNode)
                is LineNumberNode ->
                    rewriteLineNumberNode(methodNode, thisNode)
                is LookupSwitchInsnNode ->
                    rewriteLookupSwitchInsn(methodNode, thisNode)
                is TableSwitchInsnNode ->
                    rewriteTableSwitchInsn(methodNode, thisNode)
                is FrameNode ->
                    rewriteFrameNode(methodNode, thisNode)
                else ->
                    thisNode.getNext()
            }
        }
    }

    private fun rewriteLineNumberNode(methodNode: MethodNode, oldLineNode: LineNumberNode): AbstractInsnNode? {
        if (isRemoved(oldLineNode.start)) {
            val newLineNode = oldLineNode.clone(newLabelNodes)
            return methodNode.instructions.replaceNodeGetNext(oldLineNode, newLineNode)
        }
        else {
            return oldLineNode.getNext()
        }
    }

    private fun rewriteJumpInsn(methodNode: MethodNode, oldJumpNode: JumpInsnNode): AbstractInsnNode? {
        if (isRemoved(oldJumpNode.label)) {
            val newJumpNode = oldJumpNode.clone(newLabelNodes)
            return methodNode.instructions.replaceNodeGetNext(oldJumpNode, newJumpNode)
        }
        else {
            return oldJumpNode.getNext()
        }
    }

    private fun rewriteLookupSwitchInsn(methodNode: MethodNode, oldSwitchNode: LookupSwitchInsnNode): AbstractInsnNode? =
            methodNode.instructions.replaceNodeGetNext(oldSwitchNode, oldSwitchNode.clone(newLabelNodes))

    private fun rewriteTableSwitchInsn(methodNode: MethodNode, oldSwitchNode: TableSwitchInsnNode): AbstractInsnNode? =
            methodNode.instructions.replaceNodeGetNext(oldSwitchNode, oldSwitchNode.clone(newLabelNodes))

    private fun rewriteFrameNode(methodNode: MethodNode, oldFrameNode: FrameNode): AbstractInsnNode? =
            methodNode.instructions.replaceNodeGetNext(oldFrameNode, oldFrameNode.clone(newLabelNodes))

    private fun rewriteTryCatchBlocks(methodNode: MethodNode) {
        methodNode.tryCatchBlocks = methodNode.tryCatchBlocks.map { oldTcb ->
            if (isRemoved(oldTcb.start) || isRemoved(oldTcb.end) || isRemoved(oldTcb.handler)) {
                val newTcb = TryCatchBlockNode(getNew(oldTcb.start), getNew(oldTcb.end), getNew(oldTcb.handler), oldTcb.type)
                newTcb.visibleTypeAnnotations = oldTcb.visibleTypeAnnotations
                newTcb.invisibleTypeAnnotations = oldTcb.invisibleTypeAnnotations
                newTcb
            }
            else {
                oldTcb
            }
        }
    }

    private fun rewriteLocalVars(methodNode: MethodNode) {
        methodNode.localVariables = methodNode.localVariables.map { oldVar ->
            if (isRemoved(oldVar.start) || isRemoved(oldVar.end)) {
                LocalVariableNode(oldVar.name, oldVar.desc, oldVar.signature, getNew(oldVar.start), getNew(oldVar.end), oldVar.index)
            }
            else {
                oldVar
            }
        }
    }

    private fun isRemoved(labelNode: LabelNode): Boolean = removedLabelNodes.contains(labelNode)
    private fun getNew(oldLabelNode: LabelNode): LabelNode = newLabelNodes[oldLabelNode]
}

private fun InsnList.replaceNodeGetNext(oldNode: AbstractInsnNode, newNode: AbstractInsnNode): AbstractInsnNode? {
    insertBefore(oldNode, newNode)
    remove(oldNode)
    return newNode.getNext()
}

private fun InsnList.removeNodeGetNext(oldNode: AbstractInsnNode): AbstractInsnNode? {
    val next = oldNode.getNext()
    remove(oldNode)
    return next
}
