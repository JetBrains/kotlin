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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.utils.threadLocal

abstract class ReferenceSymbolTableExtension<Class, TypeAlias, Script, Function, Constructor, Property, ValueParameter, TypeParameter> {
    abstract fun referenceScript(descriptor: Script): IrScriptSymbol
    abstract fun referenceClass(descriptor: Class): IrClassSymbol
    abstract fun referenceConstructor(descriptor: Constructor): IrConstructorSymbol
    abstract fun referenceEnumEntry(descriptor: Class): IrEnumEntrySymbol
    abstract fun referenceField(descriptor: Property): IrFieldSymbol
    abstract fun referenceProperty(descriptor: Property): IrPropertySymbol
    abstract fun referenceSimpleFunction(descriptor: Function): IrSimpleFunctionSymbol
    abstract fun referenceDeclaredFunction(descriptor: Function): IrSimpleFunctionSymbol
    abstract fun referenceValueParameter(descriptor: ValueParameter): IrValueParameterSymbol
    abstract fun referenceTypeParameter(classifier: TypeParameter): IrTypeParameterSymbol
    abstract fun referenceScopedTypeParameter(classifier: TypeParameter): IrTypeParameterSymbol
    abstract fun referenceTypeAlias(descriptor: TypeAlias): IrTypeAliasSymbol
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
        descriptor: Script,
    ): IrScript {
        return declare(
            descriptor,
            scriptSlice,
            SymbolTable::declareScript,
            symbolFactory = { createScriptSymbol(descriptor, it) },
            ownerFactory = { defaultScriptFactory(startOffset, endOffset, descriptor, it) }
        )
    }

    override fun referenceScript(descriptor: Script): IrScriptSymbol {
        return scriptSlice.referenced(descriptor) { createScriptSymbol(descriptor, signature = null) }
    }

    protected abstract fun defaultScriptFactory(startOffset: Int, endOffset: Int, script: Script, symbol: IrScriptSymbol): IrScript

    protected abstract fun createScriptSymbol(descriptor: Script, signature: IdSignature?): IrScriptSymbol

    // ------------------------------------ script ------------------------------------

    fun declareClass(descriptor: Class, classFactory: (IrClassSymbol) -> IrClass): IrClass {
        return declare(
            descriptor,
            classSlice,
            SymbolTable::declareClass,
            { createClassSymbol(descriptor, it) },
            classFactory
        )
    }

    fun declareClassIfNotExists(descriptor: Class, classFactory: (IrClassSymbol) -> IrClass): IrClass {
        return declareIfNotExist(
            descriptor,
            classSlice,
            SymbolTable::declareClassIfNotExists,
            { createClassSymbol(descriptor, it) },
            classFactory
        )
    }

    override fun referenceClass(descriptor: Class): IrClassSymbol {
        return reference(
            descriptor,
            classSlice,
            SymbolTable::referenceClassImpl,
            ::createClassSymbol,
            ::createPublicClassSymbol,
            ::createPrivateClassSymbol
        )
    }

    protected fun createClassSymbol(descriptor: Class, signature: IdSignature?): IrClassSymbol {
        return signature?.let { createPublicClassSymbol(descriptor, signature) } ?: createPrivateClassSymbol(descriptor)
    }

    protected abstract fun createPublicClassSymbol(descriptor: Class, signature: IdSignature): IrClassSymbol
    protected abstract fun createPrivateClassSymbol(descriptor: Class): IrClassSymbol

    // ------------------------------------ constructor ------------------------------------

    fun declareConstructor(descriptor: Constructor, constructorFactory: (IrConstructorSymbol) -> IrConstructor): IrConstructor {
        return declare(
            descriptor,
            constructorSlice,
            SymbolTable::declareConstructor,
            { createConstructorSymbol(descriptor, it) },
            constructorFactory
        )
    }

    fun declareConstructorIfNotExists(
        descriptor: Constructor,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor {
        return declareIfNotExist(
            descriptor,
            constructorSlice,
            SymbolTable::declareConstructorIfNotExists,
            { createConstructorSymbol(descriptor, it) },
            constructorFactory
        )
    }

    override fun referenceConstructor(descriptor: Constructor): IrConstructorSymbol {
        return reference(
            descriptor,
            constructorSlice,
            SymbolTable::referenceConstructorImpl,
            ::createConstructorSymbol,
            ::createPublicConstructorSymbol,
            ::createPrivateConstructorSymbol
        )
    }

    protected fun createConstructorSymbol(descriptor: Constructor, signature: IdSignature?): IrConstructorSymbol {
        return signature?.let { createPublicConstructorSymbol(descriptor, signature) } ?: createPrivateConstructorSymbol(descriptor)
    }

    protected abstract fun createPublicConstructorSymbol(descriptor: Constructor, signature: IdSignature): IrConstructorSymbol
    protected abstract fun createPrivateConstructorSymbol(descriptor: Constructor): IrConstructorSymbol

    // ------------------------------------ enum entry ------------------------------------

    fun declareEnumEntry(descriptor: Class, enumEntryFactory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry {
        return declare(
            descriptor,
            enumEntrySlice,
            SymbolTable::declareEnumEntry,
            { createEnumEntrySymbol(descriptor, it) },
            enumEntryFactory,
            ::calculateEnumEntrySignature
        )
    }

    fun declareEnumEntry(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: Class): IrEnumEntry {
        return declareEnumEntry(
            descriptor
        ) { enumEntrySymbol -> defaultEnumEntryFactory(startOffset, endOffset, origin, descriptor, enumEntrySymbol) }
    }

    fun declareEnumEntryIfNotExists(descriptor: Class, enumEntryFactory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry {
        return declareIfNotExist(
            descriptor,
            enumEntrySlice,
            SymbolTable::declareEnumEntryIfNotExists,
            { createEnumEntrySymbol(descriptor, it) },
            enumEntryFactory,
            ::calculateEnumEntrySignature
        )
    }

    override fun referenceEnumEntry(descriptor: Class): IrEnumEntrySymbol {
        return reference(
            descriptor,
            enumEntrySlice,
            SymbolTable::referenceEnumEntryImpl,
            ::createEnumEntrySymbol,
            ::createPublicEnumEntrySymbol,
            ::createPrivateEnumEntrySymbol,
            ::calculateEnumEntrySignature
        )
    }

    protected fun createEnumEntrySymbol(descriptor: Class, signature: IdSignature?): IrEnumEntrySymbol {
        return signature?.let { createPublicEnumEntrySymbol(descriptor, signature) } ?: createPrivateEnumEntrySymbol(descriptor)
    }

    protected abstract fun createPublicEnumEntrySymbol(descriptor: Class, signature: IdSignature): IrEnumEntrySymbol
    protected abstract fun createPrivateEnumEntrySymbol(descriptor: Class): IrEnumEntrySymbol

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
        descriptor: Property,
        type: IrType,
        visibility: DescriptorVisibility? = null,
        fieldFactory: (IrFieldSymbol) -> IrField = {
            defaultFieldFactory(
                startOffset,
                endOffset,
                origin,
                descriptor,
                type,
                visibility,
                it
            )
        },
    ): IrField {
        return declare(
            descriptor,
            fieldSlice,
            SymbolTable::declareField,
            { createFieldSymbol(descriptor, it) },
            fieldFactory,
            ::calculateFieldSignature
        )
    }

    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: Property,
        type: IrType,
        irInitializer: IrExpressionBody?,
    ): IrField {
        return declareField(startOffset, endOffset, origin, descriptor, type).apply {
            initializer = irInitializer
        }
    }

    override fun referenceField(descriptor: Property): IrFieldSymbol {
        return reference(
            descriptor,
            fieldSlice,
            SymbolTable::referenceFieldImpl,
            ::createFieldSymbol,
            ::createPublicFieldSymbol,
            ::createPrivateFieldSymbol,
            ::calculateFieldSignature
        )
    }

    protected fun createFieldSymbol(descriptor: Property, signature: IdSignature?): IrFieldSymbol {
        return signature?.let { createPublicFieldSymbol(descriptor, signature) } ?: createPrivateFieldSymbol(descriptor)
    }

    protected abstract fun createPublicFieldSymbol(descriptor: Property, signature: IdSignature): IrFieldSymbol
    protected abstract fun createPrivateFieldSymbol(descriptor: Property): IrFieldSymbol

    protected abstract fun defaultFieldFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: Property,
        type: IrType,
        visibility: DescriptorVisibility?,
        symbol: IrFieldSymbol,
    ): IrField

    // ------------------------------------ property ------------------------------------

    fun declareProperty(descriptor: Property, propertyFactory: (IrPropertySymbol) -> IrProperty): IrProperty {
        return declare(
            descriptor,
            propertySlice,
            SymbolTable::declareProperty,
            { createPropertySymbol(descriptor, it) },
            propertyFactory
        )
    }

    fun declareProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: Property,
        isDelegated: Boolean
    ): IrProperty {
        return declareProperty(descriptor) { propertySymbol ->
            defaultPropertyFactory(startOffset, endOffset, origin, descriptor, isDelegated, propertySymbol)
        }
    }

    fun declarePropertyIfNotExists(descriptor: Property, propertyFactory: (IrPropertySymbol) -> IrProperty): IrProperty {
        return declareIfNotExist(
            descriptor,
            propertySlice,
            SymbolTable::declarePropertyIfNotExists,
            { createPropertySymbol(descriptor, it) },
            propertyFactory
        )
    }

    override fun referenceProperty(descriptor: Property): IrPropertySymbol {
        return reference(
            descriptor,
            propertySlice,
            SymbolTable::referencePropertyImpl,
            ::createPropertySymbol,
            ::createPublicPropertySymbol,
            ::createPrivatePropertySymbol
        )
    }

    protected fun createPropertySymbol(descriptor: Property, signature: IdSignature?): IrPropertySymbol {
        return signature?.let { createPublicPropertySymbol(descriptor, signature) } ?: createPrivatePropertySymbol(descriptor)
    }

    protected abstract fun createPublicPropertySymbol(descriptor: Property, signature: IdSignature): IrPropertySymbol
    protected abstract fun createPrivatePropertySymbol(descriptor: Property): IrPropertySymbol

    protected abstract fun defaultPropertyFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: Property,
        isDelegated: Boolean,
        symbol: IrPropertySymbol,
    ): IrProperty

    // ------------------------------------ typealias ------------------------------------

    fun declareTypeAlias(descriptor: TypeAlias, typeAliasFactory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias {
        return declare(
            descriptor,
            typeAliasSlice,
            SymbolTable::declareTypeAlias,
            { createTypeAliasSymbol(descriptor, it) },
            typeAliasFactory
        )
    }

    fun declareTypeAliasIfNotExists(descriptor: TypeAlias, typeAliasFactory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias {
        return declareIfNotExist(
            descriptor,
            typeAliasSlice,
            SymbolTable::declareTypeAliasIfNotExists,
            { createTypeAliasSymbol(descriptor, it) },
            typeAliasFactory
        )
    }

    override fun referenceTypeAlias(descriptor: TypeAlias): IrTypeAliasSymbol {
        return reference(
            descriptor,
            typeAliasSlice,
            SymbolTable::referenceTypeAliasImpl,
            ::createTypeAliasSymbol,
            ::createPublicTypeAliasSymbol,
            ::createPrivateTypeAliasSymbol
        )
    }

    protected fun createTypeAliasSymbol(descriptor: TypeAlias, signature: IdSignature?): IrTypeAliasSymbol {
        return signature?.let { createPublicTypeAliasSymbol(descriptor, signature) } ?: createPrivateTypeAliasSymbol(descriptor)
    }

    protected abstract fun createPublicTypeAliasSymbol(descriptor: TypeAlias, signature: IdSignature): IrTypeAliasSymbol
    protected abstract fun createPrivateTypeAliasSymbol(descriptor: TypeAlias): IrTypeAliasSymbol

    // ------------------------------------ function ------------------------------------

    fun declareSimpleFunction(descriptor: Function, functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction): IrSimpleFunction {
        return declare(
            descriptor,
            functionSlice,
            SymbolTable::declareSimpleFunction,
            { createFunctionSymbol(descriptor, it) },
            functionFactory
        )
    }

    fun declareSimpleFunctionIfNotExists(
        descriptor: Function,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction,
    ): IrSimpleFunction {
        return declareIfNotExist(
            descriptor,
            functionSlice,
            SymbolTable::declareSimpleFunctionIfNotExists,
            { createFunctionSymbol(descriptor, it) },
            functionFactory
        )
    }

    override fun referenceSimpleFunction(descriptor: Function): IrSimpleFunctionSymbol {
        return reference(
            descriptor,
            functionSlice,
            SymbolTable::referenceSimpleFunctionImpl,
            ::createFunctionSymbol,
            ::createPublicFunctionSymbol,
            ::createPrivateFunctionSymbol
        )
    }

    override fun referenceDeclaredFunction(descriptor: Function): IrSimpleFunctionSymbol {
        fun throwError(): Nothing = error("Function is not declared: $descriptor")

        return reference(
            descriptor,
            functionSlice,
            SymbolTable::referenceSimpleFunctionImpl,
            { _, _ -> throwError() },
            { _, _ -> throwError() },
            { throwError() },
        )
    }

    protected fun createFunctionSymbol(descriptor: Function, signature: IdSignature?): IrSimpleFunctionSymbol {
        return signature?.let { createPublicFunctionSymbol(descriptor, signature) } ?: createPrivateFunctionSymbol(descriptor)
    }

    protected abstract fun createPublicFunctionSymbol(descriptor: Function, signature: IdSignature): IrSimpleFunctionSymbol
    protected abstract fun createPrivateFunctionSymbol(descriptor: Function): IrSimpleFunctionSymbol

    // ------------------------------------ type parameter ------------------------------------

    fun declareGlobalTypeParameter(
        descriptor: TypeParameter,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter,
    ): IrTypeParameter {
        return declare(
            descriptor,
            globalTypeParameterSlice,
            SymbolTable::declareGlobalTypeParameter,
            { createTypeParameterSymbol(descriptor, it) },
            typeParameterFactory
        )
    }

    fun declareGlobalTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameter,
    ): IrTypeParameter {
        return declareGlobalTypeParameter(descriptor) {
            defaultTypeParameterFactory(startOffset, endOffset, origin, descriptor, it)
        }
    }

    fun declareScopedTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameter,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter = {
            defaultTypeParameterFactory(startOffset, endOffset, origin, descriptor, it)
        },
    ): IrTypeParameter {
        return scopedTypeParameterSlice.declare(
            descriptor,
            { createTypeParameterSymbol(descriptor, calculateSignature(descriptor)) },
            typeParameterFactory
        )
    }

    override fun referenceTypeParameter(classifier: TypeParameter): IrTypeParameterSymbol {
        scopedTypeParameterSlice.get(classifier)?.let { return it }
        return reference(
            classifier,
            globalTypeParameterSlice,
            SymbolTable::referenceTypeParameterImpl,
            ::createTypeParameterSymbol,
            ::createPublicTypeParameterSymbol,
            ::createPrivateTypeParameterSymbol
        )
    }

    override fun referenceScopedTypeParameter(classifier: TypeParameter): IrTypeParameterSymbol {
        return scopedTypeParameterSlice.referenced(classifier) { createTypeParameterSymbol(classifier, calculateSignature(classifier)) }
    }

    protected fun createTypeParameterSymbol(descriptor: TypeParameter, signature: IdSignature?): IrTypeParameterSymbol {
        return signature?.let { createPublicTypeParameterSymbol(descriptor, signature) } ?: createPrivateTypeParameterSymbol(descriptor)
    }

    protected abstract fun createPublicTypeParameterSymbol(descriptor: TypeParameter, signature: IdSignature): IrTypeParameterSymbol
    protected abstract fun createPrivateTypeParameterSymbol(descriptor: TypeParameter): IrTypeParameterSymbol

    protected abstract fun defaultTypeParameterFactory(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameter,
        symbol: IrTypeParameterSymbol,
    ): IrTypeParameter

    // ------------------------------------ value parameter ------------------------------------

    override fun referenceValueParameter(descriptor: ValueParameter): IrValueParameterSymbol {
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
        descriptor: D,
        slice: SymbolTableSlice<D, SymbolOwner, Symbol>,
        declareBySignature: SymbolTable.(IdSignature, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        crossinline symbolFactory: (IdSignature?) -> Symbol,
        noinline ownerFactory: OwnerFactory<Symbol, SymbolOwner>,
        specificCalculateSignature: (D) -> IdSignature? = { calculateSignature(it) }
    ): SymbolOwner {
        return declare(
            descriptor,
            slice,
            declareBySignature,
            SymbolTableSlice<D, SymbolOwner, Symbol>::declare,
            symbolFactory,
            ownerFactory,
            specificCalculateSignature
        )
    }

    private inline fun <D : Declaration, Symbol : IrBindableSymbol<*, SymbolOwner>, SymbolOwner : IrSymbolOwner> declareIfNotExist(
        descriptor: D,
        slice: SymbolTableSlice<D, SymbolOwner, Symbol>,
        declareBySignature: SymbolTable.(IdSignature, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        crossinline symbolFactory: (IdSignature?) -> Symbol,
        noinline ownerFactory: OwnerFactory<Symbol, SymbolOwner>,
        specificCalculateSignature: (D) -> IdSignature? = { calculateSignature(it) }
    ): SymbolOwner {
        return declare(
            descriptor,
            slice,
            declareBySignature,
            SymbolTableSlice<D, SymbolOwner, Symbol>::declareIfNotExists,
            symbolFactory,
            ownerFactory,
            specificCalculateSignature
        )
    }

    private inline fun <D : Declaration, Symbol : IrBindableSymbol<*, SymbolOwner>, SymbolOwner : IrSymbolOwner> declare(
        descriptor: D,
        slice: SymbolTableSlice<D, SymbolOwner, Symbol>,
        declareBySignature: SymbolTable.(IdSignature, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        declareByDeclaration: SymbolTableSlice<D, SymbolOwner, Symbol>.(D, () -> Symbol, OwnerFactory<Symbol, SymbolOwner>) -> SymbolOwner,
        crossinline symbolFactory: (IdSignature?) -> Symbol,
        noinline ownerFactory: OwnerFactory<Symbol, SymbolOwner>,
        specificCalculateSignature: (D) -> IdSignature?
    ): SymbolOwner {
        return when (val signature = specificCalculateSignature(descriptor)) {
            null -> slice.declareByDeclaration(descriptor, { symbolFactory(signature) }, ownerFactory)
            else -> table.declareBySignature(signature, { symbolFactory(signature) }, ownerFactory)
        }
    }

    private inline fun <D : Declaration, Symbol : IrBindableSymbol<*, SymbolOwner>, SymbolOwner : IrSymbolOwner> reference(
        descriptor: D,
        slice: SymbolTableSlice<D, SymbolOwner, Symbol>,
        referenceBySignature: SymbolTable.(IdSignature, () -> Symbol, () -> Symbol) -> Symbol,
        crossinline symbolFactory: (D, IdSignature?) -> Symbol,
        crossinline publicSymbolFactory: (D, IdSignature) -> Symbol,
        crossinline privateSymbolFactory: (D) -> Symbol,
        specificCalculateSignature: (D) -> IdSignature? = { calculateSignature(it) }
    ): Symbol {
        return when (val signature = specificCalculateSignature(descriptor)) {
            null -> slice.referenced(descriptor) { symbolFactory(descriptor, signature) }
            else -> table.referenceBySignature(
                signature,
                { publicSymbolFactory(descriptor, signature) },
                { privateSymbolFactory(descriptor) }
            )
        }
    }
}
