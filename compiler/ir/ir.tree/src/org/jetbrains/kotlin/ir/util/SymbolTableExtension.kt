/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.utils.threadLocal

abstract class ReferenceSymbolTableExtension<Class, TypeAlias, Script, Function, Constructor, Property, ValueParameter, TypeParameter> {
    abstract fun referenceScript(declaration: Script): IrScriptSymbol
    abstract fun referenceClass(declaration: Class): IrClassSymbol
    abstract fun referenceConstructor(declaration: Constructor): IrConstructorSymbol
    abstract fun referenceEnumEntry(declaration: Class): IrEnumEntrySymbol
    abstract fun referenceField(declaration: Property): IrFieldSymbol
    abstract fun referenceProperty(declaration: Property): IrPropertySymbol
    abstract fun referenceSimpleFunction(declaration: Function): IrSimpleFunctionSymbol
    abstract fun referenceDeclaredFunction(declaration: Function): IrSimpleFunctionSymbol
    abstract fun referenceValueParameter(declaration: ValueParameter): IrValueParameterSymbol
    abstract fun referenceTypeParameter(declaration: TypeParameter): IrTypeParameterSymbol
    abstract fun referenceScopedTypeParameter(declaration: TypeParameter): IrTypeParameterSymbol
    abstract fun referenceTypeAlias(declaration: TypeAlias): IrTypeAliasSymbol
}

typealias SymbolFactory<Declaration, Symbol> = (Declaration, IdSignature?) -> Symbol
typealias OwnerFactory<Symbol, SymbolOwner> = (Symbol) -> SymbolOwner

