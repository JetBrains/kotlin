/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrScriptImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazySymbolTable
import org.jetbrains.kotlin.ir.descriptors.IrBasedClassDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.utils.threadLocal

interface ReferenceSymbolTable {
    @ObsoleteDescriptorBasedAPI
    fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol
    fun referenceClass(signature: IdSignature): IrClassSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceScript(descriptor: ScriptDescriptor): IrScriptSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol
    fun referenceConstructor(signature: IdSignature): IrConstructorSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol
    fun referenceEnumEntry(signature: IdSignature): IrEnumEntrySymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceField(descriptor: PropertyDescriptor): IrFieldSymbol
    fun referenceField(signature: IdSignature): IrFieldSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol
    fun referenceProperty(signature: IdSignature): IrPropertySymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceProperty(descriptor: PropertyDescriptor, generate: () -> IrProperty): IrProperty

    @ObsoleteDescriptorBasedAPI
    fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol
    fun referenceSimpleFunction(signature: IdSignature): IrSimpleFunctionSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceDeclaredFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol
    fun referenceTypeParameter(signature: IdSignature): IrTypeParameterSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceScopedTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceVariable(descriptor: VariableDescriptor): IrVariableSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol
    fun referenceTypeAlias(signature: IdSignature): IrTypeAliasSymbol

    fun enterScope(symbol: IrSymbol)
    fun enterScope(owner: IrDeclaration)

    fun leaveScope(symbol: IrSymbol)
    fun leaveScope(owner: IrDeclaration)
}

