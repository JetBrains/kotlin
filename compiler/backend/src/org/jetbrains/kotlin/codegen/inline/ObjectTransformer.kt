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

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

abstract class ObjectTransformer<out T : TransformationInfo>(@JvmField val transformationInfo: T, val state: GenerationState) {

    abstract fun doTransform(parentRemapper: FieldRemapper): InlineResult

    @JvmField
    protected val transformationResult = InlineResult.create()

    protected fun createRemappingClassBuilderViaFactory(inliningContext: InliningContext): ClassBuilder {
        val classBuilder = state.factory.newVisitor(
                JvmDeclarationOrigin.NO_ORIGIN,
                Type.getObjectType(transformationInfo.newClassName),
                inliningContext.root.callElement.containingFile
        )

        return RemappingClassBuilder(
                classBuilder,
                AsmTypeRemapper(inliningContext.typeRemapper, inliningContext.root.typeParameterMappings == null, transformationResult)
        )
    }

    fun createClassReader(): ClassReader {
        return InlineCodegenUtil.buildClassReaderByInternalName(state, transformationInfo.oldClassName)
    }
}

class WhenMappingTransformer(
        whenObjectRegenerationInfo: WhenMappingTransformationInfo,
        val inliningContext: InliningContext
) : ObjectTransformer<WhenMappingTransformationInfo>(whenObjectRegenerationInfo, inliningContext.state) {

    override fun doTransform(parentRemapper: FieldRemapper): InlineResult {
        val classReader = createClassReader()
        //TODO add additional check that class is when mapping

        val classBuilder = createRemappingClassBuilderViaFactory(inliningContext)
        /*MAPPING File could contains mappings for several enum classes, we should filter one*/
        val methodNodes = arrayListOf<MethodNode>()
        val fieldNode = transformationInfo.fieldNode
        classReader.accept(object : ClassVisitor(InlineCodegenUtil.API, classBuilder.visitor) {
            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<String>) {
                InlineCodegenUtil.assertVersionNotGreaterThanGeneratedOne(version, name, state)
                classBuilder.defineClass(null, version, access, name, signature, superName, interfaces)
            }

            override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                return if (name == fieldNode.name) {
                    classBuilder.newField(JvmDeclarationOrigin.NO_ORIGIN, access, name, desc, signature, value)
                }
                else {
                    null
                }
            }

            override fun visitMethod(
                    access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
            ): MethodVisitor? {
                return MethodNode(access, name, desc, signature, exceptions).apply {
                    methodNodes.add(this)
                }
            }
        }, ClassReader.SKIP_FRAMES)

        assert(methodNodes.size == 1) {
            "When mapping ${fieldNode.owner} class should contain only one method but: " + methodNodes.joinToString { it.name }
        }
        val clinit = methodNodes.first()
        assert(clinit.name == "<clinit>", { "When mapping should contains only <clinit> method, but contains '${clinit.name}'" })

        val transformedClinit = cutOtherMappings(clinit)
        val result = classBuilder.newMethod(
                JvmDeclarationOrigin.NO_ORIGIN, transformedClinit.access, transformedClinit.name, transformedClinit.desc,
                transformedClinit.signature, transformedClinit.exceptions.toTypedArray()
        )
        transformedClinit.accept(result)
        classBuilder.done()

        return transformationResult
    }

    private fun cutOtherMappings(node: MethodNode): MethodNode {
        val myArrayAccess = InsnSequence(node.instructions).first {
            it is FieldInsnNode && it.name.equals(transformationInfo.fieldNode.name)
        }

        val myValuesAccess = generateSequence(myArrayAccess) { it.previous }.first {
            isValues(it)
        }

        val nextValuesAccessOrEnd = generateSequence(myArrayAccess) { it.next }.first {
            isValues(it) || it.opcode == Opcodes.RETURN
        }

        val result = MethodNode(node.access, node.name, node.desc, node.signature, node.exceptions.toTypedArray())
        InsnSequence(myValuesAccess, nextValuesAccessOrEnd).forEach { it.accept(result) }
        result.visitInsn(Opcodes.RETURN)

        return result
    }

    private fun isValues(node: AbstractInsnNode) =
            node is MethodInsnNode &&
            node.opcode == Opcodes.INVOKESTATIC &&
            node.name == "values" &&
            node.desc == "()[" + Type.getObjectType(node.owner).descriptor
}
