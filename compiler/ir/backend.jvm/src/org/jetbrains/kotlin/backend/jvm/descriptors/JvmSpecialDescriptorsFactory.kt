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

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.descriptors.DescriptorsFactory
import org.jetbrains.kotlin.builtins.CompanionObjectMapping.isMappedIntrinsicCompanionObject
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.descriptors.FileClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

class JvmSpecialDescriptorsFactory(
        private val psiSourceManager: PsiSourceManager,
        private val builtIns: KotlinBuiltIns
) : DescriptorsFactory {
    private val singletonFieldDescriptors = HashMap<ClassDescriptor, PropertyDescriptor>()
    private val outerThisDescriptors = HashMap<ClassDescriptor, PropertyDescriptor>()
    private val innerClassConstructors = HashMap<ClassConstructorDescriptor, ClassConstructorDescriptor>()

    override fun getFieldDescriptorForEnumEntry(enumEntryDescriptor: ClassDescriptor): PropertyDescriptor =
            singletonFieldDescriptors.getOrPut(enumEntryDescriptor) {
                createEnumEntryFieldDescriptor(enumEntryDescriptor)
            }

    fun createFileClassDescriptor(fileEntry: SourceManager.FileEntry, packageFragment: PackageFragmentDescriptor): FileClassDescriptor {
        val ktFile = psiSourceManager.getKtFile(fileEntry as PsiSourceManager.PsiFileEntry)
                   ?: throw AssertionError("Unexpected file entry: $fileEntry")
        val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(ktFile)
        val sourceElement = KotlinSourceElement(ktFile)
        return FileClassDescriptorImpl(
                fileClassInfo.fileClassFqName.shortName(), packageFragment,
                listOf(builtIns.anyType),
                sourceElement,
                Annotations.EMPTY // TODO file annotations
        )
    }

    override fun getOuterThisFieldDescriptor(innerClassDescriptor: ClassDescriptor): PropertyDescriptor =
            if (!innerClassDescriptor.isInner) throw AssertionError("Class is not inner: $innerClassDescriptor")
            else outerThisDescriptors.getOrPut(innerClassDescriptor) {
                val outerClassDescriptor = DescriptorUtils.getContainingClass(innerClassDescriptor) ?:
                                           throw AssertionError("No containing class for inner class $innerClassDescriptor")

                JvmPropertyDescriptorImpl.createFinalField(
                        Name.identifier("this$0"), outerClassDescriptor.defaultType, innerClassDescriptor,
                        Annotations.EMPTY, JavaVisibilities.PACKAGE_VISIBILITY, Opcodes.ACC_SYNTHETIC, SourceElement.NO_SOURCE
                )
            }

    override fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: ClassConstructorDescriptor): ClassConstructorDescriptor {
        val innerClass = innerClassConstructor.containingDeclaration
        assert(innerClass.isInner) { "Class is not inner: $innerClass" }

        return innerClassConstructors.getOrPut(innerClassConstructor) {
            createInnerClassConstructorWithOuterThisParameter(innerClassConstructor)
        }
    }

    private fun createInnerClassConstructorWithOuterThisParameter(oldDescriptor: ClassConstructorDescriptor): ClassConstructorDescriptor {
        val classDescriptor = oldDescriptor.containingDeclaration
        val outerThisType = (classDescriptor.containingDeclaration as ClassDescriptor).defaultType

        val newDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
                classDescriptor, oldDescriptor.annotations, oldDescriptor.isPrimary, oldDescriptor.source
        )

        val outerThisValueParameter = newDescriptor.createValueParameter(0, "\$outer", outerThisType)

        val newValueParameters =
                listOf(outerThisValueParameter) +
                oldDescriptor.valueParameters.map { it.copy(newDescriptor, it.name, it.index + 1) }
        newDescriptor.initialize(newValueParameters, oldDescriptor.visibility)
        newDescriptor.returnType = oldDescriptor.returnType
        return newDescriptor
    }



    private fun createEnumEntryFieldDescriptor(enumEntryDescriptor: ClassDescriptor): PropertyDescriptor {
        assert(enumEntryDescriptor.kind == ClassKind.ENUM_ENTRY) { "Should be enum entry: $enumEntryDescriptor" }

        val enumClassDescriptor = enumEntryDescriptor.containingDeclaration as ClassDescriptor
        assert(enumClassDescriptor.kind == ClassKind.ENUM_CLASS) { "Should be enum class: $enumClassDescriptor"}

        return JvmPropertyDescriptorImpl.createStaticVal(
                enumEntryDescriptor.name,
                enumClassDescriptor.defaultType,
                enumClassDescriptor,
                enumEntryDescriptor.annotations,
                Modality.FINAL,
                Visibilities.PUBLIC,
                Opcodes.ACC_ENUM,
                enumEntryDescriptor.source
        )
    }

    override fun getFieldDescriptorForObjectInstance(objectDescriptor: ClassDescriptor): PropertyDescriptor =
            singletonFieldDescriptors.getOrPut(objectDescriptor) {
                createObjectInstanceFieldDescriptor(objectDescriptor)
            }

    private fun createObjectInstanceFieldDescriptor(objectDescriptor: ClassDescriptor): PropertyDescriptor {
        assert(objectDescriptor.kind == ClassKind.OBJECT) { "Should be an object: $objectDescriptor" }

        val isNotMappedCompanion = objectDescriptor.isCompanionObject && !isMappedIntrinsicCompanionObject(objectDescriptor)
        val name = if (isNotMappedCompanion) objectDescriptor.name else Name.identifier("INSTANCE")
        val containingDeclaration = if (isNotMappedCompanion) objectDescriptor.containingDeclaration else objectDescriptor
        return PropertyDescriptorImpl.create(
                containingDeclaration,
                Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC, false,
                name,
                CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, /* lateInit = */ false, /* isConst = */ false,
                /* isExpect = */ false, /* isActual = */ false, /* isExternal = */ false, /* isDelegated = */ false
        ).initialize(objectDescriptor.defaultType)
    }
}
