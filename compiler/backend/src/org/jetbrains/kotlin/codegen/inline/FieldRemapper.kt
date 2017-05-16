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
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

open class FieldRemapper(
        val originalLambdaInternalName: String?,
        @JvmField val parent: FieldRemapper?,
        protected val parameters: Parameters
) {
    val isRoot = parent == null

    open val isInsideInliningLambda: Boolean = parent?.isInsideInliningLambda ?: false

    protected open fun canProcess(fieldOwner: String, fieldName: String, isFolding: Boolean): Boolean {
        return fieldOwner == originalLambdaInternalName &&
               //don't process general field of anonymous objects
               InlineCodegenUtil.isCapturedFieldName(fieldName)
    }

    fun foldFieldAccessChainIfNeeded(capturedFieldAccess: List<AbstractInsnNode>, node: MethodNode): AbstractInsnNode? =
            if (capturedFieldAccess.size == 1)
                null //single aload
            else
                foldFieldAccessChainIfNeeded(capturedFieldAccess, 1, node)

    //TODO: seems that this method is redundant but it added from safety purposes before new milestone
    open fun processNonAload0FieldAccessChains(isInlinedLambda: Boolean): Boolean = false

    private fun foldFieldAccessChainIfNeeded(
            capturedFieldAccess: List<AbstractInsnNode>,
            currentInstruction: Int,
            node: MethodNode
    ): AbstractInsnNode? {
        if (currentInstruction < capturedFieldAccess.lastIndex) {
            //try to fold longest chain first
            parent?.foldFieldAccessChainIfNeeded(capturedFieldAccess, currentInstruction + 1, node)?.let {
                return@foldFieldAccessChainIfNeeded it
            }
        }

        val insnNode = capturedFieldAccess[currentInstruction] as FieldInsnNode
        if (canProcess(insnNode.owner, insnNode.name, true)) {
            insnNode.name = InlineCodegenUtil.CAPTURED_FIELD_FOLD_PREFIX + getFieldNameForFolding(insnNode)
            insnNode.opcode = Opcodes.GETSTATIC

            node.remove(InsnSequence(capturedFieldAccess[0], insnNode))
            return capturedFieldAccess[capturedFieldAccess.size - 1]
        }

        return null
    }

    protected open fun getFieldNameForFolding(insnNode: FieldInsnNode): String = insnNode.name

    @JvmOverloads
    open fun findField(fieldInsnNode: FieldInsnNode, captured: Collection<CapturedParamInfo> = parameters.captured): CapturedParamInfo? {
        for (valueDescriptor in captured) {
            if (valueDescriptor.originalFieldName == fieldInsnNode.name && valueDescriptor.containingLambdaName == fieldInsnNode.owner) {
                return valueDescriptor
            }
        }
        return null
    }

    open val newLambdaInternalName: String
        get() = originalLambdaInternalName!!

    open fun getFieldForInline(node: FieldInsnNode, prefix: StackValue?): StackValue? =
            MethodInliner.findCapturedField(node, this).remapValue
}
