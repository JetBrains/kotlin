/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazySymbolTable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

open class SymbolTable(
    val signaturer: IdSignatureComposer,
    val irFactory: IrFactory,
    val nameProvider: NameProvider = NameProvider.DEFAULT,
) : ReferenceSymbolTable {
    val lock: IrLock = IrLock()

    private val scriptSlice: SymbolTableSlice.Flat<IdSignature, IrScript, IrScriptSymbol> = SymbolTableSlice.Flat(lock)
    private val classSlice: SymbolTableSlice.Flat<IdSignature, IrClass, IrClassSymbol> = SymbolTableSlice.Flat(lock)
    private val constructorSlice: SymbolTableSlice.Flat<IdSignature, IrConstructor, IrConstructorSymbol> = SymbolTableSlice.Flat(lock)
    private val enumEntrySlice: SymbolTableSlice.Flat<IdSignature, IrEnumEntry, IrEnumEntrySymbol> = SymbolTableSlice.Flat(lock)
    private val fieldSlice: SymbolTableSlice.Flat<IdSignature, IrField, IrFieldSymbol> = SymbolTableSlice.Flat(lock)
    private val functionSlice: SymbolTableSlice.Flat<IdSignature, IrSimpleFunction, IrSimpleFunctionSymbol> = SymbolTableSlice.Flat(lock)
    private val propertySlice: SymbolTableSlice.Flat<IdSignature, IrProperty, IrPropertySymbol> = SymbolTableSlice.Flat(lock)
    private val typeAliasSlice: SymbolTableSlice.Flat<IdSignature, IrTypeAlias, IrTypeAliasSymbol> = SymbolTableSlice.Flat(lock)
    private val globalTypeParameterSlice: SymbolTableSlice.Flat<IdSignature, IrTypeParameter, IrTypeParameterSymbol> = SymbolTableSlice.Flat(lock)

    @Suppress("LeakingThis")
    val lazyWrapper = IrLazySymbolTable(this)

    @Suppress("LeakingThis")
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    val descriptorExtension: DescriptorSymbolTableExtension = DescriptorSymbolTableExtension(this)

    // ------------------------------------ script ------------------------------------

    fun declareScript(
        signature: IdSignature,
        symbolFactory: () -> IrScriptSymbol,
        scriptFactory: (IrScriptSymbol) -> IrScript,
    ): IrScript {
        return scriptSlice.declare(
            signature,
            symbolFactory,
            scriptFactory
        )
    }

    // ------------------------------------ class ------------------------------------

    fun declareClass(
        signature: IdSignature,
        symbolFactory: () -> IrClassSymbol,
        classFactory: (IrClassSymbol) -> IrClass,
    ): IrClass {
        return classSlice.declare(
            signature,
            symbolFactory,
            classFactory
        )
    }

    fun declareClassWithSignature(signature: IdSignature, symbol: IrClassSymbol) {
        classSlice.set(signature, symbol)
    }

    fun declareClassIfNotExists(
        signature: IdSignature,
        symbolFactory: () -> IrClassSymbol,
        classFactory: (IrClassSymbol) -> IrClass,
    ): IrClass {
        return classSlice.declareIfNotExists(signature, symbolFactory, classFactory)
    }

    fun referenceClass(
        signature: IdSignature,
        symbolFactory: () -> IrClassSymbol,
        classFactory: (IrClassSymbol) -> IrClass,
    ): IrClassSymbol {
        return classSlice.referenced(signature) { declareClass(signature, symbolFactory, classFactory).symbol }
    }

    override fun referenceClass(signature: IdSignature): IrClassSymbol {
        return referenceClassImpl(
            signature,
            { IrClassPublicSymbolImpl(signature) },
            { IrClassSymbolImpl().also { it.privateSignature = signature } }
        )
    }

    // TODO: add OptIn
    internal inline fun referenceClassImpl(
        signature: IdSignature,
        publicSymbolFactory: () -> IrClassSymbol,
        privateSymbolFactory: () -> IrClassSymbol,
    ): IrClassSymbol {
        return when {
            signature.isPubliclyVisible -> classSlice.referenced(signature) { publicSymbolFactory() }
            else -> privateSymbolFactory()
        }
    }

    // ------------------------------------ constructor ------------------------------------

    fun declareConstructor(
        signature: IdSignature,
        symbolFactory: () -> IrConstructorSymbol,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor {
        return constructorSlice.declare(
            signature,
            symbolFactory,
            constructorFactory
        )
    }

    fun declareConstructorIfNotExists(
        signature: IdSignature,
        symbolFactory: () -> IrConstructorSymbol,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor {
        return constructorSlice.declareIfNotExists(signature, symbolFactory, constructorFactory)
    }

    fun declareConstructorWithSignature(signature: IdSignature, symbol: IrConstructorSymbol) {
        constructorSlice.set(signature, symbol)
    }

    fun referenceConstructorIfAny(signature: IdSignature): IrConstructorSymbol? {
        return constructorSlice.get(signature)
    }

    override fun referenceConstructor(signature: IdSignature): IrConstructorSymbol {
        return referenceConstructorImpl(
            signature,
            { IrConstructorPublicSymbolImpl(signature) },
            { IrConstructorSymbolImpl() }
        )
    }

    // TODO: add OptIn
    internal inline fun referenceConstructorImpl(
        signature: IdSignature,
        publicSymbolFactory: () -> IrConstructorSymbol,
        privateSymbolFactory: () -> IrConstructorSymbol,
    ): IrConstructorSymbol {
        return when {
            signature.isPubliclyVisible -> constructorSlice.referenced(signature) { publicSymbolFactory() }
            else -> privateSymbolFactory().also {
                it.privateSignature = signature
            }
        }
    }

    // ------------------------------------ enum entry ------------------------------------

    fun declareEnumEntry(
        signature: IdSignature,
        symbolFactory: () -> IrEnumEntrySymbol,
        enumEntryFactory: (IrEnumEntrySymbol) -> IrEnumEntry,
    ): IrEnumEntry {
        return enumEntrySlice.declare(signature, symbolFactory, enumEntryFactory)
    }

    fun declareEnumEntryIfNotExists(
        signature: IdSignature,
        symbolFactory: () -> IrEnumEntrySymbol,
        classFactory: (IrEnumEntrySymbol) -> IrEnumEntry,
    ): IrEnumEntry {
        return enumEntrySlice.declareIfNotExists(signature, symbolFactory, classFactory)
    }

    override fun referenceEnumEntry(signature: IdSignature): IrEnumEntrySymbol {
        return referenceEnumEntryImpl(
            signature,
            { IrEnumEntryPublicSymbolImpl(signature) },
            { IrEnumEntrySymbolImpl() }
        )
    }

    // TODO: add OptIn
    internal inline fun referenceEnumEntryImpl(
        signature: IdSignature,
        publicSymbolFactory: () -> IrEnumEntrySymbol,
        privateSymbolFactory: () -> IrEnumEntrySymbol,
    ): IrEnumEntrySymbol {
        return when {
            signature.isPubliclyVisible -> enumEntrySlice.referenced(signature) { publicSymbolFactory() }
            else -> privateSymbolFactory().also {
                it.privateSignature = signature
            }
        }
    }

    // ------------------------------------ field ------------------------------------

    fun declareField(
        signature: IdSignature,
        symbolFactory: () -> IrFieldSymbol,
        propertyFactory: (IrFieldSymbol) -> IrField,
    ): IrField {
        return fieldSlice.declare(
            signature,
            symbolFactory,
            propertyFactory
        )
    }

    fun declareFieldWithSignature(signature: IdSignature, symbol: IrFieldSymbol) {
        fieldSlice.set(signature, symbol)
    }

    override fun referenceField(signature: IdSignature): IrFieldSymbol {
        return referenceFieldImpl(
            signature,
            { IrFieldPublicSymbolImpl(signature) },
            { IrFieldSymbolImpl() }
        )
    }

    // TODO: add OptIn
    internal inline fun referenceFieldImpl(
        signature: IdSignature,
        publicSymbolFactory: () -> IrFieldSymbol,
        privateSymbolFactory: () -> IrFieldSymbol,
    ): IrFieldSymbol {
        return when {
            signature.isPubliclyVisible -> fieldSlice.referenced(signature) { publicSymbolFactory() }
            else -> privateSymbolFactory().also {
                it.privateSignature = signature
            }
        }
    }

    // ------------------------------------ property ------------------------------------

    fun declareProperty(
        signature: IdSignature,
        symbolFactory: () -> IrPropertySymbol,
        propertyFactory: (IrPropertySymbol) -> IrProperty,
    ): IrProperty {
        return propertySlice.declare(
            signature,
            symbolFactory,
            propertyFactory
        )
    }

    fun declarePropertyIfNotExists(
        signature: IdSignature,
        symbolFactory: () -> IrPropertySymbol,
        propertyFactory: (IrPropertySymbol) -> IrProperty,
    ): IrProperty {
        return propertySlice.declareIfNotExists(signature, symbolFactory, propertyFactory)
    }

    fun declarePropertyWithSignature(signature: IdSignature, symbol: IrPropertySymbol) {
        propertySlice.set(signature, symbol)
    }

    fun referencePropertyIfAny(signature: IdSignature): IrPropertySymbol? {
        return propertySlice.get(signature)
    }

    override fun referenceProperty(signature: IdSignature): IrPropertySymbol {
        return referencePropertyImpl(
            signature,
            { IrPropertyPublicSymbolImpl(signature) },
            { IrPropertySymbolImpl() }
        )
    }

    // TODO: add OptIn
    internal inline fun referencePropertyImpl(
        signature: IdSignature,
        publicSymbolFactory: () -> IrPropertySymbol,
        privateSymbolFactory: () -> IrPropertySymbol,
    ): IrPropertySymbol {
        return when {
            signature.isPubliclyVisible -> propertySlice.referenced(signature) { publicSymbolFactory() }
            else -> privateSymbolFactory().also {
                it.privateSignature = signature
            }
        }
    }

    // ------------------------------------ typealias ------------------------------------

    fun declareTypeAlias(
        signature: IdSignature,
        symbolFactory: () -> IrTypeAliasSymbol,
        factory: (IrTypeAliasSymbol) -> IrTypeAlias,
    ): IrTypeAlias {
        return typeAliasSlice.declare(signature, symbolFactory, factory)
    }

    fun declareTypeAliasIfNotExists(
        signature: IdSignature,
        symbolFactory: () -> IrTypeAliasSymbol,
        typeAliasFactory: (IrTypeAliasSymbol) -> IrTypeAlias,
    ): IrTypeAlias {
        return typeAliasSlice.declareIfNotExists(signature, symbolFactory, typeAliasFactory)
    }

    override fun referenceTypeAlias(signature: IdSignature): IrTypeAliasSymbol {
        return referenceTypeAliasImpl(
            signature,
            { IrTypeAliasPublicSymbolImpl(signature) },
            { IrTypeAliasSymbolImpl() }
        )
    }

    // TODO: add OptIn
    internal inline fun referenceTypeAliasImpl(
        signature: IdSignature,
        publicSymbolFactory: () -> IrTypeAliasSymbol,
        privateSymbolFactory: () -> IrTypeAliasSymbol,
    ): IrTypeAliasSymbol {
        return when {
            signature.isPubliclyVisible -> typeAliasSlice.referenced(signature) { publicSymbolFactory() }
            else -> privateSymbolFactory().also {
                it.privateSignature = signature
            }
        }
    }

    // ------------------------------------ function ------------------------------------

    fun declareSimpleFunction(
        signature: IdSignature,
        symbolFactory: () -> IrSimpleFunctionSymbol,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction,
    ): IrSimpleFunction {
        return functionSlice.declare(
            signature,
            symbolFactory,
            functionFactory
        )
    }

    fun declareSimpleFunctionIfNotExists(
        signature: IdSignature,
        symbolFactory: () -> IrSimpleFunctionSymbol,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction,
    ): IrSimpleFunction {
        return functionSlice.declareIfNotExists(signature, symbolFactory, functionFactory)
    }

    fun declareSimpleFunctionWithSignature(signature: IdSignature, symbol: IrSimpleFunctionSymbol) {
        functionSlice.set(signature, symbol)
    }

    fun referenceSimpleFunctionIfAny(signature: IdSignature): IrSimpleFunctionSymbol? =
        functionSlice.get(signature)

    override fun referenceSimpleFunction(signature: IdSignature): IrSimpleFunctionSymbol {
        return referenceSimpleFunctionImpl(
            signature,
            { IrSimpleFunctionPublicSymbolImpl(signature) },
            { IrSimpleFunctionSymbolImpl().also { it.privateSignature = signature } }
        )
    }

    // TODO: add OptIn
    internal inline fun referenceSimpleFunctionImpl(
        signature: IdSignature,
        publicSymbolFactory: () -> IrSimpleFunctionSymbol,
        privateSymbolFactory: () -> IrSimpleFunctionSymbol,
    ): IrSimpleFunctionSymbol {
        return when {
            signature.isPubliclyVisible -> functionSlice.referenced(signature) { publicSymbolFactory() }
            else -> privateSymbolFactory().also {
                it.privateSignature = signature
            }
        }
    }

    // ------------------------------------ type parameter ------------------------------------

    fun declareGlobalTypeParameter(
        signature: IdSignature,
        symbolFactory: () -> IrTypeParameterSymbol,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter,
    ): IrTypeParameter {
        return globalTypeParameterSlice.declare(signature, symbolFactory, typeParameterFactory)
    }

    fun declareScopedTypeParameter(
        signature: IdSignature,
        symbolFactory: (IdSignature) -> IrTypeParameterSymbol,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter,
    ): IrTypeParameter {
        // TODO: suspicios
        return typeParameterFactory(symbolFactory(signature))
    }

    override fun referenceTypeParameter(signature: IdSignature): IrTypeParameterSymbol {
        return referenceTypeParameterImpl(
            signature,
            { IrTypeParameterPublicSymbolImpl(signature) },
            { IrTypeParameterSymbolImpl() }
        )
    }

    // TODO: add OptIn
    internal inline fun referenceTypeParameterImpl(
        signature: IdSignature,
        publicSymbolFactory: () -> IrTypeParameterSymbol,
        privateSymbolFactory: () -> IrTypeParameterSymbol,
    ): IrTypeParameterSymbol {
        return when {
            signature.isPubliclyVisible -> globalTypeParameterSlice.referenced(signature) { publicSymbolFactory() }
            else -> privateSymbolFactory().also {
                it.privateSignature = signature
            }
        }
    }

    // ------------------------------------ scopes ------------------------------------

    // TODO: move to extensions
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun enterScope(symbol: IrSymbol) {
        descriptorExtension.enterScope(symbol)
    }

    // TODO: move to extensions
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun enterScope(owner: IrDeclaration) {
        descriptorExtension.enterScope(owner)
    }

    // TODO: move to extensions
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun leaveScope(symbol: IrSymbol) {
        descriptorExtension.leaveScope(symbol)
    }

    // TODO: move to extensions
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun leaveScope(owner: IrDeclaration) {
        descriptorExtension.leaveScope(owner)
    }

    // ------------------------------------ utils ------------------------------------

    /**
     * This function is quite messy and doesn't have good contract of what exactly is traversed.
     * Basic idea is it traverse symbols which can be reasonable referered from other module
     *
     * Be careful when using it, and avoid it, except really need.
     *
     * TODO: add some OptIn
     */
    fun forEachDeclarationSymbol(block: (IrSymbol) -> Unit) {
        classSlice.forEachSymbol { block(it) }
        constructorSlice.forEachSymbol { block(it) }
        functionSlice.forEachSymbol { block(it) }
        propertySlice.forEachSymbol { block(it) }
        enumEntrySlice.forEachSymbol { block(it) }
        typeAliasSlice.forEachSymbol { block(it) }
        fieldSlice.forEachSymbol { block(it) }
    }

    val allUnboundSymbols: Set<IrSymbol>
        get() = buildSet {
            fun addUnbound(slice: SymbolTableSlice<IdSignature, *, *>) {
                slice.unboundSymbols.filterTo(this) { !it.isBound }
            }

            addUnbound(classSlice)
            addUnbound(constructorSlice)
            addUnbound(functionSlice)
            addUnbound(propertySlice)
            addUnbound(enumEntrySlice)
            addUnbound(typeAliasSlice)
            addUnbound(fieldSlice)
        }

    // ------------------------------------ descriptors ------------------------------------

    @ObsoleteDescriptorBasedAPI
    override fun referenceScript(descriptor: ScriptDescriptor): IrScriptSymbol {
        return descriptorExtension.referenceScript(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareScript(
        startOffset: Int,
        endOffset: Int,
        descriptor: ScriptDescriptor,
    ): IrScript {
        return descriptorExtension.declareScript(startOffset, endOffset, descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun referenceExternalPackageFragment(descriptor: PackageFragmentDescriptor): IrExternalPackageFragmentSymbol {
        return descriptorExtension.referenceExternalPackageFragment(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareExternalPackageFragment(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        return descriptorExtension.declareExternalPackageFragment(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareExternalPackageFragmentIfNotExists(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        return descriptorExtension.declareExternalPackageFragmentIfNotExists(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: ClassDescriptor,
    ): IrAnonymousInitializer {
        return descriptorExtension.declareAnonymousInitializer(startOffset, endOffset, origin, descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareClass(
        descriptor: ClassDescriptor,
        classFactory: (IrClassSymbol) -> IrClass,
    ): IrClass {
        return descriptorExtension.declareClass(descriptor, classFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareClassIfNotExists(descriptor: ClassDescriptor, classFactory: (IrClassSymbol) -> IrClass): IrClass {
        return descriptorExtension.declareClassIfNotExists(descriptor, classFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareClassFromLinker(descriptor: ClassDescriptor, signature: IdSignature, factory: (IrClassSymbol) -> IrClass): IrClass {
        return descriptorExtension.declareClassFromLinker(descriptor, signature, factory)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol {
        return descriptorExtension.referenceClass(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareConstructor(
        descriptor: ClassConstructorDescriptor,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor {
        return descriptorExtension.declareConstructor(descriptor, constructorFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareConstructorIfNotExists(
        descriptor: ClassConstructorDescriptor,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor {
        return descriptorExtension.declareConstructorIfNotExists(descriptor, constructorFactory)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return descriptorExtension.referenceConstructor(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareConstructorFromLinker(
        descriptor: ClassConstructorDescriptor,
        signature: IdSignature,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor {
        return descriptorExtension.declareConstructorFromLinker(descriptor, signature, constructorFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareEnumEntry(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
    ): IrEnumEntry {
        return descriptorExtension.declareEnumEntry(startOffset, endOffset, origin, descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareEnumEntry(
        descriptor: ClassDescriptor,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry,
    ): IrEnumEntry {
        return descriptorExtension.declareEnumEntry(descriptor, factory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareEnumEntryIfNotExists(descriptor: ClassDescriptor, factory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry {
        return descriptorExtension.declareEnumEntryIfNotExists(descriptor, factory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareEnumEntryFromLinker(
        descriptor: ClassDescriptor,
        signature: IdSignature,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry,
    ): IrEnumEntry {
        return descriptorExtension.declareEnumEntryFromLinker(descriptor, signature, factory)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol {
        return descriptorExtension.referenceEnumEntry(descriptor)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType,
        visibility: DescriptorVisibility? = null,
        fieldFactory: (IrFieldSymbol) -> IrField,
    ): IrField {
        return descriptorExtension.declareField(startOffset, endOffset, origin, descriptor, type, visibility, fieldFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType,
        irInitializer: IrExpressionBody?,
    ): IrField {
        return descriptorExtension.declareField(startOffset, endOffset, origin, descriptor, type, irInitializer)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceField(descriptor: PropertyDescriptor): IrFieldSymbol {
        return descriptorExtension.referenceField(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        isDelegated: Boolean = descriptor.isDelegated,
    ): IrProperty {
        return descriptorExtension.declareProperty(startOffset, endOffset, origin, descriptor, isDelegated)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareProperty(
        descriptor: PropertyDescriptor,
        propertyFactory: (IrPropertySymbol) -> IrProperty,
    ): IrProperty {
        return descriptorExtension.declareProperty(descriptor, propertyFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declarePropertyIfNotExists(descriptor: PropertyDescriptor, propertyFactory: (IrPropertySymbol) -> IrProperty): IrProperty {
        return descriptorExtension.declarePropertyIfNotExists(descriptor, propertyFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declarePropertyFromLinker(
        descriptor: PropertyDescriptor,
        signature: IdSignature,
        factory: (IrPropertySymbol) -> IrProperty,
    ): IrProperty {
        return descriptorExtension.declarePropertyFromLinker(descriptor, signature, factory)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol {
        return descriptorExtension.referenceProperty(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol {
        return descriptorExtension.referenceTypeAlias(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareTypeAlias(descriptor: TypeAliasDescriptor, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias {
        return descriptorExtension.declareTypeAlias(descriptor, factory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareTypeAliasIfNotExists(descriptor: TypeAliasDescriptor, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias {
        return descriptorExtension.declareTypeAliasIfNotExists(descriptor, factory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareSimpleFunction(
        descriptor: FunctionDescriptor,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction,
    ): IrSimpleFunction {
        return descriptorExtension.declareSimpleFunction(descriptor, functionFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareSimpleFunctionIfNotExists(
        descriptor: FunctionDescriptor,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction,
    ): IrSimpleFunction {
        return descriptorExtension.declareSimpleFunctionIfNotExists(descriptor, functionFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareSimpleFunctionFromLinker(
        descriptor: FunctionDescriptor,
        signature: IdSignature,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction,
    ): IrSimpleFunction {
        return descriptorExtension.declareSimpleFunctionFromLinker(descriptor, signature, functionFactory)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
        return descriptorExtension.referenceSimpleFunction(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceDeclaredFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
        return descriptorExtension.referenceDeclaredFunction(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareGlobalTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
    ): IrTypeParameter {
        return descriptorExtension.declareGlobalTypeParameter(startOffset, endOffset, origin, descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareScopedTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter,
    ): IrTypeParameter {
        return descriptorExtension.declareScopedTypeParameter(startOffset, endOffset, origin, descriptor, typeParameterFactory)
    }

    @ObsoleteDescriptorBasedAPI
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
        valueParameterFactory: (IrValueParameterSymbol) -> IrValueParameter,
    ): IrValueParameter {
        return descriptorExtension.declareValueParameter(
            startOffset,
            endOffset,
            origin,
            descriptor,
            type,
            varargElementType,
            name,
            index,
            isAssignable,
            valueParameterFactory
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun introduceValueParameter(irValueParameter: IrValueParameter) {
        descriptorExtension.introduceValueParameter(irValueParameter)
    }

    @ObsoleteDescriptorBasedAPI
    open fun referenceValue(value: ValueDescriptor): IrValueSymbol {
        return descriptorExtension.referenceValue(value)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol {
        return descriptorExtension.referenceValueParameter(descriptor)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol {
        return descriptorExtension.referenceTypeParameter(classifier)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceScopedTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol {
        return descriptorExtension.referenceScopedTypeParameter(classifier)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptor,
        type: IrType,
        variableFactory: (IrVariableSymbol) -> IrVariable,
    ): IrVariable {
        return descriptorExtension.declareVariable(startOffset, endOffset, origin, descriptor, type, variableFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptor,
        type: IrType,
        irInitializerExpression: IrExpression?,
    ): IrVariable {
        return descriptorExtension.declareVariable(startOffset, endOffset, origin, descriptor, type, irInitializerExpression)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors,
        type: IrType,
        factory: (IrLocalDelegatedPropertySymbol) -> IrLocalDelegatedProperty,
    ): IrLocalDelegatedProperty {
        return descriptorExtension.declareLocalDelegatedProperty(startOffset, endOffset, origin, descriptor, type, factory)
    }

    @ObsoleteDescriptorBasedAPI
    fun referenceLocalDelegatedProperty(descriptor: VariableDescriptorWithAccessors): IrLocalDelegatedPropertySymbol {
        return descriptorExtension.referenceLocalDelegatedProperty(descriptor)
    }
}

inline fun <T> SymbolTable.withScope(owner: IrSymbol, block: SymbolTable.() -> T): T {
    enterScope(owner)
    val result = block()
    leaveScope(owner)
    return result
}

inline fun <T> SymbolTable.withScope(owner: IrDeclaration, block: SymbolTable.() -> T): T {
    enterScope(owner)
    val result = block()
    leaveScope(owner)
    return result
}

inline fun <T> ReferenceSymbolTable.withReferenceScope(owner: IrDeclaration, block: ReferenceSymbolTable.() -> T): T {
    enterScope(owner)
    val result = block()
    leaveScope(owner)
    return result
}
