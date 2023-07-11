/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrScriptImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrBasedClassDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.utils.threadLocal

@ObsoleteDescriptorBasedAPI
typealias DescriptorBasedReferenceSymbolTableExtension = ReferenceSymbolTableExtension<
        ClassDescriptor, TypeAliasDescriptor, ScriptDescriptor, FunctionDescriptor, ClassConstructorDescriptor,
        PropertyDescriptor, ParameterDescriptor, TypeParameterDescriptor
        >

@ObsoleteDescriptorBasedAPI
open class DescriptorSymbolTableExtension(table: SymbolTable) : SymbolTableExtension<
        DeclarationDescriptor, ClassDescriptor, TypeAliasDescriptor, ScriptDescriptor, FunctionDescriptor,
        ClassConstructorDescriptor, PropertyDescriptor, ParameterDescriptor, TypeParameterDescriptor>(table)
{
    private val irFactory: IrFactory
        get() = table.irFactory

    private val nameProvider: NameProvider
        get() = table.nameProvider

    private val signatureComposer: IdSignatureComposer
        get() = table.signaturer

    private val externalPackageFragmentSlice: SymbolTableSlice<PackageFragmentDescriptor, IrExternalPackageFragment, IrExternalPackageFragmentSymbol> = SymbolTableSlice.Flat(lock)

    private val valueParameterSlice: SymbolTableSlice.Scoped<ParameterDescriptor, IrValueParameter, IrValueParameterSymbol> by threadLocal {
        SymbolTableSlice.Scoped(lock)
    }

    private val variableSlice: SymbolTableSlice.Scoped<VariableDescriptor, IrVariable, IrVariableSymbol> by threadLocal {
        SymbolTableSlice.Scoped(lock)
    }

    private val localDelegatedPropertySlice: SymbolTableSlice.Scoped<VariableDescriptorWithAccessors, IrLocalDelegatedProperty, IrLocalDelegatedPropertySymbol> by threadLocal {
        SymbolTableSlice.Scoped(lock)
    }

    override fun MutableList<SymbolTableSlice.Scoped<*, *, *>>.initializeScopedSlices() {
        add(valueParameterSlice)
        add(variableSlice)
        add(localDelegatedPropertySlice)
    }

    // ------------------------------------ signature ------------------------------------

    override fun calculateSignature(declaration: DeclarationDescriptor): IdSignature? {
        return signatureComposer.composeSignature(declaration)
    }

    override fun calculateEnumEntrySignature(declaration: ClassDescriptor): IdSignature? {
        return signatureComposer.composeEnumEntrySignature(declaration)
    }

    override fun calculateFieldSignature(declaration: PropertyDescriptor): IdSignature? {
        return signatureComposer.composeFieldSignature(declaration)
    }

    // ------------------------------------ script ------------------------------------

    override fun defaultScriptFactory(startOffset: Int, endOffset: Int, script: ScriptDescriptor, symbol: IrScriptSymbol): IrScript {
        return IrScriptImpl(symbol, nameProvider.nameForDeclaration(script), irFactory, startOffset, endOffset)
    }

    override fun createScriptSymbol(descriptor: ScriptDescriptor, signature: IdSignature?): IrScriptSymbol {
        return IrScriptSymbolImpl(descriptor)
    }

    // ------------------------------------ class ------------------------------------

    override fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol {
        return if (descriptor is IrBasedClassDescriptor) {
            descriptor.owner.symbol
        } else {
            super.referenceClass(descriptor)
        }
    }

    fun declareClassFromLinker(descriptor: ClassDescriptor, signature: IdSignature, classFactory: (IrClassSymbol) -> IrClass): IrClass {
        return declareFromLinker(
            descriptor,
            signature,
            SymbolTable::declareClass,
            ::declareClass,
            ::createClassSymbol,
            classFactory
        )
    }

    override fun createPublicClassSymbol(descriptor: ClassDescriptor, signature: IdSignature): IrClassSymbol {
        return IrClassPublicSymbolImpl(signature, descriptor)
    }

    override fun createPrivateClassSymbol(descriptor: ClassDescriptor): IrClassSymbol {
        return IrClassSymbolImpl(descriptor)
    }

    // ------------------------------------ constructor ------------------------------------

    fun declareConstructorFromLinker(
        descriptor: ClassConstructorDescriptor,
        signature: IdSignature,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor {
        return declareFromLinker(
            descriptor,
            signature,
            SymbolTable::declareConstructor,
            ::declareConstructor,
            ::createConstructorSymbol,
            constructorFactory
        )
    }

    override fun createPublicConstructorSymbol(descriptor: ClassConstructorDescriptor, signature: IdSignature): IrConstructorSymbol {
        return IrConstructorPublicSymbolImpl(signature, descriptor)
    }

    override fun createPrivateConstructorSymbol(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return IrConstructorSymbolImpl(descriptor)
    }

    // ------------------------------------ enum entry ------------------------------------

    fun declareEnumEntryFromLinker(
        descriptor: ClassDescriptor,
        signature: IdSignature,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry,
    ): IrEnumEntry {
        return declareFromLinker(
            descriptor,
            signature,
            SymbolTable::declareEnumEntry,
            ::declareEnumEntry,
            ::createEnumEntrySymbol,
            factory
        )
    }

    override fun createPublicEnumEntrySymbol(descriptor: ClassDescriptor, signature: IdSignature): IrEnumEntrySymbol {
        return IrEnumEntryPublicSymbolImpl(signature, descriptor)
    }

    override fun createPrivateEnumEntrySymbol(descriptor: ClassDescriptor): IrEnumEntrySymbol {
        return IrEnumEntrySymbolImpl(descriptor)
    }

    override fun defaultEnumEntryFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        enumEntry: ClassDescriptor,
        symbol: IrEnumEntrySymbol,
    ): IrEnumEntry {
        return irFactory.createEnumEntry(startOffset, endOffset, origin, nameProvider.nameForDeclaration(enumEntry), symbol)
    }

    // ------------------------------------ field ------------------------------------

    override fun createPublicFieldSymbol(descriptor: PropertyDescriptor, signature: IdSignature): IrFieldSymbol {
        return IrFieldPublicSymbolImpl(signature, descriptor)
    }

    override fun createPrivateFieldSymbol(descriptor: PropertyDescriptor): IrFieldSymbol {
        return IrFieldSymbolImpl(descriptor)
    }

    override fun defaultFieldFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType,
        visibility: DescriptorVisibility?,
        symbol: IrFieldSymbol,
    ): IrField {
        return irFactory.createField(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = nameProvider.nameForDeclaration(descriptor),
            visibility = visibility ?: symbol.descriptor.visibility,
            symbol = symbol,
            type = type,
            isFinal = !symbol.descriptor.isVar,
            isStatic = symbol.descriptor.dispatchReceiverParameter == null,
            isExternal = symbol.descriptor.isEffectivelyExternal(),
        ).apply {
            metadata = DescriptorMetadataSource.Property(symbol.descriptor)
        }
    }

    // ------------------------------------ property ------------------------------------

    fun declarePropertyFromLinker(
        descriptor: PropertyDescriptor,
        signature: IdSignature,
        propertyFactory: (IrPropertySymbol) -> IrProperty,
    ): IrProperty {
        return declareFromLinker(
            descriptor,
            signature,
            SymbolTable::declareProperty,
            ::declareProperty,
            ::createPropertySymbol,
            propertyFactory
        )
    }

    override fun createPublicPropertySymbol(descriptor: PropertyDescriptor, signature: IdSignature): IrPropertySymbol {
        return IrPropertyPublicSymbolImpl(signature, descriptor)
    }

    override fun createPrivatePropertySymbol(descriptor: PropertyDescriptor): IrPropertySymbol {
        return IrPropertySymbolImpl(descriptor)
    }

    override fun defaultPropertyFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        isDelegated: Boolean,
        symbol: IrPropertySymbol,
    ): IrProperty {
        return irFactory.createProperty(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = nameProvider.nameForDeclaration(descriptor),
            visibility = descriptor.visibility,
            modality = descriptor.modality,
            symbol = symbol,
            isVar = descriptor.isVar,
            isConst = descriptor.isConst,
            isLateinit = descriptor.isLateInit,
            isDelegated = isDelegated,
            isExternal = descriptor.isEffectivelyExternal(),
            isExpect = descriptor.isExpect,
            isFakeOverride = descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
        ).apply {
            metadata = DescriptorMetadataSource.Property(symbol.descriptor)
        }
    }

    // ------------------------------------ typealias ------------------------------------

    override fun createPublicTypeAliasSymbol(descriptor: TypeAliasDescriptor, signature: IdSignature): IrTypeAliasSymbol {
        return IrTypeAliasPublicSymbolImpl(signature, descriptor)
    }

    override fun createPrivateTypeAliasSymbol(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol {
        return IrTypeAliasSymbolImpl(descriptor)
    }

    // ------------------------------------ function ------------------------------------

    fun declareSimpleFunctionFromLinker(
        descriptor: FunctionDescriptor,
        signature: IdSignature,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction,
    ): IrSimpleFunction {
        return declareFromLinker(
            descriptor,
            signature,
            SymbolTable::declareSimpleFunction,
            ::declareSimpleFunction,
            ::createFunctionSymbol,
            functionFactory
        )
    }

    override fun createPublicFunctionSymbol(descriptor: FunctionDescriptor, signature: IdSignature): IrSimpleFunctionSymbol {
        return IrSimpleFunctionPublicSymbolImpl(signature, descriptor)
    }

    override fun createPrivateFunctionSymbol(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
        return IrSimpleFunctionSymbolImpl(descriptor)
    }

    // ------------------------------------ type parameter ------------------------------------

    override fun createPublicTypeParameterSymbol(descriptor: TypeParameterDescriptor, signature: IdSignature): IrTypeParameterSymbol {
        return IrTypeParameterPublicSymbolImpl(signature, descriptor)
    }

    override fun createPrivateTypeParameterSymbol(descriptor: TypeParameterDescriptor): IrTypeParameterSymbol {
        return IrTypeParameterSymbolImpl(descriptor)
    }

    override fun defaultTypeParameterFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
        symbol: IrTypeParameterSymbol,
    ): IrTypeParameter {
        return irFactory.createTypeParameter(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = nameProvider.nameForDeclaration(descriptor),
            symbol = symbol,
            variance = symbol.descriptor.variance,
            index = symbol.descriptor.index,
            isReified = symbol.descriptor.isReified
        )
    }

    // ------------------------------------ package fragment ------------------------------------

    fun referenceExternalPackageFragment(descriptor: PackageFragmentDescriptor): IrExternalPackageFragmentSymbol =
        externalPackageFragmentSlice.referenced(descriptor) { IrExternalPackageFragmentSymbolImpl(descriptor) }

    fun declareExternalPackageFragment(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        return externalPackageFragmentSlice.declare(
            descriptor,
            { IrExternalPackageFragmentSymbolImpl(descriptor) },
            { IrExternalPackageFragmentImpl(it, descriptor.fqName) }
        )
    }

    fun declareExternalPackageFragmentIfNotExists(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        return externalPackageFragmentSlice.declareIfNotExists(
            descriptor,
            { IrExternalPackageFragmentSymbolImpl(descriptor) },
            { IrExternalPackageFragmentImpl(it, descriptor.fqName) }
        )
    }

    // ------------------------------------ anonymous initializer ------------------------------------

    fun declareAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: ClassDescriptor
    ): IrAnonymousInitializer =
        irFactory.createAnonymousInitializer(
            startOffset, endOffset, origin,
            IrAnonymousInitializerSymbolImpl(descriptor)
        )

    // ------------------------------------ value parameter ------------------------------------

    fun declareValueParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: ParameterDescriptor,
        type: IrType,
        varargElementType: IrType? = null,
        name: Name? = null,
        index: Int? = null,
        isAssignable: Boolean = false,
        valueParameterFactory: (IrValueParameterSymbol) -> IrValueParameter = {
            irFactory.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = name ?: nameProvider.nameForDeclaration(descriptor),
                type = type,
                isAssignable = isAssignable,
                symbol = it,
                index = index ?: descriptor.indexOrMinusOne,
                varargElementType = varargElementType,
                isCrossinline = descriptor.isCrossinline,
                isNoinline = descriptor.isNoinline,
                isHidden = false,
            )
        }
    ): IrValueParameter =
        valueParameterSlice.declareLocal(
            descriptor,
            { IrValueParameterSymbolImpl(descriptor) },
            valueParameterFactory
        )

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun introduceValueParameter(irValueParameter: IrValueParameter) {
        valueParameterSlice.introduceLocal(irValueParameter.descriptor, irValueParameter.symbol)
    }

    override fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol {
        return valueParameterSlice.referenced(descriptor) {
            error("Undefined parameter referenced: $descriptor\n${valueParameterSlice.dump()}")
        }
    }

    open fun referenceValue(value: ValueDescriptor): IrValueSymbol {
        return when (value) {
            is ParameterDescriptor -> valueParameterSlice.referenced(value) { error("Undefined parameter referenced: $value") }
            is VariableDescriptor -> variableSlice.referenced(value) { error("Undefined variable referenced: $value") }
            else -> error("Unexpected value descriptor: $value")
        }
    }


    // ------------------------------------ variable ------------------------------------

    fun declareVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptor,
        type: IrType,
    ): IrVariable =
        variableSlice.declareLocal(
            descriptor,
            { IrVariableSymbolImpl(descriptor) }
        ) {
            IrVariableImpl(
                startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(descriptor), type,
                descriptor.isVar, descriptor.isConst, descriptor.isLateInit
            )
        }

    fun declareVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptor,
        type: IrType,
        irInitializerExpression: IrExpression?
    ): IrVariable =
        declareVariable(startOffset, endOffset, origin, descriptor, type).apply {
            initializer = irInitializerExpression
        }

    // ------------------------------------ local delegated property ------------------------------------

    fun declareLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors,
        type: IrType
    ): IrLocalDelegatedProperty {
        return localDelegatedPropertySlice.declareLocal(
            descriptor,
            { IrLocalDelegatedPropertySymbolImpl(descriptor) },
        ) {
            irFactory.createLocalDelegatedProperty(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = nameProvider.nameForDeclaration(descriptor),
                symbol = it,
                type = type,
                isVar = descriptor.isVar
            )
        }.apply {
            metadata = DescriptorMetadataSource.LocalDelegatedProperty(descriptor)
        }
    }

    fun referenceLocalDelegatedProperty(descriptor: VariableDescriptorWithAccessors): IrLocalDelegatedPropertySymbol =
        localDelegatedPropertySlice.referenced(descriptor) {
            error("Undefined local delegated property referenced: $descriptor")
        }

    // ------------------------------------ utilities ------------------------------------

    private inline fun <D : DeclarationDescriptor, Symbol : IrBindableSymbol<*, SymbolOwner>, SymbolOwner : IrSymbolOwner> declareFromLinker(
        descriptor: D,
        signature: IdSignature,
        declareBySignature: SymbolTable.(IdSignature, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        declareByDescriptor: (D, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        crossinline symbolFactory: SymbolFactory<D, Symbol>,
        noinline ownerFactory: OwnerFactory<Symbol, SymbolOwner>
    ): SymbolOwner {
        return if (signature.isPubliclyVisible) {
            table.declareBySignature(signature, { symbolFactory(descriptor, signature) }, ownerFactory)
        } else {
            declareByDescriptor(descriptor, ownerFactory)
        }
    }
}

