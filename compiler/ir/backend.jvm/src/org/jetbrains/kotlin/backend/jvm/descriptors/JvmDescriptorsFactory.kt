/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

class JvmDescriptorsFactory(
    private val psiSourceManager: PsiSourceManager,
    private val builtIns: KotlinBuiltIns
) : DescriptorsFactory {
    private val singletonFieldDescriptors = HashMap<IrBindableSymbol<*, *>, IrFieldSymbol>()
    private val outerThisDescriptors = HashMap<IrClass, IrFieldSymbol>()
    private val innerClassConstructors = HashMap<IrConstructorSymbol, IrConstructorSymbol>()

    override fun getSymbolForEnumEntry(enumEntry: IrEnumEntrySymbol): IrFieldSymbol =
        singletonFieldDescriptors.getOrPut(enumEntry) {
            IrFieldSymbolImpl(createEnumEntryFieldDescriptor(enumEntry.descriptor))
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

    override fun getOuterThisFieldSymbol(innerClass: IrClass): IrFieldSymbol =
        if (!innerClass.isInner) throw AssertionError("Class is not inner: ${innerClass.dump()}")
        else outerThisDescriptors.getOrPut(innerClass) {
            val outerClass = innerClass.parent as? IrClass
                    ?: throw AssertionError("No containing class for inner class ${innerClass.dump()}")

            IrFieldSymbolImpl(
                JvmPropertyDescriptorImpl.createFinalField(
                    Name.identifier("this$0"), outerClass.defaultType.toKotlinType(), innerClass.descriptor,
                    Annotations.EMPTY, JavaVisibilities.PACKAGE_VISIBILITY, Opcodes.ACC_SYNTHETIC, SourceElement.NO_SOURCE
                )
            )
        }

    override fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructorSymbol {
        assert((innerClassConstructor.parent as IrClass).isInner) { "Class is not inner: ${(innerClassConstructor.parent as IrClass).dump()}" }

        return innerClassConstructors.getOrPut(innerClassConstructor.symbol) {
            createInnerClassConstructorWithOuterThisParameter(innerClassConstructor)
        }
    }

    private fun createInnerClassConstructorWithOuterThisParameter(oldConstructor: IrConstructor): IrConstructorSymbol {
        val oldDescriptor = oldConstructor.descriptor
        val classDescriptor = oldDescriptor.containingDeclaration
        val outerThisType = (classDescriptor.containingDeclaration as ClassDescriptor).defaultType

        val newDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
            classDescriptor, oldDescriptor.annotations, oldDescriptor.isPrimary, oldDescriptor.source
        )

        val outerThisValueParameter = newDescriptor.createValueParameter(0, "\$outer", outerThisType)

        val newValueParameters =
            listOf(outerThisValueParameter) +
                    oldDescriptor.valueParameters.map { it.copy(newDescriptor, it.name, it.index + 1) }
        // Call the long version of `initialize()`, because otherwise default implementation inserts
        // an unwanted `dispatchReceiverParameter`
        newDescriptor.initialize(
            oldDescriptor.extensionReceiverParameter?.copy(newDescriptor),
            null,
            oldDescriptor.typeParameters,
            newValueParameters,
            oldDescriptor.returnType,
            oldDescriptor.modality,
            oldDescriptor.visibility)
        return IrConstructorSymbolImpl(newDescriptor)
    }


    private fun createEnumEntryFieldDescriptor(enumEntryDescriptor: ClassDescriptor): PropertyDescriptor {
        assert(enumEntryDescriptor.kind == ClassKind.ENUM_ENTRY) { "Should be enum entry: $enumEntryDescriptor" }

        val enumClassDescriptor = enumEntryDescriptor.containingDeclaration as ClassDescriptor
        assert(enumClassDescriptor.kind == ClassKind.ENUM_CLASS) { "Should be enum class: $enumClassDescriptor" }

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

    override fun getSymbolForObjectInstance(singleton: IrClassSymbol): IrFieldSymbol =
        singletonFieldDescriptors.getOrPut(singleton) {
            IrFieldSymbolImpl(createObjectInstanceFieldDescriptor(singleton.descriptor))
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
