/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue

fun MethodInliner.getLambdaIfExistsAndMarkInstructions(
        sourceValue: SourceValue,
        processSwap: Boolean,
        insnList: InsnList,
        frames: Array<Frame<SourceValue>?>,
        toDelete: MutableSet<AbstractInsnNode>
): LambdaInfo? {
    val toDeleteInner = SmartSet.create<AbstractInsnNode>()

    val lambdaSet = SmartSet.create<LambdaInfo?>()
    sourceValue.insns.mapTo(lambdaSet) {
        getLambdaIfExistsAndMarkInstructions(it, processSwap, insnList, frames, toDeleteInner)
    }

    return lambdaSet.singleOrNull()?.also {
        toDelete.addAll(toDeleteInner)
    }
}

private fun MethodInliner.getLambdaIfExistsAndMarkInstructions(
        insnNode: AbstractInsnNode?,
        processSwap: Boolean,
        insnList: InsnList,
        frames: Array<Frame<SourceValue>?>,
        toDelete: MutableSet<AbstractInsnNode>
): LambdaInfo? {
    if (insnNode == null) return null

    getLambdaIfExists(insnNode)?.let {
        //delete lambda aload instruction
        toDelete.add(insnNode)
        return it
    }

    if (insnNode is VarInsnNode && insnNode.opcode == Opcodes.ALOAD) {
        val varIndex = insnNode.`var`
        val localFrame = frames[insnList.indexOf(insnNode)] ?: return null
        val storeIns = localFrame.getLocal(varIndex).singleOrNullInsn()
        if (storeIns is VarInsnNode && storeIns.getOpcode() == Opcodes.ASTORE) {
            val frame = frames[insnList.indexOf(storeIns)] ?: return null
            val topOfStack = frame.top()!!
            getLambdaIfExistsAndMarkInstructions(topOfStack, processSwap, insnList, frames, toDelete)?.let {
                //remove intermediate lambda astore, aload instruction: see 'complexStack/simple.1.kt' test
                toDelete.add(storeIns)
                toDelete.add(insnNode)
                return it
            }
        }
    }
    else if (processSwap && insnNode.opcode == Opcodes.SWAP) {
        val swapFrame = frames[insnList.indexOf(insnNode)] ?: return null
        val dispatchReceiver = swapFrame.top()!!
        getLambdaIfExistsAndMarkInstructions(dispatchReceiver, false, insnList, frames, toDelete)?.let {
            //remove swap instruction (dispatch receiver would be deleted on recursion call): see 'complexStack/simpleExtension.1.kt' test
            toDelete.add(insnNode)
            return it
        }
    }

    return null
}

fun SourceValue.singleOrNullInsn() = insns.singleOrNull()

fun expandMaskConditionsAndUpdateVariableNodes(node: MethodNode, maskStartIndex: Int, masks: List<Int>, methodHandlerIndex: Int) {
    class Condition(mask: Int, constant: Int, val maskInstruction: VarInsnNode, val jumpInstruction: JumpInsnNode, val varIndex: Int) {
        val expandNotDelete = mask and constant != 0
    }
    fun isMaskIndex(varIndex: Int): Boolean {
        return maskStartIndex <= varIndex && varIndex < maskStartIndex + masks.size
    }

    val maskProcessingHeader = node.instructions.asSequence().takeWhile {
        if (it is VarInsnNode) {
            if (isMaskIndex(it.`var`)) {
                /*if slot for default mask is updated than we occurred in actual function body*/
                return@takeWhile it.opcode == Opcodes.ILOAD
            }
            else if (isMethodHandleIndex(methodHandlerIndex, it)) {
                return@takeWhile it.opcode == Opcodes.ALOAD
            }
        }
        true
    }

    val conditions = maskProcessingHeader.filterIsInstance<VarInsnNode>().mapNotNull {
        if (isMaskIndex(it.`var`) &&
            it.next?.next?.opcode == Opcodes.IAND &&
            it.next.next.next?.opcode == Opcodes.IFEQ) {
            val jumpInstruction = it.next?.next?.next as JumpInsnNode
            Condition(
                    masks[it.`var` - maskStartIndex],
                    InlineCodegenUtil.getConstant(it.next),
                    it,
                    jumpInstruction,
                    (jumpInstruction.label.previous as VarInsnNode).`var`
            )
        }
        else if (isMethodHandleIndex(methodHandlerIndex, it) &&
                 it.next?.opcode == Opcodes.IFNULL &&
                 it.next.next?.opcode == Opcodes.NEW) {
            //Always delete method handle for now
            //This logic should be updated when method handles would be supported
            Condition(0, 0, it,it.next as JumpInsnNode, -1)
        }
        else null
    }

    val indexToVarNode = node.localVariables?.filter { it.index < maskStartIndex }?.associateBy { it.index } ?: emptyMap()
    val toDelete = linkedSetOf<AbstractInsnNode>()
    conditions.forEach {
        val jumpInstruction = it.jumpInstruction
        InsnSequence(it.maskInstruction, (if (it.expandNotDelete) jumpInstruction.next else jumpInstruction.label)).forEach {
            toDelete.add(it)
        }
        if (it.expandNotDelete) {
            indexToVarNode[it.varIndex]?.let { varNode ->
               varNode.start = it.jumpInstruction.label
            }
        }
    }

    node.localVariables.removeIf { it.start in toDelete && it.end in toDelete }

    toDelete.forEach {
        node.instructions.remove(it)
    }
}

private fun isMethodHandleIndex(methodHandlerIndex: Int, it: VarInsnNode) = methodHandlerIndex == it.`var`