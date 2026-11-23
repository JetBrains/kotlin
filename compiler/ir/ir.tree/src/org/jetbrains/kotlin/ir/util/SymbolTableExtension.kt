/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
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

    override fun referenceScript(declaration: Script): IrScriptSymbol {
        return scriptSlice.referenced(declaration) { createScriptSymbol(declaration, signature = null) }
    }

    protected abstract fun defaultScriptFactory(startOffset: Int, endOffset: Int, script: Script, symbol: IrScriptSymbol): IrScript

    protected open fun createScriptSymbol(declaration: Script, signature: IdSignature?): IrScriptSymbol {
        return IrScriptSymbolImpl()
    }

    // ------------------------------------ class ------------------------------------

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
        return IrClassSymbolImpl(signature = signature)
    }

    protected open fun createPrivateClassSymbol(descriptor: Class): IrClassSymbol {
        return IrClassSymbolImpl()
    }

    // ------------------------------------ constructor ------------------------------------

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
        return IrConstructorSymbolImpl(signature = signature)
    }

    protected open fun createPrivateConstructorSymbol(declaration: Constructor): IrConstructorSymbol {
        return IrConstructorSymbolImpl()
    }

    // ------------------------------------ enum entry ------------------------------------

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
        return IrEnumEntrySymbolImpl(signature = signature)
    }

    protected open fun createPrivateEnumEntrySymbol(declaration: Class): IrEnumEntrySymbol {
        return IrEnumEntrySymbolImpl()
    }

    // ------------------------------------ field ------------------------------------
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
        return IrFieldSymbolImpl(signature = signature)
    }

    protected open fun createPrivateFieldSymbol(declaration: Property): IrFieldSymbol {
        return IrFieldSymbolImpl()
    }

    // ------------------------------------ property ------------------------------------

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
        return IrPropertySymbolImpl(signature = signature)
    }

    protected open fun createPrivatePropertySymbol(declaration: Property): IrPropertySymbol {
        return IrPropertySymbolImpl()
    }

    // ------------------------------------ typealias ------------------------------------

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
        return IrTypeAliasSymbolImpl(signature = signature)
    }

    protected open fun createPrivateTypeAliasSymbol(declaration: TypeAlias): IrTypeAliasSymbol {
        return IrTypeAliasSymbolImpl()
    }

    // ------------------------------------ function ------------------------------------
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
        return IrSimpleFunctionSymbolImpl(signature = signature)
    }

    protected open fun createPrivateFunctionSymbol(declaration: Function): IrSimpleFunctionSymbol {
        return IrSimpleFunctionSymbolImpl()
    }

    // ------------------------------------ type parameter ------------------------------------

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
        return IrTypeParameterSymbolImpl(signature = signature)
    }

    protected open fun createPrivateTypeParameterSymbol(declaration: TypeParameter): IrTypeParameterSymbol {
        return IrTypeParameterSymbolImpl()
    }

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
