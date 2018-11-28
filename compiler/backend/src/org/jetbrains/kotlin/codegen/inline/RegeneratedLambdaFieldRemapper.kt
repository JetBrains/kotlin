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
import org.jetbrains.kotlin.codegen.AsmUtil.THIS
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode

class RegeneratedLambdaFieldRemapper(
        originalLambdaInternalName: String,
        override val newLambdaInternalName: String,
        parameters: Parameters,
        val recapturedLambdas: Map<String, LambdaInfo>,
        remapper: FieldRemapper,
        private val isConstructor: Boolean
) : FieldRemapper(originalLambdaInternalName, remapper, parameters) {

    public override fun canProcess(fieldOwner: String, fieldName: String, isFolding: Boolean) =
            super.canProcess(fieldOwner, fieldName, isFolding) || isRecapturedLambdaType(fieldOwner, isFolding)

    private fun isRecapturedLambdaType(owner: String, isFolding: Boolean) =
            recapturedLambdas.containsKey(owner) && (isFolding || parent !is InlinedLambdaRemapper)

    override fun findField(fieldInsnNode: FieldInsnNode, captured: Collection<CapturedParamInfo>): CapturedParamInfo? {
        val searchInParent = !canProcess(fieldInsnNode.owner, fieldInsnNode.name, false)
        if (searchInParent) {
            return parent!!.findField(fieldInsnNode)
        }
        return findFieldInSuper(fieldInsnNode)
    }

    override fun shouldProcessNonAload0FieldAccessChains(): Boolean {
        return isConstructor
    }

    private fun findFieldInSuper(fieldInsnNode: FieldInsnNode): CapturedParamInfo? {
        return super.findField(fieldInsnNode, parameters.captured)
    }

    override fun getFieldForInline(node: FieldInsnNode, prefix: StackValue?): StackValue? {
        val fieldName = node.name
        assert(fieldName.startsWith(CAPTURED_FIELD_FOLD_PREFIX)) { "Captured field template should start with $CAPTURED_FIELD_FOLD_PREFIX prefix" }
        if (fieldName == CAPTURED_FIELD_FOLD_PREFIX + THIS) {
            assert(originalLambdaInternalName == node.owner) { "Can't unfold '$CAPTURED_FIELD_FOLD_PREFIX$THIS' parameter" }
            return StackValue.LOCAL_0
        }

        val fin = FieldInsnNode(node.opcode, node.owner, fieldName.substringAfter(CAPTURED_FIELD_FOLD_PREFIX), node.desc)
        var fromParent = false
        val field = findFieldInSuper(fin) ?:
                    //search in parent
                    findFieldInSuper(FieldInsnNode(
                            Opcodes.GETSTATIC, originalLambdaInternalName, AsmUtil.CAPTURED_THIS_FIELD,
                            Type.getObjectType(parent!!.originalLambdaInternalName!!).descriptor
                    ))?.also { fromParent = true } ?:
                    throw AssertionError("Couldn't find captured this $originalLambdaInternalName for $fieldName")


        val result = StackValue.field(
                if (field.isSkipped)
                    Type.getObjectType(parent!!.parent!!.newLambdaInternalName)
                else
                    field.getType(),
                Type.getObjectType(newLambdaInternalName), /*TODO owner type*/
                field.newFieldName, false,
                prefix ?: StackValue.LOCAL_0
        )

        return if (fromParent) parent!!.getFieldForInline(node, result) else result
    }
}
