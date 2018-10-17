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

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode

class InlinedLambdaRemapper(
        originalLambdaInternalName: String,
        parent: FieldRemapper,
        methodParams: Parameters,
        private val isDefaultBoundCallableReference: Boolean
) : FieldRemapper(originalLambdaInternalName, parent, methodParams) {

    public override fun canProcess(fieldOwner: String, fieldName: String, isFolding: Boolean) =
            isFolding && (isMyBoundReceiverForDefaultLambda(fieldOwner, fieldName) || super.canProcess(fieldOwner, fieldName, true))

    private fun isMyBoundReceiverForDefaultLambda(fieldOwner: String, fieldName: String) =
            isDefaultBoundCallableReference && fieldName == AsmUtil.BOUND_REFERENCE_RECEIVER && fieldOwner == originalLambdaInternalName

    override fun getFieldNameForFolding(insnNode: FieldInsnNode): String =
            if (isMyBoundReceiverForDefaultLambda(insnNode.owner, insnNode.name)) AsmUtil.RECEIVER_PARAMETER_NAME else insnNode.name

    override fun findField(fieldInsnNode: FieldInsnNode, captured: Collection<CapturedParamInfo>) =
            parent!!.findField(fieldInsnNode, captured)

    override val isInsideInliningLambda: Boolean = true

    override fun getFieldForInline(node: FieldInsnNode, prefix: StackValue?) =
            if (parent!!.isRoot)
                super.getFieldForInline(node, prefix)
            else
                parent.getFieldForInline(node, prefix)
}
