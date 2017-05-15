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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

open class FieldRemapper(val lambdaInternalName: String?, @JvmField val parent: FieldRemapper?, private val params: Parameters) {

    protected open fun canProcess(fieldOwner: String, fieldName: String, isFolding: Boolean): Boolean {
        return fieldOwner == lambdaInternalName &&
               //don't process general field of anonymous objects
               InlineCodegenUtil.isCapturedFieldName(fieldName)
    }

    fun foldFieldAccessChainIfNeeded(capturedFieldAccess: List<AbstractInsnNode>, node: MethodNode): AbstractInsnNode? {
        if (capturedFieldAccess.size == 1) {
            //just aload
            return null
        }

        return foldFieldAccessChainIfNeeded(capturedFieldAccess, 1, node)
    }

    //TODO: seems that this method is redundant but it added from safety purposes before new milestone
    open fun processNonAload0FieldAccessChains(isInlinedLambda: Boolean): Boolean {
        return false
    }

    private fun foldFieldAccessChainIfNeeded(
            capturedFieldAccess: List<AbstractInsnNode>,
            currentInstruction: Int,
            node: MethodNode
    ): AbstractInsnNode? {
        val checkParent = !isRoot && currentInstruction < capturedFieldAccess.size - 1
        if (checkParent) {
            val transformed = parent!!.foldFieldAccessChainIfNeeded(capturedFieldAccess, currentInstruction + 1, node)
            if (transformed != null) {
                return transformed
            }
        }

        val insnNode = capturedFieldAccess[currentInstruction] as FieldInsnNode
        if (canProcess(insnNode.owner, insnNode.name, true)) {
            insnNode.name = "$$$" + insnNode.name
            insnNode.opcode = Opcodes.GETSTATIC

            var next = capturedFieldAccess[0]
            while (next !== insnNode) {
                val toDelete = next
                next = next.next
                node.instructions.remove(toDelete)
            }

            return capturedFieldAccess[capturedFieldAccess.size - 1]
        }

        return null
    }

    fun findField(fieldInsnNode: FieldInsnNode): CapturedParamInfo? {
        return findField(fieldInsnNode, params.captured)
    }

    open fun findField(fieldInsnNode: FieldInsnNode, captured: Collection<CapturedParamInfo>): CapturedParamInfo? {
        for (valueDescriptor in captured) {
            if (valueDescriptor.originalFieldName == fieldInsnNode.name && valueDescriptor.containingLambdaName == fieldInsnNode.owner) {
                return valueDescriptor
            }
        }
        return null
    }

    open fun getNewLambdaInternalName(): String {
        return lambdaInternalName!!
    }

    val isRoot: Boolean
        get() = parent == null

    open fun getFieldForInline(node: FieldInsnNode, prefix: StackValue?): StackValue? {
        return MethodInliner.findCapturedField(node, this).remapValue
    }

    open val isInsideInliningLambda: Boolean
        get() = !isRoot && parent!!.isInsideInliningLambda
}