@OptIn(SymbolTableInternals::class)
abstract class SymbolTableExtension<
        Declaration, Class, TypeAlias, Script, Function, Constructor,
        Property, ValueParameter, TypeParameter,
        >(
    val table: SymbolTable,
) : ReferenceSymbolTableExtension<Class, TypeAlias, Script, Function, Constructor, Property, ValueParameter, TypeParameter>()
        where Class : Declaration,
              TypeAlias : Declaration,
              Script : Declaration,
              Function : Declaration,
              Constructor : Declaration,
              Property : Declaration,
              ValueParameter : Declaration,
              TypeParameter : Declaration {
    protected val lock: IrLock
        get() = table.lock

    private val scriptSlice: SymbolTableSlice.Flat<Script, IrScript, IrScriptSymbol> =
        SymbolTableSlice.Flat(lock)
    private val classSlice: SymbolTableSlice.Flat<Class, IrClass, IrClassSymbol> =
        SymbolTableSlice.Flat(lock)
    private val constructorSlice: SymbolTableSlice.Flat<Constructor, IrConstructor, IrConstructorSymbol> =
        SymbolTableSlice.Flat(lock)
    private val enumEntrySlice: SymbolTableSlice.Flat<Class, IrEnumEntry, IrEnumEntrySymbol> =
        SymbolTableSlice.Flat(lock)
    private val fieldSlice: SymbolTableSlice.Flat<Property, IrField, IrFieldSymbol> =
        SymbolTableSlice.Flat(lock)
    private val functionSlice: SymbolTableSlice.Flat<Function, IrSimpleFunction, IrSimpleFunctionSymbol> =
        SymbolTableSlice.Flat(lock)
    private val propertySlice: SymbolTableSlice.Flat<Property, IrProperty, IrPropertySymbol> =
        SymbolTableSlice.Flat(lock)
    private val typeAliasSlice: SymbolTableSlice.Flat<TypeAlias, IrTypeAlias, IrTypeAliasSymbol> =
        SymbolTableSlice.Flat(lock)
    private val globalTypeParameterSlice: SymbolTableSlice.Flat<TypeParameter, IrTypeParameter, IrTypeParameterSymbol> =
        SymbolTableSlice.Flat(lock)

    private val scopedTypeParameterSlice: SymbolTableSlice.Scoped<TypeParameter, IrTypeParameter, IrTypeParameterSymbol> by threadLocal {
        SymbolTableSlice.Scoped(lock)
    }

    protected abstract fun MutableList<SymbolTableSlice.Scoped<*, *, *>>.initializeScopedSlices()

    private val scopedSlices: List<SymbolTableSlice.Scoped<*, *, *>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            add(scopedTypeParameterSlice)
            initializeScopedSlices()
        }
    }

    // ------------------------------------ signature ------------------------------------

    protected abstract fun calculateSignature(declaration: Declaration): IdSignature?
    protected abstract fun calculateEnumEntrySignature(declaration: Class): IdSignature?
    protected abstract fun calculateFieldSignature(declaration: Property): IdSignature?

    // ------------------------------------ script ------------------------------------

    fun declareScript(
        startOffset: Int,
        endOffset: Int,
        declaration: Script,
    ): IrScript {
        return declare(
            declaration,
            scriptSlice,
            SymbolTable::declareScript,
            symbolFactory = { createScriptSymbol(declaration, it) },
            ownerFactory = { defaultScriptFactory(startOffset, endOffset, declaration, it) }
        )
    }

    override fun referenceScript(declaration: Script): IrScriptSymbol {
        return scriptSlice.referenced(declaration) { createScriptSymbol(declaration, signature = null) }
    }

    protected abstract fun defaultScriptFactory(startOffset: Int, endOffset: Int, script: Script, symbol: IrScriptSymbol): IrScript

    protected open fun createScriptSymbol(declaration: Script, signature: IdSignature?): IrScriptSymbol {
        return IrScriptSymbolImpl()
    }

    // ------------------------------------ class ------------------------------------

    fun declareClass(declaration: Class, classFactory: (IrClassSymbol) -> IrClass): IrClass {
        return declare(
            declaration,
            classSlice,
            SymbolTable::declareClass,
            { createClassSymbol(declaration, it) },
            classFactory
        )
    }

    fun declareClassIfNotExists(declaration: Class, classFactory: (IrClassSymbol) -> IrClass): IrClass {
        return declareIfNotExist(
            declaration,
            classSlice,
            SymbolTable::declareClassIfNotExists,
            { createClassSymbol(declaration, it) },
            classFactory
        )
    }

    override fun referenceClass(declaration: Class): IrClassSymbol {
        return reference(
            declaration,
            classSlice,
            SymbolTable::referenceClassImpl,
            ::createClassSymbol,
            ::createPublicClassSymbol,
            ::createPrivateClassSymbol
        )
    }

    protected fun createClassSymbol(declaration: Class, signature: IdSignature?): IrClassSymbol {
        return signature?.let { createPublicClassSymbol(declaration, signature) } ?: createPrivateClassSymbol(declaration)
    }

    protected open fun createPublicClassSymbol(declaration: Class, signature: IdSignature): IrClassSymbol {
        return IrClassPublicSymbolImpl(signature)
    }

    protected open fun createPrivateClassSymbol(descriptor: Class): IrClassSymbol {
        return IrClassSymbolImpl()
    }

    // ------------------------------------ constructor ------------------------------------

    fun declareConstructor(declaration: Constructor, constructorFactory: (IrConstructorSymbol) -> IrConstructor): IrConstructor {
        return declare(
            declaration,
            constructorSlice,
            SymbolTable::declareConstructor,
            { createConstructorSymbol(declaration, it) },
            constructorFactory
        )
    }

    fun declareConstructorIfNotExists(
        declaration: Constructor,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor {
        return declareIfNotExist(
            declaration,
            constructorSlice,
            SymbolTable::declareConstructorIfNotExists,
            { createConstructorSymbol(declaration, it) },
            constructorFactory
        )
    }

    override fun referenceConstructor(declaration: Constructor): IrConstructorSymbol {
        return reference(
            declaration,
            constructorSlice,
            SymbolTable::referenceConstructorImpl,
            ::createConstructorSymbol,
            ::createPublicConstructorSymbol,
            ::createPrivateConstructorSymbol
        )
    }

    protected fun createConstructorSymbol(declaration: Constructor, signature: IdSignature?): IrConstructorSymbol {
        return signature?.let { createPublicConstructorSymbol(declaration, signature) } ?: createPrivateConstructorSymbol(declaration)
    }

    protected open fun createPublicConstructorSymbol(declaration: Constructor, signature: IdSignature): IrConstructorSymbol {
        return IrConstructorPublicSymbolImpl(signature)
    }

    protected open fun createPrivateConstructorSymbol(declaration: Constructor): IrConstructorSymbol {
        return IrConstructorSymbolImpl()
    }

    // ------------------------------------ enum entry ------------------------------------

    fun declareEnumEntry(declaration: Class, enumEntryFactory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry {
        return declare(
            declaration,
            enumEntrySlice,
            SymbolTable::declareEnumEntry,
            { createEnumEntrySymbol(declaration, it) },
            enumEntryFactory,
            ::calculateEnumEntrySignature
        )
    }

    fun declareEnumEntry(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, declaration: Class): IrEnumEntry {
        return declareEnumEntry(
            declaration
        ) { enumEntrySymbol -> defaultEnumEntryFactory(startOffset, endOffset, origin, declaration, enumEntrySymbol) }
    }

    fun declareEnumEntryIfNotExists(declaration: Class, enumEntryFactory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry {
        return declareIfNotExist(
            declaration,
            enumEntrySlice,
            SymbolTable::declareEnumEntryIfNotExists,
            { createEnumEntrySymbol(declaration, it) },
            enumEntryFactory,
            ::calculateEnumEntrySignature
        )
    }

    override fun referenceEnumEntry(declaration: Class): IrEnumEntrySymbol {
        return reference(
            declaration,
            enumEntrySlice,
            SymbolTable::referenceEnumEntryImpl,
            ::createEnumEntrySymbol,
            ::createPublicEnumEntrySymbol,
            ::createPrivateEnumEntrySymbol,
            ::calculateEnumEntrySignature
        )
    }

    protected fun createEnumEntrySymbol(declaration: Class, signature: IdSignature?): IrEnumEntrySymbol {
        return signature?.let { createPublicEnumEntrySymbol(declaration, signature) } ?: createPrivateEnumEntrySymbol(declaration)
    }

    protected open fun createPublicEnumEntrySymbol(declaration: Class, signature: IdSignature): IrEnumEntrySymbol {
        return IrEnumEntryPublicSymbolImpl(signature)
    }

    protected open fun createPrivateEnumEntrySymbol(declaration: Class): IrEnumEntrySymbol {
        return IrEnumEntrySymbolImpl()
    }

    protected abstract fun defaultEnumEntryFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        enumEntry: Class,
        symbol: IrEnumEntrySymbol,
    ): IrEnumEntry

    // ------------------------------------ field ------------------------------------

    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        declaration: Property,
        type: IrType,
        visibility: DescriptorVisibility? = null,
        fieldFactory: (IrFieldSymbol) -> IrField = {
            defaultFieldFactory(
                startOffset,
                endOffset,
                origin,
                declaration,
                type,
                visibility,
                it
            )
        },
    ): IrField {
        return declare(
            declaration,
            fieldSlice,
            SymbolTable::declareField,
            { createFieldSymbol(declaration, it) },
            fieldFactory,
            ::calculateFieldSignature
        )
    }

    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        declaration: Property,
        type: IrType,
        irInitializer: IrExpressionBody?,
    ): IrField {
        return declareField(startOffset, endOffset, origin, declaration, type).apply {
            initializer = irInitializer
        }
    }

    override fun referenceField(declaration: Property): IrFieldSymbol {
        return reference(
            declaration,
            fieldSlice,
            SymbolTable::referenceFieldImpl,
            ::createFieldSymbol,
            ::createPublicFieldSymbol,
            ::createPrivateFieldSymbol,
            ::calculateFieldSignature
        )
    }

    protected fun createFieldSymbol(declaration: Property, signature: IdSignature?): IrFieldSymbol {
        return signature?.let { createPublicFieldSymbol(declaration, signature) } ?: createPrivateFieldSymbol(declaration)
    }

    protected open fun createPublicFieldSymbol(declaration: Property, signature: IdSignature): IrFieldSymbol {
        return IrFieldPublicSymbolImpl(signature)
    }

    protected open fun createPrivateFieldSymbol(declaration: Property): IrFieldSymbol {
        return IrFieldSymbolImpl()
    }


    protected abstract fun defaultFieldFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        declaration: Property,
        type: IrType,
        visibility: DescriptorVisibility?,
        symbol: IrFieldSymbol,
    ): IrField

    // ------------------------------------ property ------------------------------------

    fun declareProperty(declaration: Property, propertyFactory: (IrPropertySymbol) -> IrProperty): IrProperty {
        return declare(
            declaration,
            propertySlice,
            SymbolTable::declareProperty,
            { createPropertySymbol(declaration, it) },
            propertyFactory
        )
    }

    fun declareProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        declaration: Property,
        isDelegated: Boolean
    ): IrProperty {
        return declareProperty(declaration) { propertySymbol ->
            defaultPropertyFactory(startOffset, endOffset, origin, declaration, isDelegated, propertySymbol)
        }
    }

    fun declarePropertyIfNotExists(declaration: Property, propertyFactory: (IrPropertySymbol) -> IrProperty): IrProperty {
        return declareIfNotExist(
            declaration,
            propertySlice,
            SymbolTable::declarePropertyIfNotExists,
            { createPropertySymbol(declaration, it) },
            propertyFactory
        )
    }

    override fun referenceProperty(declaration: Property): IrPropertySymbol {
        return reference(
            declaration,
            propertySlice,
            SymbolTable::referencePropertyImpl,
            ::createPropertySymbol,
            ::createPublicPropertySymbol,
            ::createPrivatePropertySymbol
        )
    }

    protected fun createPropertySymbol(declaration: Property, signature: IdSignature?): IrPropertySymbol {
        return signature?.let { createPublicPropertySymbol(declaration, signature) } ?: createPrivatePropertySymbol(declaration)
    }

    protected open fun createPublicPropertySymbol(declaration: Property, signature: IdSignature): IrPropertySymbol {
        return IrPropertyPublicSymbolImpl(signature)
    }

    protected open fun createPrivatePropertySymbol(declaration: Property): IrPropertySymbol {
        return IrPropertySymbolImpl()
    }

    protected abstract fun defaultPropertyFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        declaration: Property,
        isDelegated: Boolean,
        symbol: IrPropertySymbol,
    ): IrProperty

    // ------------------------------------ typealias ------------------------------------

    fun declareTypeAlias(declaration: TypeAlias, typeAliasFactory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias {
        return declare(
            declaration,
            typeAliasSlice,
            SymbolTable::declareTypeAlias,
            { createTypeAliasSymbol(declaration, it) },
            typeAliasFactory
        )
    }

    fun declareTypeAliasIfNotExists(declaration: TypeAlias, typeAliasFactory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias {
        return declareIfNotExist(
            declaration,
            typeAliasSlice,
            SymbolTable::declareTypeAliasIfNotExists,
            { createTypeAliasSymbol(declaration, it) },
            typeAliasFactory
        )
    }

    override fun referenceTypeAlias(declaration: TypeAlias): IrTypeAliasSymbol {
        return reference(
            declaration,
            typeAliasSlice,
            SymbolTable::referenceTypeAliasImpl,
            ::createTypeAliasSymbol,
            ::createPublicTypeAliasSymbol,
            ::createPrivateTypeAliasSymbol
        )
    }

    protected fun createTypeAliasSymbol(declaration: TypeAlias, signature: IdSignature?): IrTypeAliasSymbol {
        return signature?.let { createPublicTypeAliasSymbol(declaration, signature) } ?: createPrivateTypeAliasSymbol(declaration)
    }

    protected open fun createPublicTypeAliasSymbol(declaration: TypeAlias, signature: IdSignature): IrTypeAliasSymbol {
        return IrTypeAliasPublicSymbolImpl(signature)
    }

    protected open fun createPrivateTypeAliasSymbol(declaration: TypeAlias): IrTypeAliasSymbol {
        return IrTypeAliasSymbolImpl()
    }

    // ------------------------------------ function ------------------------------------

    fun declareSimpleFunction(declaration: Function, functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction): IrSimpleFunction {
        return declare(
            declaration,
            functionSlice,
            SymbolTable::declareSimpleFunction,
            { createFunctionSymbol(declaration, it) },
            functionFactory
        )
    }

    fun declareSimpleFunctionIfNotExists(
        declaration: Function,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction,
    ): IrSimpleFunction {
        return declareIfNotExist(
            declaration,
            functionSlice,
            SymbolTable::declareSimpleFunctionIfNotExists,
            { createFunctionSymbol(declaration, it) },
            functionFactory
        )
    }

    override fun referenceSimpleFunction(declaration: Function): IrSimpleFunctionSymbol {
        return reference(
            declaration,
            functionSlice,
            SymbolTable::referenceSimpleFunctionImpl,
            ::createFunctionSymbol,
            ::createPublicFunctionSymbol,
            ::createPrivateFunctionSymbol
        )
    }

    override fun referenceDeclaredFunction(declaration: Function): IrSimpleFunctionSymbol {
        fun throwError(): Nothing = error("Function is not declared: $declaration")

        return reference(
            declaration,
            functionSlice,
            SymbolTable::referenceSimpleFunctionImpl,
            { _, _ -> throwError() },
            { _, _ -> throwError() },
            { throwError() },
        )
    }

    protected fun createFunctionSymbol(declaration: Function, signature: IdSignature?): IrSimpleFunctionSymbol {
        return signature?.let { createPublicFunctionSymbol(declaration, signature) } ?: createPrivateFunctionSymbol(declaration)
    }

    protected open fun createPublicFunctionSymbol(declaration: Function, signature: IdSignature): IrSimpleFunctionSymbol {
        return IrSimpleFunctionPublicSymbolImpl(signature)
    }

    protected open fun createPrivateFunctionSymbol(declaration: Function): IrSimpleFunctionSymbol {
        return IrSimpleFunctionSymbolImpl()
    }

    // ------------------------------------ type parameter ------------------------------------

    fun declareGlobalTypeParameter(
        declaration: TypeParameter,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter,
    ): IrTypeParameter {
        return declare(
            declaration,
            globalTypeParameterSlice,
            SymbolTable::declareGlobalTypeParameter,
            { createTypeParameterSymbol(declaration, it) },
            typeParameterFactory
        )
    }

    fun declareGlobalTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        declaration: TypeParameter,
    ): IrTypeParameter {
        return declareGlobalTypeParameter(declaration) {
            defaultTypeParameterFactory(startOffset, endOffset, origin, declaration, it)
        }
    }

    fun declareScopedTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        declaration: TypeParameter,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter = {
            defaultTypeParameterFactory(startOffset, endOffset, origin, declaration, it)
        },
    ): IrTypeParameter {
        return scopedTypeParameterSlice.declare(
            declaration,
            { createTypeParameterSymbol(declaration, calculateSignature(declaration)) },
            typeParameterFactory
        )
    }

    override fun referenceTypeParameter(declaration: TypeParameter): IrTypeParameterSymbol {
        scopedTypeParameterSlice.get(declaration)?.let { return it }
        return reference(
            declaration,
            globalTypeParameterSlice,
            SymbolTable::referenceTypeParameterImpl,
            ::createTypeParameterSymbol,
            ::createPublicTypeParameterSymbol,
            ::createPrivateTypeParameterSymbol
        )
    }

    override fun referenceScopedTypeParameter(declaration: TypeParameter): IrTypeParameterSymbol {
        return scopedTypeParameterSlice.referenced(declaration) { createTypeParameterSymbol(declaration, calculateSignature(declaration)) }
    }

    protected fun createTypeParameterSymbol(declaration: TypeParameter, signature: IdSignature?): IrTypeParameterSymbol {
        return signature?.let { createPublicTypeParameterSymbol(declaration, signature) } ?: createPrivateTypeParameterSymbol(declaration)
    }

    protected open fun createPublicTypeParameterSymbol(declaration: TypeParameter, signature: IdSignature): IrTypeParameterSymbol {
        return IrTypeParameterPublicSymbolImpl(signature)
    }

    protected open fun createPrivateTypeParameterSymbol(declaration: TypeParameter): IrTypeParameterSymbol {
        return IrTypeParameterSymbolImpl()
    }

    protected abstract fun defaultTypeParameterFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        declaration: TypeParameter,
        symbol: IrTypeParameterSymbol,
    ): IrTypeParameter

    // ------------------------------------ value parameter ------------------------------------

    override fun referenceValueParameter(declaration: ValueParameter): IrValueParameterSymbol {
        error("There is no default implementation for any symbol table extension")
    }

    // ------------------------------------ scopes ------------------------------------

    fun enterScope(symbol: IrSymbol) {
        scopedSlices.forEach { it.enterScope(symbol) }
    }

    fun enterScope(owner: IrDeclaration) {
        enterScope(owner.symbol)
    }

    fun leaveScope(symbol: IrSymbol) {
        scopedSlices.forEach { it.leaveScope(symbol) }
    }

    fun leaveScope(owner: IrDeclaration) {
        leaveScope(owner.symbol)
    }

    // ------------------------------------ utilities ------------------------------------

    /**
     * This function is quite messy and doesn't have good contract of what exactly is traversed.
     * Basic idea is it traverse symbols which can be reasonable referered from other module
     *
     * Be careful when using it, and avoid it, except really need.
     */
    @DelicateSymbolTableApi
    fun forEachDeclarationSymbol(block: (IrSymbol) -> Unit) {
        table.forEachDeclarationSymbol(block)

        scriptSlice.forEachSymbol { block(it) }
        classSlice.forEachSymbol { block(it) }
        constructorSlice.forEachSymbol { block(it) }
        enumEntrySlice.forEachSymbol { block(it) }
        fieldSlice.forEachSymbol { block(it) }
        functionSlice.forEachSymbol { block(it) }
        propertySlice.forEachSymbol { block(it) }
        typeAliasSlice.forEachSymbol { block(it) }
        globalTypeParameterSlice.forEachSymbol { block(it) }
    }

    @OptIn(DelicateSymbolTableApi::class)
    val allUnboundSymbols: Set<IrSymbol>
        get() = buildSet {
            fun addUnbound(slice: SymbolTableSlice<*, *, *>) {
                slice.unboundSymbols.filterTo(this) { !it.isBound }
            }
            addAll(table.allUnboundSymbols)

            addUnbound(scriptSlice)
            addUnbound(classSlice)
            addUnbound(constructorSlice)
            addUnbound(enumEntrySlice)
            addUnbound(fieldSlice)
            addUnbound(functionSlice)
            addUnbound(propertySlice)
            addUnbound(typeAliasSlice)
            addUnbound(globalTypeParameterSlice)
        }

    private inline fun <D : Declaration, Symbol : IrBindableSymbol<*, SymbolOwner>, SymbolOwner : IrSymbolOwner> declare(
        declaration: D,
        slice: SymbolTableSlice<D, SymbolOwner, Symbol>,
        declareBySignature: SymbolTable.(IdSignature, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        crossinline symbolFactory: (IdSignature?) -> Symbol,
        noinline ownerFactory: OwnerFactory<Symbol, SymbolOwner>,
        specificCalculateSignature: (D) -> IdSignature? = { calculateSignature(it) }
    ): SymbolOwner {
        return declare(
            declaration,
            slice,
            declareBySignature,
            SymbolTableSlice<D, SymbolOwner, Symbol>::declare,
            symbolFactory,
            ownerFactory,
            specificCalculateSignature
        )
    }

    private inline fun <D : Declaration, Symbol : IrBindableSymbol<*, SymbolOwner>, SymbolOwner : IrSymbolOwner> declareIfNotExist(
        declaration: D,
        slice: SymbolTableSlice<D, SymbolOwner, Symbol>,
        declareBySignature: SymbolTable.(IdSignature, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        crossinline symbolFactory: (IdSignature?) -> Symbol,
        noinline ownerFactory: OwnerFactory<Symbol, SymbolOwner>,
        specificCalculateSignature: (D) -> IdSignature? = { calculateSignature(it) }
    ): SymbolOwner {
        return declare(
            declaration,
            slice,
            declareBySignature,
            SymbolTableSlice<D, SymbolOwner, Symbol>::declareIfNotExists,
            symbolFactory,
            ownerFactory,
            specificCalculateSignature
        )
    }

    private inline fun <D : Declaration, Symbol : IrBindableSymbol<*, SymbolOwner>, SymbolOwner : IrSymbolOwner> declare(
        declaration: D,
        slice: SymbolTableSlice<D, SymbolOwner, Symbol>,
        declareBySignature: SymbolTable.(IdSignature, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        declareByDeclaration: SymbolTableSlice<D, SymbolOwner, Symbol>.(D, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        crossinline symbolFactory: (IdSignature?) -> Symbol,
        noinline ownerFactory: OwnerFactory<Symbol, SymbolOwner>,
        specificCalculateSignature: (D) -> IdSignature?
    ): SymbolOwner {
        return when (val signature = specificCalculateSignature(declaration)) {
            null -> slice.declareByDeclaration(declaration, { symbolFactory(signature) }, ownerFactory)
            else -> table.declareBySignature(signature, { symbolFactory(signature) }, ownerFactory)
        }
    }

    private inline fun <D : Declaration, Symbol : IrBindableSymbol<*, SymbolOwner>, SymbolOwner : IrSymbolOwner> reference(
        declaration: D,
        slice: SymbolTableSlice<D, SymbolOwner, Symbol>,
        referenceBySignature: SymbolTable.(IdSignature, () -> Symbol, () -> Symbol) -> Symbol,
        crossinline symbolFactory: (D, IdSignature?) -> Symbol,
        crossinline publicSymbolFactory: (D, IdSignature) -> Symbol,
        crossinline privateSymbolFactory: (D) -> Symbol,
        specificCalculateSignature: (D) -> IdSignature? = { calculateSignature(it) }
    ): Symbol {
        return when (val signature = specificCalculateSignature(declaration)) {
            null -> slice.referenced(declaration) { symbolFactory(declaration, signature) }
            else -> table.referenceBySignature(
                signature,
                { publicSymbolFactory(declaration, signature) },
                { privateSymbolFactory(declaration) }
            )
        }
    }
}