open class SymbolTable(
    val signaturer: IdSignatureComposer,
    val irFactory: IrFactory,
    val nameProvider: NameProvider = NameProvider.DEFAULT
) : ReferenceSymbolTable {

    val lock = IrLock()

    @Suppress("LeakingThis")
    val lazyWrapper = IrLazySymbolTable(this)

    @Suppress("DuplicatedCode")
    private abstract class SymbolTableBase<Descriptor, SymbolOwner, Symbol>(val lock: IrLock)
            where Descriptor : DeclarationDescriptor,
                  SymbolOwner : IrSymbolOwner,
                  Symbol : IrBindableSymbol<Descriptor, SymbolOwner> {
        /**
         * With the partial linkage turned on it's hard to predict whether a newly created [IrSymbol] that is added to the [SymbolTable]
         * via one of referenceXXX() calls will or won't be bound to some declaration. The latter may happen in certain cases,
         * for example when the symbol refers from an IR expression to a non-top level declaration that was removed in newer version
         * of Kotlin library (KLIB). Unless such symbol is registered as "probably unbound" it remains invisible for the linkage process.
         *
         * The optimization that allows to reference symbols without registering them as "probably unbound" is fragile. It's better
         * to avoid calling any referenceXXX(reg = false) functions. Instead, wherever it is s suitable it is recommended to use one
         * of the appropriate declareXXX() calls.
         *
         * For the future: Consider implementing the optimization once again for the new "flat" ID signatures.
         */
        val unboundSymbols = linkedSetOf<Symbol>()

        protected abstract fun signature(descriptor: Descriptor): IdSignature?

        // TODO: consider replacing paired get/set calls by java.util.Map.computeIfAbsent() which would be more performant
        abstract fun get(descriptor: Descriptor, signature: IdSignature?): Symbol?
        abstract fun set(symbol: Symbol)
        abstract fun get(signature: IdSignature): Symbol?
        abstract fun set(signature: IdSignature, symbol: Symbol)

        inline fun declare(
            descriptor: Descriptor,
            createSymbol: (IdSignature?) -> Symbol,
            createOwner: (Symbol) -> SymbolOwner,
        ): SymbolOwner {
            synchronized(lock) {
                val signature = signature(descriptor)
                val existing = get(descriptor, signature)
                val symbol = if (existing == null) {
                    checkOriginal(descriptor)
                    val new = createSymbol(signature)
                    set(new)
                    new
                } else {
                    unboundSymbols.remove(existing)
                    existing
                }
                return createOwnerSafe(symbol, createOwner)
            }
        }

        inline fun declare(signature: IdSignature, createSymbol: () -> Symbol, createOwner: (Symbol) -> SymbolOwner): SymbolOwner {
            synchronized(lock) {
                val existing = get(signature)
                val symbol = if (existing == null) {
                    createSymbol()
                } else {
                    unboundSymbols.remove(existing)
                    existing
                }
                return createOwnerSafe(symbol, createOwner)
                    .also {
                        // TODO: try to get rid of this
                        set(symbol)
                    }
            }
        }

        inline fun declareIfNotExists(
            descriptor: Descriptor,
            createSymbol: (IdSignature?) -> Symbol,
            createOwner: (Symbol) -> SymbolOwner,
        ): SymbolOwner {
            synchronized(lock) {
                val signature = signature(descriptor)
                val existing = get(descriptor, signature)
                val symbol = if (existing == null) {
                    checkOriginal(descriptor)
                    val new = createSymbol(signature)
                    set(new)
                    new
                } else {
                    if (existing.isBound) {
                        unboundSymbols.remove(existing)
                        return existing.owner
                    }
                    existing
                }
                return createOwnerSafe(symbol, createOwner)
            }
        }

        inline fun declare(
            signature: IdSignature,
            descriptor: Descriptor?,
            createSymbol: () -> Symbol,
            createOwner: (Symbol) -> SymbolOwner,
        ): SymbolOwner {
            synchronized(lock) {
                val existing = get(signature)
                val symbol = if (existing == null) {
                    checkOriginal(descriptor)
                    val new = createSymbol()
                    set(new)
                    new
                } else {
                    unboundSymbols.remove(existing)
                    existing
                }
                return createOwnerSafe(symbol, createOwner)
            }
        }

        inline fun referenced(descriptor: Descriptor, orElse: (IdSignature?) -> Symbol): Symbol {
            synchronized(lock) {
                val signature = signature(descriptor)
                return get(descriptor, signature) ?: run {
                    checkOriginal(descriptor)
                    val new = orElse(signature)
                    assert(unboundSymbols.add(new)) { "Symbol for $new was already referenced" }
                    set(new)
                    new
                }
            }
        }

        inline fun referenced(signature: IdSignature, orElse: () -> Symbol): Symbol {
            synchronized(lock) {
                return get(signature) ?: run {
                    val new = orElse()
                    assert(unboundSymbols.add(new)) { "Symbol for ${new.signature} was already referenced" }
                    set(new)
                    new
                }
            }
        }

        private fun checkOriginal(descriptor: Descriptor?) {
            @Suppress("UNCHECKED_CAST")
            val descriptor0 = descriptor?.original as Descriptor
            assert(descriptor0 === descriptor) {
                "Non-original descriptor in declaration: $descriptor\n\tExpected: $descriptor0"
            }
        }

        protected inline fun createOwnerSafe(symbol: Symbol, createOwner: (Symbol) -> SymbolOwner): SymbolOwner {
            val owner = createOwner(symbol)
            require(symbol.isBound)
            require(symbol.owner === owner) {
                "Attempt to rebind an IR symbol or to re-create its owner: old owner ${symbol.owner.render()}, new owner ${owner.render()}"
            }
            return owner
        }
    }

    private open inner class FlatSymbolTable<Descriptor, SymbolOwner, Symbol> : SymbolTableBase<Descriptor, SymbolOwner, Symbol>(lock)
            where Descriptor : DeclarationDescriptor,
                  SymbolOwner : IrSymbolOwner,
                  Symbol : IrBindableSymbol<Descriptor, SymbolOwner> {
        val descriptorToSymbol = hashMapOf<Descriptor, Symbol>()
        val idSignatureToSymbol = hashMapOf<IdSignature, Symbol>()

        override fun signature(descriptor: Descriptor): IdSignature? =
            signaturer.composeSignature(descriptor)

        override fun get(descriptor: Descriptor, signature: IdSignature?): Symbol? =
            if (signature != null) {
                idSignatureToSymbol[signature]
            } else {
                descriptorToSymbol[descriptor]
            }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        override fun set(symbol: Symbol) {
            val signature = symbol.signature
            if (signature != null) {
                idSignatureToSymbol[signature] = symbol
            } else if (symbol.hasDescriptor) {
                descriptorToSymbol[symbol.descriptor] = symbol
            }
        }

        override fun get(signature: IdSignature): Symbol? = idSignatureToSymbol[signature]

        override fun set(signature: IdSignature, symbol: Symbol) {
            idSignatureToSymbol[signature] = symbol
        }
    }

    private inner class EnumEntrySymbolTable : FlatSymbolTable<ClassDescriptor, IrEnumEntry, IrEnumEntrySymbol>() {
        override fun signature(descriptor: ClassDescriptor): IdSignature? = signaturer.composeEnumEntrySignature(descriptor)
    }

    private inner class FieldSymbolTable : FlatSymbolTable<PropertyDescriptor, IrField, IrFieldSymbol>() {
        override fun signature(descriptor: PropertyDescriptor): IdSignature? = signaturer.composeFieldSignature(descriptor)
    }

    private inner class ScopedSymbolTable<Descriptor, SymbolOwner, Symbol> : SymbolTableBase<Descriptor, SymbolOwner, Symbol>(lock)
            where Descriptor : DeclarationDescriptor,
                  SymbolOwner : IrSymbolOwner,
                  Symbol : IrBindableSymbol<Descriptor, SymbolOwner> {
        inner class Scope(val owner: IrSymbol, val parent: Scope?) {
            private val descriptorToSymbol = hashMapOf<Descriptor, Symbol>()
            private val idSignatureToSymbol = hashMapOf<IdSignature, Symbol>()

            private fun getByDescriptor(descriptor: Descriptor): Symbol? =
                descriptorToSymbol[descriptor] ?: parent?.getByDescriptor(descriptor)

            private fun getByIdSignature(signature: IdSignature): Symbol? =
                idSignatureToSymbol[signature] ?: parent?.getByIdSignature(signature)

            operator fun get(descriptor: Descriptor, signature: IdSignature?): Symbol? =
                if (signature != null) {
                    getByIdSignature(signature)
                } else {
                    getByDescriptor(descriptor)
                }

            fun getLocal(descriptor: Descriptor) = descriptorToSymbol[descriptor]

            @OptIn(ObsoleteDescriptorBasedAPI::class)
            operator fun set(descriptor: Descriptor, symbol: Symbol) {
                symbol.signature?.let {
                    require(descriptor is TypeParameterDescriptor)
                    idSignatureToSymbol[it] = symbol
                } ?: run {
                    assert(symbol.hasDescriptor)
                    descriptorToSymbol[symbol.descriptor] = symbol
                }
            }

            operator fun get(signature: IdSignature): Symbol? =
                idSignatureToSymbol[signature] ?: parent?.get(signature)

            operator fun set(signature: IdSignature, symbol: Symbol) {
                idSignatureToSymbol[signature] = symbol
            }

            fun dumpTo(stringBuilder: StringBuilder): StringBuilder =
                stringBuilder.also {
                    it.append("owner=")
                    it.append(owner)
                    it.append("; ")
                    descriptorToSymbol.keys.joinTo(prefix = "[", postfix = "]", buffer = it)
                    it.append('\n')
                    parent?.dumpTo(it)
                }

            fun dump(): String = dumpTo(StringBuilder()).toString()
        }

        private var currentScope: Scope? = null

        override fun signature(descriptor: Descriptor): IdSignature? =
            signaturer.composeSignature(descriptor)

        override fun get(descriptor: Descriptor, signature: IdSignature?): Symbol? {
            val scope = currentScope ?: return null
            return scope[descriptor, signature]
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        override fun set(symbol: Symbol) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[symbol.descriptor] = symbol
        }

        override fun get(signature: IdSignature): Symbol? {
            val scope = currentScope ?: return null
            return scope[signature]
        }

        override fun set(signature: IdSignature, symbol: Symbol) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[signature] = symbol
        }

        inline fun declareLocal(descriptor: Descriptor, createSymbol: () -> Symbol, createOwner: (Symbol) -> SymbolOwner): SymbolOwner {
            val scope = currentScope ?: throw AssertionError("No active scope")
            val symbol = scope.getLocal(descriptor) ?: createSymbol().also { scope[descriptor] = it }
            return createOwnerSafe(symbol, createOwner)
        }

        fun introduceLocal(descriptor: Descriptor, symbol: Symbol) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[descriptor, signature(descriptor)]?.let {
                throw AssertionError("$descriptor is already bound to $it")
            }
            scope[descriptor] = symbol
        }

        fun enterScope(owner: IrSymbol) {
            currentScope = Scope(owner, currentScope)
        }

        fun leaveScope(owner: IrSymbol) {
            currentScope?.owner.let {
                assert(it == owner) { "Unexpected leaveScope: owner=$owner, currentScope.owner=$it" }
            }

            currentScope = currentScope?.parent

            if (currentScope != null && unboundSymbols.isNotEmpty()) {
                @OptIn(ObsoleteDescriptorBasedAPI::class)
                throw AssertionError("Local scope contains unbound symbols: ${unboundSymbols.joinToString { it.descriptor.toString() }}")
            }
        }

        fun dump(): String =
            currentScope?.dump() ?: "<none>"
    }

    private val externalPackageFragmentTable =
        FlatSymbolTable<PackageFragmentDescriptor, IrExternalPackageFragment, IrExternalPackageFragmentSymbol>()
    private val scriptSymbolTable = FlatSymbolTable<ScriptDescriptor, IrScript, IrScriptSymbol>()
    private val classSymbolTable = FlatSymbolTable<ClassDescriptor, IrClass, IrClassSymbol>()
    private val constructorSymbolTable = FlatSymbolTable<ClassConstructorDescriptor, IrConstructor, IrConstructorSymbol>()
    private val enumEntrySymbolTable = EnumEntrySymbolTable()
    private val fieldSymbolTable = FieldSymbolTable()
    private val simpleFunctionSymbolTable = FlatSymbolTable<FunctionDescriptor, IrSimpleFunction, IrSimpleFunctionSymbol>()
    private val propertySymbolTable = FlatSymbolTable<PropertyDescriptor, IrProperty, IrPropertySymbol>()
    private val typeAliasSymbolTable = FlatSymbolTable<TypeAliasDescriptor, IrTypeAlias, IrTypeAliasSymbol>()

    private val globalTypeParameterSymbolTable = FlatSymbolTable<TypeParameterDescriptor, IrTypeParameter, IrTypeParameterSymbol>()
    private val scopedTypeParameterSymbolTable by threadLocal {
        ScopedSymbolTable<TypeParameterDescriptor, IrTypeParameter, IrTypeParameterSymbol>()
    }
    private val valueParameterSymbolTable by threadLocal {
        ScopedSymbolTable<ParameterDescriptor, IrValueParameter, IrValueParameterSymbol>()
    }
    private val variableSymbolTable by threadLocal {
        ScopedSymbolTable<VariableDescriptor, IrVariable, IrVariableSymbol>()
    }
    private val localDelegatedPropertySymbolTable by threadLocal {
        ScopedSymbolTable<VariableDescriptorWithAccessors, IrLocalDelegatedProperty, IrLocalDelegatedPropertySymbol>()
    }
    private val scopedSymbolTables by threadLocal {
        listOf(valueParameterSymbolTable, variableSymbolTable, scopedTypeParameterSymbolTable, localDelegatedPropertySymbolTable)
    }

    @ObsoleteDescriptorBasedAPI
    fun referenceExternalPackageFragment(descriptor: PackageFragmentDescriptor) =
        externalPackageFragmentTable.referenced(descriptor) { IrExternalPackageFragmentSymbolImpl(descriptor) }

    @ObsoleteDescriptorBasedAPI
    fun declareExternalPackageFragment(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        return externalPackageFragmentTable.declare(
            descriptor,
            { IrExternalPackageFragmentSymbolImpl(descriptor) },
            { IrExternalPackageFragmentImpl(it, descriptor.fqName) }
        )
    }

    @ObsoleteDescriptorBasedAPI
    fun declareExternalPackageFragmentIfNotExists(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        return externalPackageFragmentTable.declareIfNotExists(
            descriptor,
            { IrExternalPackageFragmentSymbolImpl(descriptor) },
            { IrExternalPackageFragmentImpl(it, descriptor.fqName) }
        )
    }

    @ObsoleteDescriptorBasedAPI
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

    @ObsoleteDescriptorBasedAPI
    fun declareScript(
        startOffset: Int,
        endOffset: Int,
        descriptor: ScriptDescriptor,
        scriptFactory: (IrScriptSymbol) -> IrScript = { symbol: IrScriptSymbol ->
            IrScriptImpl(symbol, nameProvider.nameForDeclaration(descriptor), irFactory, startOffset, endOffset)
        }
    ): IrScript {
        return scriptSymbolTable.declare(
            descriptor,
            { IrScriptSymbolImpl(descriptor) },
            scriptFactory
        )
    }

    fun declareScript(
        signature: IdSignature,
        symbolFactory: () -> IrScriptSymbol,
        scriptFactory: (IrScriptSymbol) -> IrScript
    ): IrScript {
        return scriptSymbolTable.declare(
            signature,
            symbolFactory,
            scriptFactory
        )
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceScript(descriptor: ScriptDescriptor): IrScriptSymbol =
        scriptSymbolTable.referenced(descriptor) { IrScriptSymbolImpl(descriptor) }

    private fun createClassSymbol(descriptor: ClassDescriptor, signature: IdSignature?): IrClassSymbol =
        signature?.let { IrClassPublicSymbolImpl(it, descriptor) } ?: IrClassSymbolImpl(descriptor)

    @ObsoleteDescriptorBasedAPI
    fun declareClass(
        descriptor: ClassDescriptor,
        classFactory: (IrClassSymbol) -> IrClass
    ): IrClass {
        return classSymbolTable.declare(
            descriptor,
            { createClassSymbol(descriptor, signaturer.composeSignature(descriptor)) },
            classFactory
        )
    }

    fun declareClass(
        signature: IdSignature,
        symbolFactory: () -> IrClassSymbol,
        classFactory: (IrClassSymbol) -> IrClass
    ): IrClass {
        return classSymbolTable.declare(
            signature,
            symbolFactory,
            classFactory
        )
    }

    fun declareClassWithSignature(signature: IdSignature, symbol: IrClassSymbol) {
        classSymbolTable.set(signature, symbol)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareClassIfNotExists(descriptor: ClassDescriptor, classFactory: (IrClassSymbol) -> IrClass): IrClass =
        classSymbolTable.declareIfNotExists(descriptor, { signature -> createClassSymbol(descriptor, signature) }, classFactory)

    // Note: used in native
    @ObsoleteDescriptorBasedAPI
    @Suppress("unused")
    fun declareClassFromLinker(descriptor: ClassDescriptor, signature: IdSignature, factory: (IrClassSymbol) -> IrClass): IrClass {
        return classSymbolTable.run {
            if (signature.isPubliclyVisible) {
                declare(signature, descriptor, { IrClassPublicSymbolImpl(signature, descriptor) }, factory)
            } else {
                declare(descriptor, { IrClassSymbolImpl(descriptor) }, factory)
            }
        }
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol =
        @Suppress("Reformat")
        // This is needed for cases like kt46069.kt, where psi2ir creates descriptor-less IR elements for adapted function references.
        // In JVM IR, symbols are linked via descriptors by default, so for an adapted function reference, an IrBasedClassDescriptor
        // is created for any classifier used in the function parameter/return types. Any attempt to translate such type to IrType goes
        // to this method, which puts the descriptor into unboundClasses, which causes an assertion failure later because we won't bind
        // such symbol anywhere.
        // TODO: maybe there's a better solution.
        if (descriptor is IrBasedClassDescriptor)
            descriptor.owner.symbol
        else
            classSymbolTable.referenced(descriptor) { signature -> createClassSymbol(descriptor, signature) }

    fun referenceClass(
        signature: IdSignature,
        symbolFactory: () -> IrClassSymbol,
        classFactory: (IrClassSymbol) -> IrClass
    ) =
        classSymbolTable.referenced(signature) { declareClass(signature, symbolFactory, classFactory).symbol }

    override fun referenceClass(signature: IdSignature): IrClassSymbol =
        classSymbolTable.run {
            if (signature.isPubliclyVisible) referenced(signature) { IrClassPublicSymbolImpl(signature) }
            else IrClassSymbolImpl().also {
                it.privateSignature = signature
            }
        }

    val unboundClasses: Set<IrClassSymbol> get() = classSymbolTable.unboundSymbols

    private fun createConstructorSymbol(descriptor: ClassConstructorDescriptor, signature: IdSignature?): IrConstructorSymbol =
        signature?.let { IrConstructorPublicSymbolImpl(it, descriptor) } ?: IrConstructorSymbolImpl(descriptor)

    @ObsoleteDescriptorBasedAPI
    fun declareConstructor(
        descriptor: ClassConstructorDescriptor,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor
    ): IrConstructor =
        constructorSymbolTable.declare(
            descriptor,
            { signature -> createConstructorSymbol(descriptor, signature) },
            constructorFactory
        )

    fun declareConstructor(
        signature: IdSignature,
        symbolFactory: () -> IrConstructorSymbol,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor
    ): IrConstructor {
        return constructorSymbolTable.declare(
            signature,
            symbolFactory,
            constructorFactory
        )
    }

    @ObsoleteDescriptorBasedAPI
    fun declareConstructorIfNotExists(
        descriptor: ClassConstructorDescriptor,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor,
    ): IrConstructor =
        constructorSymbolTable.declareIfNotExists(
            descriptor,
            { signature -> createConstructorSymbol(descriptor, signature) },
            constructorFactory
        )

    fun declareConstructorWithSignature(signature: IdSignature, symbol: IrConstructorSymbol) {
        constructorSymbolTable.set(signature, symbol)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol =
        constructorSymbolTable.referenced(descriptor) { signature -> createConstructorSymbol(descriptor, signature) }

    fun referenceConstructorIfAny(signature: IdSignature): IrConstructorSymbol? =
        constructorSymbolTable.get(signature)

    // Note: used in native
    @ObsoleteDescriptorBasedAPI
    @Suppress("unused")
    fun declareConstructorFromLinker(
        descriptor: ClassConstructorDescriptor,
        signature: IdSignature,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor
    ): IrConstructor {
        return constructorSymbolTable.run {
            if (signature.isPubliclyVisible) {
                declare(signature, descriptor, { IrConstructorPublicSymbolImpl(signature, descriptor) }, constructorFactory)
            } else {
                declare(descriptor, { IrConstructorSymbolImpl(descriptor) }, constructorFactory)
            }
        }
    }

    override fun referenceConstructor(signature: IdSignature): IrConstructorSymbol =
        constructorSymbolTable.run {
            if (signature.isPubliclyVisible) referenced(signature) { IrConstructorPublicSymbolImpl(signature) }
            else IrConstructorSymbolImpl()
        }

    val unboundConstructors: Set<IrConstructorSymbol> get() = constructorSymbolTable.unboundSymbols

    private fun createEnumEntrySymbol(descriptor: ClassDescriptor, signature: IdSignature?): IrEnumEntrySymbol =
        signature?.let { IrEnumEntryPublicSymbolImpl(it, descriptor) } ?: IrEnumEntrySymbolImpl(descriptor)

    @ObsoleteDescriptorBasedAPI
    fun declareEnumEntry(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry = {
            irFactory.createEnumEntry(startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(descriptor))
        }
    ): IrEnumEntry =
        enumEntrySymbolTable.declare(
            descriptor,
            { signature -> createEnumEntrySymbol(descriptor, signature) },
            factory
        )

    fun declareEnumEntry(
        signature: IdSignature,
        symbolFactory: () -> IrEnumEntrySymbol,
        enumEntryFactory: (IrEnumEntrySymbol) -> IrEnumEntry
    ): IrEnumEntry = enumEntrySymbolTable.declare(signature, symbolFactory, enumEntryFactory)

    @ObsoleteDescriptorBasedAPI
    fun declareEnumEntryIfNotExists(descriptor: ClassDescriptor, factory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry =
        enumEntrySymbolTable.declareIfNotExists(descriptor, { signature -> createEnumEntrySymbol(descriptor, signature) }, factory)

    // Note: used in native
    @ObsoleteDescriptorBasedAPI
    @Suppress("unused")
    fun declareEnumEntryFromLinker(
        descriptor: ClassDescriptor,
        signature: IdSignature,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry
    ): IrEnumEntry {
        return enumEntrySymbolTable.run {
            if (signature.isPubliclyVisible) {
                declare(signature, descriptor, { IrEnumEntryPublicSymbolImpl(signature, descriptor) }, factory)
            } else {
                declare(descriptor, { IrEnumEntrySymbolImpl(descriptor) }, factory)
            }
        }
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol =
        enumEntrySymbolTable.referenced(descriptor) { signature -> createEnumEntrySymbol(descriptor, signature) }

    override fun referenceEnumEntry(signature: IdSignature) =
        enumEntrySymbolTable.run {
            if (signature.isPubliclyVisible) referenced(signature) { IrEnumEntryPublicSymbolImpl(signature) }
            else IrEnumEntrySymbolImpl()
        }

    val unboundEnumEntries: Set<IrEnumEntrySymbol> get() = enumEntrySymbolTable.unboundSymbols

    private fun createFieldSymbol(descriptor: PropertyDescriptor, signature: IdSignature?): IrFieldSymbol =
        signature?.let { IrFieldPublicSymbolImpl(it, descriptor) } ?: IrFieldSymbolImpl(descriptor)

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType,
        visibility: DescriptorVisibility? = null,
        fieldFactory: (IrFieldSymbol) -> IrField = {
            irFactory.createField(
                startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(descriptor), type,
                visibility ?: it.descriptor.visibility, !it.descriptor.isVar, it.descriptor.isEffectivelyExternal(),
                it.descriptor.dispatchReceiverParameter == null
            ).apply {
                metadata = DescriptorMetadataSource.Property(it.descriptor)
            }
        }
    ): IrField =
        fieldSymbolTable.declare(
            descriptor,
            { signature -> createFieldSymbol(descriptor, signature) },
            fieldFactory
        )

    @ObsoleteDescriptorBasedAPI
    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType,
        irInitializer: IrExpressionBody?
    ): IrField =
        declareField(startOffset, endOffset, origin, descriptor, type).apply {
            initializer = irInitializer
        }

    fun declareField(
        signature: IdSignature,
        symbolFactory: () -> IrFieldSymbol,
        propertyFactory: (IrFieldSymbol) -> IrField
    ): IrField {
        return fieldSymbolTable.declare(
            signature,
            symbolFactory,
            propertyFactory
        )
    }

    fun declareFieldWithSignature(signature: IdSignature, symbol: IrFieldSymbol) {
        fieldSymbolTable.set(signature, symbol)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceField(descriptor: PropertyDescriptor): IrFieldSymbol =
        fieldSymbolTable.referenced(descriptor) { signature -> createFieldSymbol(descriptor, signature) }

    override fun referenceField(signature: IdSignature): IrFieldSymbol =
        fieldSymbolTable.run {
            if (signature.isPubliclyVisible) {
                referenced(signature) { IrFieldPublicSymbolImpl(signature) }
            } else IrFieldSymbolImpl()
        }

    val unboundFields: Set<IrFieldSymbol> get() = fieldSymbolTable.unboundSymbols

    @Deprecated(message = "Use declareProperty/referenceProperty", level = DeprecationLevel.WARNING)
    val propertyTable = HashMap<PropertyDescriptor, IrProperty>()

    @ObsoleteDescriptorBasedAPI
    override fun referenceProperty(descriptor: PropertyDescriptor, generate: () -> IrProperty): IrProperty =
        @Suppress("DEPRECATION")
        propertyTable.getOrPut(descriptor, generate)

    private fun createPropertySymbol(descriptor: PropertyDescriptor, signature: IdSignature?): IrPropertySymbol =
        signature?.let { IrPropertyPublicSymbolImpl(it, descriptor) } ?: IrPropertySymbolImpl(descriptor)

    @ObsoleteDescriptorBasedAPI
    fun declareProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        isDelegated: Boolean = descriptor.isDelegated,
        propertyFactory: (IrPropertySymbol) -> IrProperty = { symbol ->
            irFactory.createProperty(
                startOffset, endOffset, origin, symbol, name = nameProvider.nameForDeclaration(descriptor),
                visibility = descriptor.visibility,
                modality = descriptor.modality,
                isVar = descriptor.isVar,
                isConst = descriptor.isConst,
                isLateinit = descriptor.isLateInit,
                isDelegated = isDelegated,
                isExternal = descriptor.isEffectivelyExternal(),
                isExpect = descriptor.isExpect,
                isFakeOverride = descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
            ).apply {
                metadata = DescriptorMetadataSource.Property(symbol.descriptor)
            }
        }
    ): IrProperty =
        propertySymbolTable.declare(
            descriptor,
            { signature -> createPropertySymbol(descriptor, signature) },
            propertyFactory
        )

    fun declareProperty(
        signature: IdSignature,
        symbolFactory: () -> IrPropertySymbol,
        propertyFactory: (IrPropertySymbol) -> IrProperty
    ): IrProperty {
        return propertySymbolTable.declare(
            signature,
            symbolFactory,
            propertyFactory
        )
    }

    @ObsoleteDescriptorBasedAPI
    fun declarePropertyIfNotExists(descriptor: PropertyDescriptor, propertyFactory: (IrPropertySymbol) -> IrProperty): IrProperty =
        propertySymbolTable.declareIfNotExists(descriptor, { signature -> createPropertySymbol(descriptor, signature) }, propertyFactory)

    @ObsoleteDescriptorBasedAPI
    fun declarePropertyFromLinker(
        descriptor: PropertyDescriptor,
        signature: IdSignature,
        factory: (IrPropertySymbol) -> IrProperty,
    ): IrProperty {
        return propertySymbolTable.run {
            if (signature.isPubliclyVisible) {
                declare(signature, descriptor, { IrPropertyPublicSymbolImpl(signature, descriptor) }, factory)
            } else {
                declare(descriptor, { IrPropertySymbolImpl(descriptor) }, factory)
            }
        }
    }

    fun declarePropertyWithSignature(signature: IdSignature, symbol: IrPropertySymbol) {
        propertySymbolTable.set(signature, symbol)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol =
        propertySymbolTable.referenced(descriptor) { signature -> createPropertySymbol(descriptor, signature) }

    fun referencePropertyIfAny(signature: IdSignature): IrPropertySymbol? =
        propertySymbolTable.get(signature)

    override fun referenceProperty(signature: IdSignature): IrPropertySymbol =
        propertySymbolTable.run {
            if (signature.isPubliclyVisible) referenced(signature) { IrPropertyPublicSymbolImpl(signature) }
            else IrPropertySymbolImpl()
        }

    val unboundProperties: Set<IrPropertySymbol> get() = propertySymbolTable.unboundSymbols

    private fun createTypeAliasSymbol(descriptor: TypeAliasDescriptor, signature: IdSignature?): IrTypeAliasSymbol =
        signature?.let { IrTypeAliasPublicSymbolImpl(it, descriptor) } ?: IrTypeAliasSymbolImpl(descriptor)

    @ObsoleteDescriptorBasedAPI
    override fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol =
        typeAliasSymbolTable.referenced(descriptor) { signature -> createTypeAliasSymbol(descriptor, signature) }

    override fun referenceTypeAlias(signature: IdSignature) =
        typeAliasSymbolTable.run {
            if (signature.isPubliclyVisible) referenced(signature) { IrTypeAliasPublicSymbolImpl(signature) }
            else IrTypeAliasSymbolImpl()
        }

    @ObsoleteDescriptorBasedAPI
    fun declareTypeAlias(descriptor: TypeAliasDescriptor, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias =
        typeAliasSymbolTable.declare(descriptor, { signature -> createTypeAliasSymbol(descriptor, signature) }, factory)

    fun declareTypeAlias(
        signature: IdSignature,
        symbolFactory: () -> IrTypeAliasSymbol,
        factory: (IrTypeAliasSymbol) -> IrTypeAlias
    ): IrTypeAlias {
        return typeAliasSymbolTable.declare(signature, symbolFactory, factory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareTypeAliasIfNotExists(descriptor: TypeAliasDescriptor, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias =
        typeAliasSymbolTable.declareIfNotExists(descriptor, { signature -> createTypeAliasSymbol(descriptor, signature) }, factory)

    val unboundTypeAliases: Set<IrTypeAliasSymbol> get() = typeAliasSymbolTable.unboundSymbols

    private fun createSimpleFunctionSymbol(descriptor: FunctionDescriptor, signature: IdSignature?): IrSimpleFunctionSymbol =
        signature?.let { IrSimpleFunctionPublicSymbolImpl(it, descriptor) } ?: IrSimpleFunctionSymbolImpl(descriptor)

    @ObsoleteDescriptorBasedAPI
    fun declareSimpleFunction(
        descriptor: FunctionDescriptor,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.declare(
            descriptor,
            { signature -> createSimpleFunctionSymbol(descriptor, signature) },
            functionFactory
        )
    }

    fun declareSimpleFunction(
        signature: IdSignature,
        symbolFactory: () -> IrSimpleFunctionSymbol,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.declare(
            signature,
            symbolFactory,
            functionFactory
        )
    }

    @ObsoleteDescriptorBasedAPI
    fun declareSimpleFunctionIfNotExists(
        descriptor: FunctionDescriptor,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction =
        simpleFunctionSymbolTable.declareIfNotExists(
            descriptor, { signature -> createSimpleFunctionSymbol(descriptor, signature) }, functionFactory
        )

    @ObsoleteDescriptorBasedAPI
    fun declareSimpleFunctionFromLinker(
        descriptor: FunctionDescriptor?,
        signature: IdSignature,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.run {
            if (signature.isPubliclyVisible) {
                declare(signature, descriptor, { IrSimpleFunctionPublicSymbolImpl(signature, descriptor) }, functionFactory)
            } else {
                declare(descriptor!!, { IrSimpleFunctionSymbolImpl(descriptor) }, functionFactory)
            }
        }
    }

    fun declareSimpleFunctionWithSignature(signature: IdSignature, symbol: IrSimpleFunctionSymbol) {
        simpleFunctionSymbolTable.set(signature, symbol)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol =
        simpleFunctionSymbolTable.referenced(descriptor) { signature -> createSimpleFunctionSymbol(descriptor, signature) }

    fun referenceSimpleFunctionIfAny(signature: IdSignature): IrSimpleFunctionSymbol? =
        simpleFunctionSymbolTable.get(signature)

    override fun referenceSimpleFunction(signature: IdSignature): IrSimpleFunctionSymbol {
        return simpleFunctionSymbolTable.run {
            if (signature.isPubliclyVisible) referenced(signature) { IrSimpleFunctionPublicSymbolImpl(signature) }
            else IrSimpleFunctionSymbolImpl().also {
                it.privateSignature = signature
            }
        }
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceDeclaredFunction(descriptor: FunctionDescriptor) =
        simpleFunctionSymbolTable.referenced(descriptor) { throw AssertionError("Function is not declared: $descriptor") }

    val unboundSimpleFunctions: Set<IrSimpleFunctionSymbol> get() = simpleFunctionSymbolTable.unboundSymbols

    private fun createTypeParameterSymbol(descriptor: TypeParameterDescriptor, signature: IdSignature?): IrTypeParameterSymbol =
        signature?.let { IrTypeParameterPublicSymbolImpl(it, descriptor) } ?: IrTypeParameterSymbolImpl(descriptor)

    @ObsoleteDescriptorBasedAPI
    fun declareGlobalTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter = {
            irFactory.createTypeParameter(
                startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(descriptor),
                it.descriptor.index, it.descriptor.isReified, it.descriptor.variance
            )
        }
    ): IrTypeParameter =
        globalTypeParameterSymbolTable.declare(
            descriptor,
            { signature -> createTypeParameterSymbol(descriptor, signature) },
            typeParameterFactory
        )

    fun declareGlobalTypeParameter(
        signature: IdSignature,
        symbolFactory: () -> IrTypeParameterSymbol,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
        return globalTypeParameterSymbolTable.declare(signature, symbolFactory, typeParameterFactory)
    }

    @ObsoleteDescriptorBasedAPI
    fun declareScopedTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter = {
            irFactory.createTypeParameter(
                startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(descriptor),
                it.descriptor.index, it.descriptor.isReified, it.descriptor.variance
            )
        }
    ): IrTypeParameter =
        scopedTypeParameterSymbolTable.declare(
            descriptor,
            { signature -> createTypeParameterSymbol(descriptor, signature) },
            typeParameterFactory
        )

    fun declareScopedTypeParameter(
        signature: IdSignature,
        symbolFactory: (IdSignature) -> IrTypeParameterSymbol,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
        return typeParameterFactory(symbolFactory(signature))
    }

    val unboundTypeParameters: Set<IrTypeParameterSymbol> get() = globalTypeParameterSymbolTable.unboundSymbols

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
        valueParameterFactory: (IrValueParameterSymbol) -> IrValueParameter = {
            irFactory.createValueParameter(
                startOffset, endOffset, origin, it, name ?: nameProvider.nameForDeclaration(descriptor),
                index ?: descriptor.indexOrMinusOne, type, varargElementType, descriptor.isCrossinline, descriptor.isNoinline,
                isHidden = false, isAssignable = isAssignable
            )
        }
    ): IrValueParameter =
        valueParameterSymbolTable.declareLocal(
            descriptor,
            { IrValueParameterSymbolImpl(descriptor) },
            valueParameterFactory
        )

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun introduceValueParameter(irValueParameter: IrValueParameter) {
        valueParameterSymbolTable.introduceLocal(irValueParameter.descriptor, irValueParameter.symbol)
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceValueParameter(descriptor: ParameterDescriptor) =
        valueParameterSymbolTable.referenced(descriptor) {
            throw AssertionError("Undefined parameter referenced: $descriptor\n${valueParameterSymbolTable.dump()}")
        }

    @ObsoleteDescriptorBasedAPI
    override fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol =
        scopedTypeParameterSymbolTable.get(classifier, signaturer.composeSignature(classifier))
            ?: globalTypeParameterSymbolTable.referenced(classifier) { signature -> createTypeParameterSymbol(classifier, signature) }

    @ObsoleteDescriptorBasedAPI
    override fun referenceScopedTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol =
        scopedTypeParameterSymbolTable.referenced(classifier) { signature -> createTypeParameterSymbol(classifier, signature) }

    override fun referenceTypeParameter(signature: IdSignature): IrTypeParameterSymbol =
        globalTypeParameterSymbolTable.referenced(signature) {
            if (signature.isPubliclyVisible) IrTypeParameterPublicSymbolImpl(signature) else IrTypeParameterSymbolImpl()
        }

    @ObsoleteDescriptorBasedAPI
    fun declareVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptor,
        type: IrType,
        variableFactory: (IrVariableSymbol) -> IrVariable = {
            IrVariableImpl(
                startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(descriptor), type,
                descriptor.isVar, descriptor.isConst, descriptor.isLateInit
            )
        }
    ): IrVariable =
        variableSymbolTable.declareLocal(
            descriptor,
            { IrVariableSymbolImpl(descriptor) },
            variableFactory
        )

    @ObsoleteDescriptorBasedAPI
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

    @ObsoleteDescriptorBasedAPI
    override fun referenceVariable(descriptor: VariableDescriptor) =
        variableSymbolTable.referenced(descriptor) { throw AssertionError("Undefined variable referenced: $descriptor") }

    @ObsoleteDescriptorBasedAPI
    fun declareLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors,
        type: IrType,
        factory: (IrLocalDelegatedPropertySymbol) -> IrLocalDelegatedProperty = {
            irFactory.createLocalDelegatedProperty(
                startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(descriptor), type, descriptor.isVar
            )
        }
    ): IrLocalDelegatedProperty =
        localDelegatedPropertySymbolTable.declareLocal(
            descriptor,
            { IrLocalDelegatedPropertySymbolImpl(descriptor) },
            factory
        ).apply {
            metadata = DescriptorMetadataSource.LocalDelegatedProperty(descriptor)
        }

    @ObsoleteDescriptorBasedAPI
    fun referenceLocalDelegatedProperty(descriptor: VariableDescriptorWithAccessors) =
        localDelegatedPropertySymbolTable.referenced(descriptor) {
            throw AssertionError("Undefined local delegated property referenced: $descriptor")
        }

    override fun enterScope(symbol: IrSymbol) {
        scopedSymbolTables.forEach { it.enterScope(symbol) }
    }

    override fun enterScope(owner: IrDeclaration) {
        enterScope(owner.symbol)
    }

    override fun leaveScope(symbol: IrSymbol) {
        scopedSymbolTables.forEach { it.leaveScope(symbol) }
    }

    override fun leaveScope(owner: IrDeclaration) {
        leaveScope(owner.symbol)
    }

    @ObsoleteDescriptorBasedAPI
    open fun referenceValue(value: ValueDescriptor): IrValueSymbol =
        when (value) {
            is ParameterDescriptor ->
                valueParameterSymbolTable.referenced(value) { throw AssertionError("Undefined parameter referenced: $value") }
            is VariableDescriptor ->
                variableSymbolTable.referenced(value) { throw AssertionError("Undefined variable referenced: $value") }
            else ->
                throw IllegalArgumentException("Unexpected value descriptor: $value")
        }

    private inline fun <Descriptor, Owner, Symbol> FlatSymbolTable<Descriptor, Owner, Symbol>.forEachSymbolImpl(block: (IrSymbol) -> Unit)
            where Descriptor : DeclarationDescriptor,
                  Owner : IrSymbolOwner,
                  Symbol : IrBindableSymbol<Descriptor, Owner> {
        idSignatureToSymbol.forEach { (_, symbol) ->
            block(symbol)
        }
    }

    /**
     * This function is quite messy and doesn't have good contract of what exactly is traversed.
     * Basic idea is it traverse symbols which can be reasonable referered from other module
     *
     * Be careful when using it, and avoid it, except really need.
     */
    fun forEachDeclarationSymbol(block: (IrSymbol) -> Unit) {
        classSymbolTable.forEachSymbolImpl { block(it) }
        constructorSymbolTable.forEachSymbolImpl { block(it) }
        simpleFunctionSymbolTable.forEachSymbolImpl { block(it) }
        propertySymbolTable.forEachSymbolImpl { block(it) }
        enumEntrySymbolTable.forEachSymbolImpl { block(it) }
        typeAliasSymbolTable.forEachSymbolImpl { block(it) }
        fieldSymbolTable.forEachSymbolImpl { block(it) }
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

val SymbolTable.allUnbound: Set<IrSymbol>
    get() = buildSet {
        fun addUnbound(symbols: Collection<IrSymbol>) {
            symbols.filterTo(this) { !it.isBound }
        }

        addUnbound(unboundClasses)
        addUnbound(unboundConstructors)
        addUnbound(unboundEnumEntries)
        addUnbound(unboundFields)
        addUnbound(unboundSimpleFunctions)
        addUnbound(unboundProperties)
        addUnbound(unboundTypeAliases)
        addUnbound(unboundTypeParameters)
    }
