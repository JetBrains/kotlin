/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.DescriptorsToIrRemapper
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.MethodSignatureMapper
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.builtins.CompanionObjectMapping.isMappedIntrinsicCompanionObject
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import java.util.*

class JvmDeclarationFactory(
    private val methodSignatureMapper: MethodSignatureMapper
) : DeclarationFactory {
    private val singletonFieldDeclarations = HashMap<IrSymbolOwner, IrField>()
    private val interfaceCompanionFieldDeclarations = HashMap<IrSymbolOwner, IrField>()
    private val outerThisDeclarations = HashMap<IrClass, IrField>()
    private val innerClassConstructors = HashMap<IrConstructor, IrConstructor>()

    private val defaultImplsMethods = HashMap<IrSimpleFunction, IrSimpleFunction>()
    private val defaultImplsClasses = HashMap<IrClass, IrClass>()
    private val defaultImplsRedirections = HashMap<IrSimpleFunction, IrSimpleFunction>()

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
            oldConstructor.startOffset,
            oldConstructor.endOffset,
            oldConstructor.origin,
            IrConstructorSymbolImpl(newDescriptor),
            oldConstructor.name,
            oldConstructor.visibility,
            oldConstructor.returnType,
            isInline = oldConstructor.isInline,
            isExternal = oldConstructor.isExternal,
            isPrimary = oldConstructor.isPrimary,
            isExpect = oldConstructor.isExpect
        ).apply {
            newDescriptor.bind(this)
            annotations.addAll(oldConstructor.annotations.map { it.deepCopyWithSymbols(this) })
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

    fun getPrivateFieldForObjectInstance(singleton: IrClass): IrField =
        if (singleton.isCompanion && singleton.parentAsClass.isJvmInterface)
            interfaceCompanionFieldDeclarations.getOrPut(singleton) {
                buildField {
                    name = Name.identifier("\$\$INSTANCE")
                    type = singleton.defaultType
                    origin = JvmLoweredDeclarationOrigin.INTERFACE_COMPANION_PRIVATE_INSTANCE
                    isFinal = true
                    isStatic = true
                    visibility = JavaVisibilities.PACKAGE_VISIBILITY
                }.apply {
                    parent = singleton
                }
            }
        else
            getFieldForObjectInstance(singleton)

    fun getDefaultImplsFunction(interfaceFun: IrSimpleFunction): IrSimpleFunction {
        val parent = interfaceFun.parentAsClass
        assert(parent.isJvmInterface) { "Parent of ${interfaceFun.dump()} should be interface" }
        return defaultImplsMethods.getOrPut(interfaceFun) {
            val defaultImpls = getDefaultImplsClass(interfaceFun.parentAsClass)

            val name = Name.identifier(methodSignatureMapper.mapFunctionName(interfaceFun))
            createStaticFunctionWithReceivers(
                defaultImpls, name, interfaceFun,
                dispatchReceiverType = parent.defaultType,
                // If `interfaceFun` is not a real implementation, then we're generating stubs in a descendant
                // interface's DefaultImpls. For example,
                //
                //     interface I1 { fun f() { ... } }
                //     interface I2 : I1
                //
                // is supposed to allow using `I2.DefaultImpls.f` as if it was inherited from `I1.DefaultImpls`.
                // The classes are not actually related and `I2.DefaultImpls.f` is not a fake override but a bridge.
                origin = when {
                    interfaceFun.origin != IrDeclarationOrigin.FAKE_OVERRIDE -> interfaceFun.origin
                    interfaceFun.resolveFakeOverride()!!.origin.isSynthetic -> JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_TO_SYNTHETIC
                    else -> JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE
                },
                // Old backend doesn't generate ACC_FINAL on DefaultImpls methods.
                modality = Modality.OPEN
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
                isInline = false,
                isExpect = false
            ).apply {
                descriptor.bind(this)
                parent = interfaceClass
                createImplicitParameterDeclarationWithWrappedDescriptor()
            }
        }

    fun getDefaultImplsRedirection(fakeOverride: IrSimpleFunction): IrSimpleFunction =
        defaultImplsRedirections.getOrPut(fakeOverride) {
            assert(fakeOverride.origin == IrDeclarationOrigin.FAKE_OVERRIDE)
            val irClass = fakeOverride.parentAsClass
            val descriptor = DescriptorsToIrRemapper.remapDeclaredSimpleFunction(fakeOverride.descriptor)
            with(fakeOverride) {
                IrFunctionImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE,
                    IrSimpleFunctionSymbolImpl(descriptor),
                    name, visibility, modality, returnType,
                    isInline = isInline,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = isSuspend,
                    isExpect = false,
                    isFakeOverride = false
                ).apply {
                    descriptor.bind(this)
                    parent = irClass
                    overriddenSymbols.addAll(fakeOverride.overriddenSymbols)
                    copyParameterDeclarationsFrom(fakeOverride)
                    annotations.addAll(fakeOverride.annotations)
                    fakeOverride.correspondingPropertySymbol?.owner?.let { fakeOverrideProperty ->
                        // NB: property is only generated for the sake of the type mapper.
                        // If both setter and getter are present, original property will be duplicated.
                        val newPropertyDescriptor = DescriptorsToIrRemapper.remapDeclaredProperty(fakeOverrideProperty.descriptor)
                        correspondingPropertySymbol = with(fakeOverrideProperty) {
                            IrPropertyImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                IrDeclarationOrigin.DEFINED, IrPropertySymbolImpl(newPropertyDescriptor),
                                name, visibility, modality, isVar, isConst, isLateinit, isDelegated,
                                isExternal = false,
                                isExpect = isExpect
                            ).apply {
                                newPropertyDescriptor.bind(this)
                                parent = irClass
                            }.symbol
                        }
                    }
                }
            }


        }

}
