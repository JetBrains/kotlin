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

import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue

fun MethodInliner.getLambdaIfExistsAndMarkInstructions(
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
            val topOfStack = frame.top()!!.singleOrNullInsn()
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
        val dispatchReceiver = swapFrame.top()!!.singleOrNullInsn()
        getLambdaIfExistsAndMarkInstructions(dispatchReceiver, false, insnList, frames, toDelete)?.let {
            //remove swap instruction (dispatch receiver would be deleted on recursion call): see 'complexStack/simpleExtension.1.kt' test
            toDelete.add(insnNode)
            return it
        }
    }

    return null
}

fun SourceValue.singleOrNullInsn() = insns.singleOrNull()
