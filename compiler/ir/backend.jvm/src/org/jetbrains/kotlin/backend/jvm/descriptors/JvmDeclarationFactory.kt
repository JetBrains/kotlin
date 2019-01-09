/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.deepCopyWithWrappedDescriptors
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.ir.DeclarationFactory
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.createStaticFunctionWithReceivers
import org.jetbrains.kotlin.builtins.CompanionObjectMapping.isMappedIntrinsicCompanionObject
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

class JvmDeclarationFactory(
    private val state: GenerationState
) : DeclarationFactory {
    private val singletonFieldDeclarations = HashMap<IrSymbolOwner, IrField>()
    private val outerThisDeclarations = HashMap<IrClass, IrField>()
    private val innerClassConstructors = HashMap<IrConstructor, IrConstructor>()

    private val defaultImplsMethods = HashMap<IrSimpleFunction, IrSimpleFunction>()
    private val defaultImplsClasses = HashMap<IrClass, IrClass>()

    override fun getFieldForEnumEntry(enumEntry: IrEnumEntry, type: IrType): IrField =
        singletonFieldDeclarations.getOrPut(enumEntry) {
            val symbol = IrFieldSymbolImpl(createEnumEntryFieldDescriptor(enumEntry.descriptor))
            IrFieldImpl(
                enumEntry.startOffset,
                enumEntry.endOffset,
                IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY,
                symbol,
                type
            )
        }

    override fun getOuterThisField(innerClass: IrClass): IrField =
        if (!innerClass.isInner) throw AssertionError("Class is not inner: ${innerClass.dump()}")
        else outerThisDeclarations.getOrPut(innerClass) {
            val outerClass = innerClass.parent as? IrClass
                ?: throw AssertionError("No containing class for inner class ${innerClass.dump()}")

            val symbol = IrFieldSymbolImpl(
                JvmPropertyDescriptorImpl.createFinalField(
                    Name.identifier("this$0"), outerClass.defaultType.toKotlinType(), innerClass.descriptor,
                    Annotations.EMPTY, JavaVisibilities.PACKAGE_VISIBILITY, Opcodes.ACC_SYNTHETIC, SourceElement.NO_SOURCE
                )
            )

            IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, DeclarationFactory.FIELD_FOR_OUTER_THIS, symbol, outerClass.defaultType).also {
                it.parent = innerClass
            }
        }

    override fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructor {
        assert((innerClassConstructor.parent as IrClass).isInner) { "Class is not inner: ${(innerClassConstructor.parent as IrClass).dump()}" }

        return innerClassConstructors.getOrPut(innerClassConstructor) {
            createInnerClassConstructorWithOuterThisParameter(innerClassConstructor)
        }
    }

    private fun createInnerClassConstructorWithOuterThisParameter(oldConstructor: IrConstructor): IrConstructor {
        val newDescriptor = WrappedClassConstructorDescriptor(oldConstructor.descriptor.annotations)
        return IrConstructorImpl(
            oldConstructor.startOffset, oldConstructor.endOffset, oldConstructor.origin,
            IrConstructorSymbolImpl(newDescriptor),
            oldConstructor.name, oldConstructor.visibility, oldConstructor.returnType,
            oldConstructor.isInline, oldConstructor.isExternal, oldConstructor.isPrimary
        ).apply {
            newDescriptor.bind(this)
            annotations.addAll(oldConstructor.annotations.map { it.deepCopyWithWrappedDescriptors(this) })
            parent = oldConstructor.parent
            returnType = oldConstructor.returnType
            copyTypeParametersFrom(oldConstructor)

            val outerThisType = oldConstructor.parentAsClass.parentAsClass.defaultType
            val outerThisDescriptor = WrappedValueParameterDescriptor()
            val outerThisValueParameter = IrValueParameterImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.FIELD_FOR_OUTER_THIS,
                IrValueParameterSymbolImpl(outerThisDescriptor),
                Name.identifier("\$outer"),
                0,
                type = outerThisType,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false
            ).also {
                outerThisDescriptor.bind(it)
                it.parent = this
            }
            valueParameters.add(outerThisValueParameter)

            oldConstructor.valueParameters.mapTo(valueParameters) { it.copyTo(this, index = it.index + 1) }
            metadata = oldConstructor.metadata
        }
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

    override fun getFieldForObjectInstance(singleton: IrClass): IrField =
        singletonFieldDeclarations.getOrPut(singleton) {
            val symbol = IrFieldSymbolImpl(createObjectInstanceFieldDescriptor(singleton.descriptor))
            return IrFieldImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE,
                symbol,
                singleton.defaultType
            )
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

    fun getDefaultImplsFunction(interfaceFun: IrSimpleFunction): IrSimpleFunction {
        val parent = interfaceFun.parentAsClass
        assert(parent.isInterface) { "Parent of ${interfaceFun.dump()} should be interface" }
        return defaultImplsMethods.getOrPut(interfaceFun) {
            val defaultImpls = getDefaultImplsClass(interfaceFun.parentAsClass)

            val name = Name.identifier(state.typeMapper.mapFunctionName(interfaceFun.descriptor.original, OwnerKind.IMPLEMENTATION))
            createStaticFunctionWithReceivers(
                defaultImpls, name, interfaceFun,
                dispatchReceiverType = parent.defaultType,
                origin = JvmLoweredDeclarationOrigin.DEFAULT_IMPLS
            )
        }
    }

    fun getDefaultImplsClass(interfaceClass: IrClass): IrClass =
        defaultImplsClasses.getOrPut(interfaceClass) {
            val descriptor = WrappedClassDescriptor()
            IrClassImpl(
                interfaceClass.startOffset, interfaceClass.endOffset,
                JvmLoweredDeclarationOrigin.DEFAULT_IMPLS,
                IrClassSymbolImpl(descriptor),
                Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME),
                ClassKind.CLASS,
                Visibilities.PUBLIC,
                Modality.FINAL,
                isCompanion = false,
                isInner = false,
                isData = false,
                isExternal = false,
                isInline = false
            ).apply {
                descriptor.bind(this)
                parent = interfaceClass
                createImplicitParameterDeclarationWithWrappedDescriptor()
            }
        }
}
