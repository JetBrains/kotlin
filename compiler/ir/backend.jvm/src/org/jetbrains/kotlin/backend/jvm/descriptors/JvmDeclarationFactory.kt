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
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.setSourceRange
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

    override fun getFieldForEnumEntry(enumEntry: IrEnumEntry, entryType: IrType): IrField =
        singletonFieldDeclarations.getOrPut(enumEntry) {
            buildField {
                setSourceRange(enumEntry)
                name = enumEntry.name
                type = enumEntry.parentAsClass.defaultType
                origin = IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY
                isFinal = true
                isStatic = true
            }.apply {
                parent = enumEntry.parent
            }
        }

    override fun getOuterThisField(innerClass: IrClass): IrField =
        outerThisDeclarations.getOrPut(innerClass) {
            assert(innerClass.isInner) { "Class is not inner: ${innerClass.dump()}" }
            buildField {
                name = Name.identifier("this$0")
                type = innerClass.parentAsClass.defaultType
                origin = DeclarationFactory.FIELD_FOR_OUTER_THIS
                visibility = JavaVisibilities.PACKAGE_VISIBILITY
                isFinal = true
            }.apply {
                parent = innerClass
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

    override fun getFieldForObjectInstance(singleton: IrClass): IrField =
        singletonFieldDeclarations.getOrPut(singleton) {
            val isNotMappedCompanion = singleton.isCompanion && !isMappedIntrinsicCompanionObject(singleton.descriptor)
            buildField {
                name = if (isNotMappedCompanion) singleton.name else Name.identifier(JvmAbi.INSTANCE_FIELD)
                type = singleton.defaultType
                origin = IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
                isFinal = true
                isStatic = true
            }.apply {
                parent = if (isNotMappedCompanion) singleton.parent else singleton
            }
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
