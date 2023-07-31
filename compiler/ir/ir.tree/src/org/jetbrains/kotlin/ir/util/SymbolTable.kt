/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazySymbolTable
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*

private fun <SymbolOwner : IrSymbolOwner, Symbol : IrBindableSymbol<*, SymbolOwner>> IdSignatureSymbolTableSlice(lock: IrLock) =
    SymbolTableSlice.Flat<IdSignature, SymbolOwner, Symbol>(lock) { it.signature != null }

@OptIn(SymbolTableInternals::class)
open class SymbolTable(
    val signaturer: IdSignatureComposer,
    val irFactory: IrFactory,
    val nameProvider: NameProvider = NameProvider.DEFAULT,
) : ReferenceSymbolTable {
    val lock: IrLock = IrLock()

    private val scriptSlice = IdSignatureSymbolTableSlice<IrScript, IrScriptSymbol>(lock)
    private val classSlice = IdSignatureSymbolTableSlice<IrClass, IrClassSymbol>(lock)
    private val constructorSlice = IdSignatureSymbolTableSlice<IrConstructor, IrConstructorSymbol>(lock)
    private val enumEntrySlice = IdSignatureSymbolTableSlice<IrEnumEntry, IrEnumEntrySymbol>(lock)
    private val fieldSlice = IdSignatureSymbolTableSlice<IrField, IrFieldSymbol>(lock)
    private val functionSlice = IdSignatureSymbolTableSlice<IrSimpleFunction, IrSimpleFunctionSymbol>(lock)
    private val propertySlice = IdSignatureSymbolTableSlice<IrProperty, IrPropertySymbol>(lock)
    private val typeAliasSlice = IdSignatureSymbolTableSlice<IrTypeAlias, IrTypeAliasSymbol>(lock)
    private val globalTypeParameterSlice = IdSignatureSymbolTableSlice<IrTypeParameter, IrTypeParameterSymbol>(lock)

    @Suppress("LeakingThis")
    val lazyWrapper = IrLazySymbolTable(this)

    @Suppress("LeakingThis")
    @ObsoleteDescriptorBasedAPI
    override val descriptorExtension: DescriptorSymbolTableExtension = createDescriptorExtension()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    protected open fun createDescriptorExtension(): DescriptorSymbolTableExtension {
        return DescriptorSymbolTableExtension(this)
    }

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

    @SymbolTableInternals
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

    @SymbolTableInternals
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

    @SymbolTableInternals
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

    @SymbolTableInternals
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

    @SymbolTableInternals
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

    @DelicateSymbolTableApi
    fun removeProperty(symbol: IrPropertySymbol) {
        symbol.signature?.let { propertySlice.remove(it) }
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

    @SymbolTableInternals
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

    @DelicateSymbolTableApi
    fun removeSimpleFunction(function: IrSimpleFunctionSymbol) {
        function.signature?.let { functionSlice.remove(it) }
    }

    @SymbolTableInternals
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
        // TODO: probably this function should be completely removed, since it doesn't cache anything. KT-60375
        return typeParameterFactory(symbolFactory(signature))
    }

    override fun referenceTypeParameter(signature: IdSignature): IrTypeParameterSymbol {
        return referenceTypeParameterImpl(
            signature,
            { IrTypeParameterPublicSymbolImpl(signature) },
            { IrTypeParameterSymbolImpl() }
        )
    }

    @SymbolTableInternals
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

    // TODO: move to extensions, KT-60376
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun enterScope(symbol: IrSymbol) {
        descriptorExtension.enterScope(symbol)
    }

    // TODO: move to extensions, KT-60376
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun enterScope(owner: IrDeclaration) {
        descriptorExtension.enterScope(owner)
    }

    // TODO: move to extensions, KT-60376
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun leaveScope(symbol: IrSymbol) {
        descriptorExtension.leaveScope(symbol)
    }

    // TODO: move to extensions, KT-60376
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
     */
    @DelicateSymbolTableApi
    fun forEachDeclarationSymbol(block: (IrSymbol) -> Unit) {
        classSlice.forEachSymbol { block(it) }
        constructorSlice.forEachSymbol { block(it) }
        functionSlice.forEachSymbol { block(it) }
        propertySlice.forEachSymbol { block(it) }
        enumEntrySlice.forEachSymbol { block(it) }
        typeAliasSlice.forEachSymbol { block(it) }
        fieldSlice.forEachSymbol { block(it) }
    }

    /**
     * This method should not be used directly, because in a lot of cases there are unbound symbols
     *   not only in SymbolTable itself, but in descriptorExtension too
     * So please use `SymbolTable.descriptorExtension.allUnboundSymbols` instead
     */
    @DelicateSymbolTableApi
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
}

/*
 * This annotation marks that some method is actually part of SymbolTable implementation and should not be called
 *   outside SymbolTable or its extension
 */
@RequiresOptIn
annotation class SymbolTableInternals

/*
 * This annotation marks that some method of SymbolTable is not very safe and should be used only if you really
 *   know what you are doing
 */
@RequiresOptIn
annotation class DelicateSymbolTableApi

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
