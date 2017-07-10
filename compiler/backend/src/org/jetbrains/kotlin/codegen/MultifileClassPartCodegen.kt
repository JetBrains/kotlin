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

package org.jetbrains.kotlin.codegen

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.context.MultifileClassPartContext
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MultifileClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import java.util.*

class MultifileClassPartCodegen(
        v: ClassBuilder,
        file: KtFile,
        private val packageFragment: PackageFragmentDescriptor,
        private val superClassInternalName: String,
        private val shouldGeneratePartHierarchy: Boolean,
        partContext: MultifileClassPartContext,
        state: GenerationState
) : MemberCodegen<KtFile>(state, null, partContext, file, v) {
    private val partType = partContext.filePartType
    private val facadeClassType = partContext.multifileClassType
    private val staticInitClassType = Type.getObjectType(partType.internalName + STATIC_INIT_CLASS_SUFFIX)

    private val partClassAttributes =
            if (shouldGeneratePartHierarchy)
                OPEN_PART_CLASS_ATTRIBUTES
            else
                FINAL_PART_CLASS_ATTRIBUTES

    private fun ClassBuilder.newSpecialMethod(originDescriptor: DeclarationDescriptor, name: String) =
            newMethod(OtherOrigin(originDescriptor), Opcodes.ACC_STATIC, name, "()V", null, null)

    private val staticInitClassBuilder = ClassBuilderOnDemand {
        state.factory.newVisitor(MultifileClass(file, packageFragment), staticInitClassType, file).apply {
            defineClass(file, state.classFileVersion, STATE_INITIALIZER_CLASS_ATTRIBUTES,
                        staticInitClassType.internalName, null, "java/lang/Object", ArrayUtil.EMPTY_STRING_ARRAY)

            visitSource(file.name, null)
        }
    }

    private val requiresDeferredStaticInitialization =
            shouldGeneratePartHierarchy && file.declarations.any {
                it is KtProperty && shouldInitializeProperty(it)
            }

    override fun generate() {
        if (!state.classBuilderMode.generateMultiFileFacadePartClasses) return

        super.generate()

        val generateBodies = state.classBuilderMode.generateBodies

        if (shouldGeneratePartHierarchy) {
            v.newMethod(OtherOrigin(packageFragment), Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).apply {
                if (generateBodies) {
                    visitCode()
                    visitVarInsn(Opcodes.ALOAD, 0)
                    visitMethodInsn(Opcodes.INVOKESPECIAL, superClassInternalName, "<init>", "()V", false)
                    visitInsn(Opcodes.RETURN)
                    visitMaxs(1, 1)
                }
                visitEnd()
            }
        }

        if (requiresDeferredStaticInitialization) {
            staticInitClassBuilder.apply {
                newField(OtherOrigin(packageFragment), Opcodes.ACC_STATIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_VOLATILE,
                         CLINIT_SYNC_NAME, "I", null, null)

                newSpecialMethod(packageFragment, CLINIT_TRIGGER_NAME).apply {
                    if (generateBodies) {
                        visitCode()
                        visitFieldInsn(Opcodes.GETSTATIC, staticInitClassType.internalName, CLINIT_SYNC_NAME, "I")
                        visitInsn(Opcodes.RETURN)
                        visitMaxs(1, 0)
                    }
                    visitEnd()
                }

                newSpecialMethod(packageFragment, "<clinit>").apply {
                    if (generateBodies) {
                        visitCode()
                        visitMethodInsn(Opcodes.INVOKESTATIC, partType.internalName, DEFERRED_PART_CLINIT_NAME, "()V", false)
                        visitInsn(Opcodes.ICONST_0)
                        visitFieldInsn(Opcodes.PUTSTATIC, staticInitClassType.internalName, CLINIT_SYNC_NAME, "I")
                        visitInsn(Opcodes.RETURN)
                        visitMaxs(1, 0)
                    }
                    visitEnd()
                }

                writeSyntheticClassMetadata(this, state)
            }
        }
    }

    override fun generateDeclaration() {
        v.defineClass(element, state.classFileVersion, partClassAttributes, partType.internalName, null, superClassInternalName, ArrayUtil.EMPTY_STRING_ARRAY)
        v.visitSource(element.name, null)

        generatePropertyMetadataArrayFieldIfNeeded(partType)
    }

    override fun generateBody() {
        for (declaration in element.declarations) {
            if (declaration is KtNamedFunction || declaration is KtProperty || declaration is KtTypeAlias) {
                genSimpleMember(declaration)
            }
        }

        if (state.classBuilderMode.generateBodies) {
            generateInitializers { createOrGetClInitCodegen() }
        }
    }

    override fun createClInitMethodVisitor(contextDescriptor: DeclarationDescriptor): MethodVisitor =
            if (requiresDeferredStaticInitialization)
                v.newSpecialMethod(contextDescriptor, DEFERRED_PART_CLINIT_NAME)
            else
                super.createClInitMethodVisitor(contextDescriptor)

    override fun done() {
        super.done()

        if (staticInitClassBuilder.isComputed) {
            staticInitClassBuilder.done()
        }
    }

    override fun generateKotlinMetadataAnnotation() {
        val members = ArrayList<DeclarationDescriptor>()
        for (declaration in element.declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val functionDescriptor = bindingContext.get(BindingContext.FUNCTION, declaration)
                    members.add(functionDescriptor ?: throw AssertionError("Function ${declaration.name} is not bound in ${element.name}"))
                }
                is KtProperty -> {
                    val property = bindingContext.get(BindingContext.VARIABLE, declaration)
                    members.add(property ?: throw AssertionError("Property ${declaration.name} is not bound in ${element.name}"))
                }
            }
        }

        val serializer = DescriptorSerializer.createTopLevel(JvmSerializerExtension(v.serializationBindings, state))
        val packageProto = serializer.packagePartProto(packageFragment.fqName, members).build()

        val extraFlags = if (shouldGeneratePartHierarchy) JvmAnnotationNames.METADATA_MULTIFILE_PARTS_INHERIT_FLAG else 0

        writeKotlinMetadata(v, state, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART, extraFlags) { av ->
            AsmUtil.writeAnnotationData(av, serializer, packageProto)
            av.visit(JvmAnnotationNames.METADATA_MULTIFILE_CLASS_NAME_FIELD_NAME, facadeClassType.internalName)
        }
    }

    override fun generateSyntheticPartsAfterBody() {
        generateSyntheticAccessors()
    }

    override fun beforeMethodBody(mv: MethodVisitor) {
        if (requiresDeferredStaticInitialization) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, staticInitClassType.internalName, CLINIT_TRIGGER_NAME, "()V", false)
        }
    }

    companion object {
        private val OPEN_PART_CLASS_ATTRIBUTES = Opcodes.ACC_SUPER
        private val FINAL_PART_CLASS_ATTRIBUTES = Opcodes.ACC_SYNTHETIC or Opcodes.ACC_SUPER or Opcodes.ACC_FINAL
        private val STATE_INITIALIZER_CLASS_ATTRIBUTES = Opcodes.ACC_SYNTHETIC or Opcodes.ACC_SUPER or Opcodes.ACC_FINAL

        private val STATIC_INIT_CLASS_SUFFIX = "__Clinit"
        private val CLINIT_TRIGGER_NAME = "\$\$clinitTrigger"
        private val CLINIT_SYNC_NAME = "\$\$clinitSync"
        private val DEFERRED_PART_CLINIT_NAME = "\$\$clinit"

        @JvmStatic fun isStaticInitTrigger(insn: AbstractInsnNode) =
                insn.opcode == Opcodes.INVOKESTATIC
                && insn is MethodInsnNode
                && insn.owner.endsWith(STATIC_INIT_CLASS_SUFFIX)
                && insn.name == CLINIT_TRIGGER_NAME
                && insn.desc == "()V"
    }
}
