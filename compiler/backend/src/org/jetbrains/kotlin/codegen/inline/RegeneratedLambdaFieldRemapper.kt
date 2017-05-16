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
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode

class RegeneratedLambdaFieldRemapper(
        val oldOwnerType: String,
        override val newLambdaInternalName: String,
        private val parameters: Parameters,
        private val recapturedLambdas: Map<String, LambdaInfo>,
        remapper: FieldRemapper,
        private val isConstructor: Boolean
) : FieldRemapper(oldOwnerType, remapper, parameters) {

    public override fun canProcess(fieldOwner: String, fieldName: String, isFolding: Boolean) =
            super.canProcess(fieldOwner, fieldName, isFolding) || isRecapturedLambdaType(fieldOwner, isFolding)

    private fun isRecapturedLambdaType(owner: String, isFolding: Boolean) =
            recapturedLambdas.containsKey(owner) && (isFolding || parent !is InlinedLambdaRemapper)

    override fun findField(fieldInsnNode: FieldInsnNode, captured: Collection<CapturedParamInfo>): CapturedParamInfo? {
        val searchInParent = !canProcess(fieldInsnNode.owner, fieldInsnNode.name, false)
        if (searchInParent) {
            return parent!!.findField(fieldInsnNode)
        }
        return findFieldInMyCaptured(fieldInsnNode)
    }

    override fun processNonAload0FieldAccessChains(isInlinedLambda: Boolean): Boolean {
        return isInlinedLambda && isConstructor
    }

    private fun findFieldInMyCaptured(fieldInsnNode: FieldInsnNode): CapturedParamInfo? {
        return super.findField(fieldInsnNode, parameters.captured)
    }

    override fun getFieldForInline(node: FieldInsnNode, prefix: StackValue?): StackValue? {
        assert(node.name.startsWith("$$$")) { "Captured field template should start with $$$ prefix" }
        if (node.name == "$$$" + InlineCodegenUtil.THIS) {
            assert(oldOwnerType == node.owner) { "Can't unfold '$$\$THIS' parameter" }
            return StackValue.LOCAL_0
        }

        val fin = FieldInsnNode(node.opcode, node.owner, node.name.substring(3), node.desc)
        var field = findFieldInMyCaptured(fin)

        var searchInParent = false
        if (field == null) {
            field = findFieldInMyCaptured(FieldInsnNode(
                    Opcodes.GETSTATIC, oldOwnerType, InlineCodegenUtil.`THIS$0`,
                    Type.getObjectType(parent!!.lambdaInternalName!!).descriptor
            ))
            searchInParent = true
            if (field == null) {
                throw IllegalStateException("Couldn't find captured this " + lambdaInternalName + " for " + node.name)
            }
        }

        val result = StackValue.field(
                if (field.isSkipped)
                    Type.getObjectType(parent!!.parent!!.newLambdaInternalName)
                else
                    field.getType(),
                Type.getObjectType(newLambdaInternalName), /*TODO owner type*/
                field.newFieldName, false,
                prefix ?: StackValue.LOCAL_0
        )

        return if (searchInParent) parent!!.getFieldForInline(node, result) else result
    }
}
