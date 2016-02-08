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
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue


fun MethodInliner.getLambdaIfExistsAndMarkInstructions(
        insnNode: AbstractInsnNode,
        localFrame: Frame<SourceValue>,
        insAndFrames: InstructionsAndFrames,
        toDelete: MutableSet<AbstractInsnNode>,
        processSwap: Boolean): LambdaInfo? {

    val processingInsnNode = if (processSwap && insnNode.opcode == Opcodes.SWAP) InlineCodegenUtil.getPrevMeaningful(insnNode) else insnNode

    var lambdaInfo = getLambdaIfExists(processingInsnNode)

    if (lambdaInfo == null && processingInsnNode is VarInsnNode && processingInsnNode.opcode == Opcodes.ALOAD) {
        val varIndex = processingInsnNode.`var`
        val local = localFrame.getLocal(varIndex)
        val storeIns = local.singleOrNullInsn()
        if (storeIns is VarInsnNode && storeIns.getOpcode() == Opcodes.ASTORE) {
            val frame = insAndFrames[storeIns]
            if (frame != null) {
                val topOfStack = frame.getStack(frame.stackSize - 1)
                val lambdaAload = topOfStack.singleOrNullInsn()
                if (lambdaAload != null) {
                    lambdaInfo = getLambdaIfExistsAndMarkInstructions(lambdaAload, frame, insAndFrames, toDelete, processSwap)
                    if (lambdaInfo != null) {
                        toDelete.add(storeIns)
                        toDelete.add(lambdaAload)
                    }
                }
            }
        }
    }

    if (lambdaInfo != null) {
        InsnSequence(processingInsnNode!!, insnNode).forEach { toDelete.add(it) }
        toDelete.add(insnNode)
    }

    return lambdaInfo
}

fun SourceValue.singleOrNullInsn(): AbstractInsnNode? {
    return insns.singleOrNull()
}
