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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.utils.threadLocal

interface ReferenceSymbolTable {
    fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol

    fun referenceScript(descriptor: ScriptDescriptor): IrScriptSymbol

    fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol

    fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol
    fun referenceField(descriptor: PropertyDescriptor): IrFieldSymbol
    fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol

    fun referenceProperty(descriptor: PropertyDescriptor, generate: () -> IrProperty): IrProperty

    fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol
    fun referenceDeclaredFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol
    fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol

    fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol
    fun referenceVariable(descriptor: VariableDescriptor): IrVariableSymbol

    fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol

    fun referenceClassFromLinker(sig: IdSignature): IrClassSymbol
    fun referenceConstructorFromLinker(sig: IdSignature): IrConstructorSymbol
    fun referenceEnumEntryFromLinker(sig: IdSignature): IrEnumEntrySymbol
    fun referenceFieldFromLinker(sig: IdSignature): IrFieldSymbol
    fun referencePropertyFromLinker(sig: IdSignature): IrPropertySymbol
    fun referenceSimpleFunctionFromLinker(sig: IdSignature): IrSimpleFunctionSymbol
    fun referenceTypeParameterFromLinker(sig: IdSignature): IrTypeParameterSymbol
    fun referenceTypeAliasFromLinker(sig: IdSignature): IrTypeAliasSymbol

    fun enterScope(owner: IrSymbol)
    fun enterScope(owner: IrDeclaration)

    fun leaveScope(owner: IrSymbol)
    fun leaveScope(owner: IrDeclaration)
}

open class SymbolTable(
    val signaturer: IdSignatureComposer,
    val irFactory: IrFactory,
    val nameProvider: NameProvider = NameProvider.DEFAULT,
) : ReferenceSymbolTable {

    val lock = IrLock()

    @Suppress("LeakingThis")
    val lazyWrapper = IrLazySymbolTable(this)

    protected abstract class SymbolTableBase<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>>(val lock: IrLock) {
        val unboundSymbols = linkedSetOf<S>()

        abstract fun get(d: D): S?
        abstract fun set(s: S)
        abstract fun get(sig: IdSignature): S?

        inline fun declare(d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            synchronized(lock) {
                @Suppress("UNCHECKED_CAST")
                val d0 = d.original as D
                assert(d0 === d) {
                    "Non-original descriptor in declaration: $d\n\tExpected: $d0"
                }
                val existing = get(d0)
                val symbol = if (existing == null) {
                    val new = createSymbol()
                    set(new)
                    new
                } else {
                    unboundSymbols.remove(existing)
                    existing
                }
                return createOwner(symbol)
            }
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        inline fun declare(sig: IdSignature, createSymbol: () -> S, createOwner: (S) -> B): B {
            synchronized(lock) {
                val existing = get(sig)
                val symbol = if (existing == null) {
                    createSymbol()
                } else {
                    unboundSymbols.remove(existing)
                    existing
                }
                val result = createOwner(symbol)
                // TODO: try to get rid of this
                set(symbol)
                return result
            }
        }

        inline fun declareIfNotExists(d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            synchronized(lock) {
                @Suppress("UNCHECKED_CAST")
                val d0 = d.original as D
                assert(d0 === d) {
                    "Non-original descriptor in declaration: $d\n\tExpected: $d0"
                }
                val existing = get(d0)
                val symbol = if (existing == null) {
                    val new = createSymbol()
                    set(new)
                    new
                } else {
                    if (!existing.isBound) unboundSymbols.remove(existing)
                    existing
                }
                return if (symbol.isBound) symbol.owner else createOwner(symbol)
            }
        }

        inline fun declare(sig: IdSignature, d: D?, createSymbol: () -> S, createOwner: (S) -> B): B {
            synchronized(lock) {
                @Suppress("UNCHECKED_CAST")
                val d0 = d?.original as D
                assert(d0 === d) {
                    "Non-original descriptor in declaration: $d\n\tExpected: $d0"
                }
                val existing = get(sig)
                val symbol = if (existing == null) {
                    val new = createSymbol()
                    set(new)
                    new
                } else {
                    unboundSymbols.remove(existing)
                    existing
                }
                return createOwner(symbol)
            }
        }

        inline fun referenced(d: D, orElse: () -> S): S {
            synchronized(lock) {
                @Suppress("UNCHECKED_CAST")
                val d0 = d.original as D
                assert(d0 === d) {
                    "Non-original descriptor in declaration: $d\n\tExpected: $d0"
                }
                val s = get(d0)
                if (s == null) {
                    val new = orElse()
                    assert(unboundSymbols.add(new)) {
                        "Symbol for $new was already referenced"
                    }
                    set(new)
                    return new
                }
                return s
            }
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        inline fun referenced(sig: IdSignature, orElse: () -> S): S {
            synchronized(lock) {
                return get(sig) ?: run {
                    val new = orElse()
                    assert(unboundSymbols.add(new)) {
                        "Symbol for ${new.signature} was already referenced"
                    }
                    set(new)
                    new
                }
            }
        }
    }

    protected open inner class FlatSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>> :
        SymbolTableBase<D, B, S>(lock) {
        val descriptorToSymbol = linkedMapOf<D, S>()
        val idSigToSymbol = linkedMapOf<IdSignature, S>()

        protected open fun signature(descriptor: D): IdSignature? = signaturer.composeSignature(descriptor)

        override fun get(d: D): S? {
            val sig = signature(d)
            return if (sig != null) {
                idSigToSymbol[sig]
            } else {
                descriptorToSymbol[d]
            }
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        override fun set(s: S) {
            val signature = s.signature
            if (signature != null) {
                idSigToSymbol[signature] = s
            } else if (s.hasDescriptor) {
                descriptorToSymbol[s.descriptor] = s
            }
        }

        override fun get(sig: IdSignature): S? = idSigToSymbol[sig]
    }

    private inner class EnumEntrySymbolTable : FlatSymbolTable<ClassDescriptor, IrEnumEntry, IrEnumEntrySymbol>() {
        override fun signature(descriptor: ClassDescriptor): IdSignature? = signaturer.composeEnumEntrySignature(descriptor)
    }

    protected inner class FieldSymbolTable : FlatSymbolTable<PropertyDescriptor, IrField, IrFieldSymbol>() {
        override fun signature(descriptor: PropertyDescriptor): IdSignature? = null
    }

    protected inner class ScopedSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>>
        : SymbolTableBase<D, B, S>(lock) {
        inner class Scope(val owner: IrSymbol, val parent: Scope?) {
            private val descriptorToSymbol = linkedMapOf<D, S>()
            private val idSigToSymbol = linkedMapOf<IdSignature, S>()

            private fun getByDescriptor(d: D): S? {
                return descriptorToSymbol[d] ?: parent?.getByDescriptor(d)
            }

            private fun getByIdSignature(sig: IdSignature): S? {
                return idSigToSymbol[sig] ?: parent?.getByIdSignature(sig)
            }

            operator fun get(d: D): S? {
                val sig = signaturer.composeSignature(d)
                return if (sig != null) {
                    getByIdSignature(sig)
                } else {
                    getByDescriptor(d)
                }
            }

            fun getLocal(d: D) = descriptorToSymbol[d]

            @OptIn(ObsoleteDescriptorBasedAPI::class)
            operator fun set(d: D, s: S) {
                s.signature?.let {
                    require(d is TypeParameterDescriptor)
                    idSigToSymbol[it] = s
                } ?: run {
                    assert(s.hasDescriptor)
                    descriptorToSymbol[s.descriptor] = s
                }
            }

            operator fun get(sig: IdSignature): S? = idSigToSymbol[sig] ?: parent?.get(sig)

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

        protected var currentScope: Scope? = null

        override fun get(d: D): S? {
            val scope = currentScope ?: return null
            return scope[d]
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        override fun set(s: S) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[s.descriptor] = s
        }

        override fun get(sig: IdSignature): S? {
            val scope = currentScope ?: return null
            return scope[sig]
        }

        inline fun declareLocal(d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            val scope = currentScope ?: throw AssertionError("No active scope")
            val symbol = scope.getLocal(d) ?: createSymbol().also { scope[d] = it }
            return createOwner(symbol)
        }

        fun introduceLocal(descriptor: D, symbol: S) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[descriptor]?.let {
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
    protected val fieldSymbolTable = FieldSymbolTable()
    private val simpleFunctionSymbolTable = FlatSymbolTable<FunctionDescriptor, IrSimpleFunction, IrSimpleFunctionSymbol>()
    private val propertySymbolTable = FlatSymbolTable<PropertyDescriptor, IrProperty, IrPropertySymbol>()
    private val typeAliasSymbolTable = FlatSymbolTable<TypeAliasDescriptor, IrTypeAlias, IrTypeAliasSymbol>()

    protected val globalTypeParameterSymbolTable = FlatSymbolTable<TypeParameterDescriptor, IrTypeParameter, IrTypeParameterSymbol>()
    protected val scopedTypeParameterSymbolTable by threadLocal {
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

    fun referenceExternalPackageFragment(descriptor: PackageFragmentDescriptor) =
        externalPackageFragmentTable.referenced(descriptor) { IrExternalPackageFragmentSymbolImpl(descriptor) }

    fun declareExternalPackageFragment(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        return externalPackageFragmentTable.declare(
            descriptor,
            { IrExternalPackageFragmentSymbolImpl(descriptor) },
            { IrExternalPackageFragmentImpl(it, descriptor.fqName) }
        )
    }

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

    fun listExistedScripts() = scriptSymbolTable.descriptorToSymbol.map { it.value }

    fun declareScript(
        descriptor: ScriptDescriptor,
        scriptFactory: (IrScriptSymbol) -> IrScript = { symbol: IrScriptSymbol ->
            IrScriptImpl(symbol, nameProvider.nameForDeclaration(descriptor), irFactory)
        }
    ): IrScript {
        return scriptSymbolTable.declare(
            descriptor,
            { IrScriptSymbolImpl(descriptor) },
            scriptFactory
        )
    }

    fun declareScript(
        sig: IdSignature,
        symbolFactory: () -> IrScriptSymbol,
        classFactory: (IrScriptSymbol) -> IrScript
    ): IrScript {
        return scriptSymbolTable.declare(
            sig,
            symbolFactory,
            classFactory
        )
    }

    override fun referenceScript(descriptor: ScriptDescriptor): IrScriptSymbol {
        return scriptSymbolTable.referenced(descriptor) { IrScriptSymbolImpl(descriptor) }
    }

    private fun createClassSymbol(descriptor: ClassDescriptor): IrClassSymbol {
        return signaturer.composeSignature(descriptor)?.let { IrClassPublicSymbolImpl(it, descriptor) } ?: IrClassSymbolImpl(descriptor)
    }

    fun declareClass(
        descriptor: ClassDescriptor,
        classFactory: (IrClassSymbol) -> IrClass
    ): IrClass {
        return classSymbolTable.declare(
            descriptor,
            { createClassSymbol(descriptor) },
            classFactory
        )
    }

    fun declareClass(
        sig: IdSignature,
        symbolFactory: () -> IrClassSymbol,
        classFactory: (IrClassSymbol) -> IrClass
    ): IrClass {
        return classSymbolTable.declare(
            sig,
            symbolFactory,
            classFactory
        )
    }

    fun declareClassIfNotExists(descriptor: ClassDescriptor, classFactory: (IrClassSymbol) -> IrClass): IrClass {
        return classSymbolTable.declareIfNotExists(descriptor, { createClassSymbol(descriptor) }, classFactory)
    }

    fun declareClassFromLinker(descriptor: ClassDescriptor, sig: IdSignature, factory: (IrClassSymbol) -> IrClass): IrClass {
        return classSymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrClassPublicSymbolImpl(sig, descriptor) }, factory)
            } else {
                declare(descriptor, { IrClassSymbolImpl(descriptor) }, factory)
            }
        }
    }

    override fun referenceClass(descriptor: ClassDescriptor) =
        classSymbolTable.referenced(descriptor) { createClassSymbol(descriptor) }

    fun referenceClassIfAny(sig: IdSignature): IrClassSymbol? =
        classSymbolTable.get(sig)

    override fun referenceClassFromLinker(sig: IdSignature): IrClassSymbol =
        classSymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrClassPublicSymbolImpl(sig) }
            else IrClassSymbolImpl()
        }

    val unboundClasses: Set<IrClassSymbol> get() = classSymbolTable.unboundSymbols

    private fun createConstructorSymbol(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return signaturer.composeSignature(descriptor)?.let { IrConstructorPublicSymbolImpl(it, descriptor) } ?: IrConstructorSymbolImpl(
            descriptor
        )
    }

    fun declareConstructor(
        descriptor: ClassConstructorDescriptor,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor
    ): IrConstructor =
        constructorSymbolTable.declare(
            descriptor,
            { createConstructorSymbol(descriptor) },
            constructorFactory
        )

    fun declareConstructor(
        sig: IdSignature,
        symbolFactory: () -> IrConstructorSymbol,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor
    ): IrConstructor {
        return constructorSymbolTable.declare(
            sig,
            symbolFactory,
            constructorFactory
        )
    }

    fun declareConstructorIfNotExists(descriptor: ClassConstructorDescriptor, constructorFactory: (IrConstructorSymbol) -> IrConstructor): IrConstructor =
        constructorSymbolTable.declareIfNotExists(
            descriptor,
            { createConstructorSymbol(descriptor) },
            constructorFactory
        )

    override fun referenceConstructor(descriptor: ClassConstructorDescriptor) =
        constructorSymbolTable.referenced(descriptor) { createConstructorSymbol(descriptor) }

    fun referenceConstructorIfAny(sig: IdSignature): IrConstructorSymbol? =
        constructorSymbolTable.get(sig)

    fun declareConstructorFromLinker(
        descriptor: ClassConstructorDescriptor,
        sig: IdSignature,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor
    ): IrConstructor {
        return constructorSymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrConstructorPublicSymbolImpl(sig, descriptor) }, constructorFactory)
            } else {
                declare(descriptor, { IrConstructorSymbolImpl(descriptor) }, constructorFactory)
            }
        }
    }

    override fun referenceConstructorFromLinker(sig: IdSignature): IrConstructorSymbol =
        constructorSymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrConstructorPublicSymbolImpl(sig) }
            else IrConstructorSymbolImpl()
        }

    val unboundConstructors: Set<IrConstructorSymbol> get() = constructorSymbolTable.unboundSymbols

    private fun createEnumEntrySymbol(descriptor: ClassDescriptor): IrEnumEntrySymbol {
        return signaturer.composeEnumEntrySignature(descriptor)?.let { IrEnumEntryPublicSymbolImpl(it, descriptor) }
            ?: IrEnumEntrySymbolImpl(descriptor)
    }

    fun declareEnumEntry(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry = {
            irFactory.createEnumEntry(startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(descriptor))
        }
    ): IrEnumEntry =
        enumEntrySymbolTable.declare(
            descriptor,
            { createEnumEntrySymbol(descriptor) },
            factory
        )

    fun declareEnumEntry(
        sig: IdSignature,
        symbolFactory: () -> IrEnumEntrySymbol,
        enumEntryFactory: (IrEnumEntrySymbol) -> IrEnumEntry
    ): IrEnumEntry = enumEntrySymbolTable.declare(sig, symbolFactory, enumEntryFactory)

    fun declareEnumEntryIfNotExists(descriptor: ClassDescriptor, factory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry {
        return enumEntrySymbolTable.declareIfNotExists(descriptor, { createEnumEntrySymbol(descriptor) }, factory)
    }

    fun declareEnumEntryFromLinker(
        descriptor: ClassDescriptor,
        sig: IdSignature,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry
    ): IrEnumEntry {
        return enumEntrySymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrEnumEntryPublicSymbolImpl(sig, descriptor) }, factory)
            } else {
                declare(descriptor, { IrEnumEntrySymbolImpl(descriptor) }, factory)
            }
        }
    }

    override fun referenceEnumEntry(descriptor: ClassDescriptor) =
        enumEntrySymbolTable.referenced(descriptor) { createEnumEntrySymbol(descriptor) }

    override fun referenceEnumEntryFromLinker(sig: IdSignature) =
        enumEntrySymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrEnumEntryPublicSymbolImpl(sig) }
            else IrEnumEntrySymbolImpl()
        }

    val unboundEnumEntries: Set<IrEnumEntrySymbol> get() = enumEntrySymbolTable.unboundSymbols

    private fun createFieldSymbol(descriptor: PropertyDescriptor): IrFieldSymbol {
        return IrFieldSymbolImpl(descriptor)
    }

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
            { createFieldSymbol(descriptor) },
            fieldFactory
        )

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
        sig: IdSignature,
        symbolFactory: () -> IrFieldSymbol,
        propertyFactory: (IrFieldSymbol) -> IrField
    ): IrField {
        return fieldSymbolTable.declare(
            sig,
            symbolFactory,
            propertyFactory
        )
    }

    fun declareFieldFromLinker(descriptor: PropertyDescriptor, sig: IdSignature, factory: (IrFieldSymbol) -> IrField): IrField {
        return fieldSymbolTable.run {
            require(sig.isLocal)
            declare(descriptor, { IrFieldSymbolImpl(descriptor) }, factory)
        }
    }

    override fun referenceField(descriptor: PropertyDescriptor) =
        fieldSymbolTable.referenced(descriptor) { createFieldSymbol(descriptor) }

    override fun referenceFieldFromLinker(sig: IdSignature): IrFieldSymbol =
        fieldSymbolTable.run {
            require(sig.isLocal)
            IrFieldSymbolImpl()
        }

    val unboundFields: Set<IrFieldSymbol> get() = fieldSymbolTable.unboundSymbols

    @Deprecated(message = "Use declareProperty/referenceProperty", level = DeprecationLevel.WARNING)
    val propertyTable = HashMap<PropertyDescriptor, IrProperty>()

    override fun referenceProperty(descriptor: PropertyDescriptor, generate: () -> IrProperty): IrProperty =
        @Suppress("DEPRECATION")
        propertyTable.getOrPut(descriptor, generate)

    private fun createPropertySymbol(descriptor: PropertyDescriptor): IrPropertySymbol {
        return signaturer.composeSignature(descriptor)?.let { IrPropertyPublicSymbolImpl(it, descriptor) } ?: IrPropertySymbolImpl(
            descriptor
        )

    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
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
                isExpect = descriptor.isExpect
            ).apply {
                metadata = DescriptorMetadataSource.Property(symbol.descriptor)
            }
        }
    ): IrProperty =
        propertySymbolTable.declare(
            descriptor,
            { createPropertySymbol(descriptor) },
            propertyFactory
        )

    fun declareProperty(
        sig: IdSignature,
        symbolFactory: () -> IrPropertySymbol,
        propertyFactory: (IrPropertySymbol) -> IrProperty
    ): IrProperty {
        return propertySymbolTable.declare(
            sig,
            symbolFactory,
            propertyFactory
        )
    }

    fun declarePropertyIfNotExists(descriptor: PropertyDescriptor, propertyFactory: (IrPropertySymbol) -> IrProperty): IrProperty =
        propertySymbolTable.declareIfNotExists(descriptor, { createPropertySymbol(descriptor) }, propertyFactory)

    fun declarePropertyFromLinker(descriptor: PropertyDescriptor, sig: IdSignature, factory: (IrPropertySymbol) -> IrProperty): IrProperty {
        return propertySymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrPropertyPublicSymbolImpl(sig, descriptor) }, factory)
            } else {
                declare(descriptor, { IrPropertySymbolImpl(descriptor) }, factory)
            }
        }
    }

    override fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol =
        propertySymbolTable.referenced(descriptor) { createPropertySymbol(descriptor) }

    fun referencePropertyIfAny(sig: IdSignature): IrPropertySymbol? =
        propertySymbolTable.get(sig)

    override fun referencePropertyFromLinker(sig: IdSignature): IrPropertySymbol =
        propertySymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrPropertyPublicSymbolImpl(sig) }
            else IrPropertySymbolImpl()
        }

    val unboundProperties: Set<IrPropertySymbol> get() = propertySymbolTable.unboundSymbols

    private fun createTypeAliasSymbol(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol {
        return signaturer.composeSignature(descriptor)?.let { IrTypeAliasPublicSymbolImpl(it, descriptor) } ?: IrTypeAliasSymbolImpl(
            descriptor
        )
    }

    override fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol =
        typeAliasSymbolTable.referenced(descriptor) { createTypeAliasSymbol(descriptor) }

    fun declareTypeAliasFromLinker(
        descriptor: TypeAliasDescriptor,
        sig: IdSignature,
        factory: (IrTypeAliasSymbol) -> IrTypeAlias
    ): IrTypeAlias {
        return typeAliasSymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrTypeAliasPublicSymbolImpl(sig, descriptor) }, factory)
            } else {
                declare(descriptor, { IrTypeAliasSymbolImpl(descriptor) }, factory)
            }
        }
    }

    override fun referenceTypeAliasFromLinker(sig: IdSignature) =
        typeAliasSymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrTypeAliasPublicSymbolImpl(sig) }
            else IrTypeAliasSymbolImpl()
        }

    fun declareTypeAlias(descriptor: TypeAliasDescriptor, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias =
        typeAliasSymbolTable.declare(descriptor, { createTypeAliasSymbol(descriptor) }, factory)

    fun declareTypeAlias(
        sig: IdSignature,
        symbolFactory: () -> IrTypeAliasSymbol,
        factory: (IrTypeAliasSymbol) -> IrTypeAlias
    ): IrTypeAlias {
        return typeAliasSymbolTable.declare(sig, symbolFactory, factory)
    }

    fun declareTypeAliasIfNotExists(descriptor: TypeAliasDescriptor, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias =
        typeAliasSymbolTable.declareIfNotExists(descriptor, { createTypeAliasSymbol(descriptor) }, factory)

    val unboundTypeAliases: Set<IrTypeAliasSymbol> get() = typeAliasSymbolTable.unboundSymbols

    private fun createSimpleFunctionSymbol(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
        return signaturer.composeSignature(descriptor)?.let { IrSimpleFunctionPublicSymbolImpl(it, descriptor) }
            ?: IrSimpleFunctionSymbolImpl(descriptor)
    }

    fun declareSimpleFunction(
        descriptor: FunctionDescriptor,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.declare(
            descriptor,
            { createSimpleFunctionSymbol(descriptor) },
            functionFactory
        )
    }

    fun declareSimpleFunction(
        sig: IdSignature,
        symbolFactory: () -> IrSimpleFunctionSymbol,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.declare(
            sig,
            symbolFactory,
            functionFactory
        )
    }

    fun declareSimpleFunctionIfNotExists(
        descriptor: FunctionDescriptor,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.declareIfNotExists(descriptor, { createSimpleFunctionSymbol(descriptor) }, functionFactory)
    }

    fun declareSimpleFunctionFromLinker(
        descriptor: FunctionDescriptor?,
        sig: IdSignature,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrSimpleFunctionPublicSymbolImpl(sig, descriptor) }, functionFactory)
            } else {
                declare(descriptor!!, { IrSimpleFunctionSymbolImpl(descriptor) }, functionFactory)
            }
        }
    }

    override fun referenceSimpleFunction(descriptor: FunctionDescriptor) =
        simpleFunctionSymbolTable.referenced(descriptor) { createSimpleFunctionSymbol(descriptor) }

    fun referenceSimpleFunctionIfAny(sig: IdSignature): IrSimpleFunctionSymbol? =
        simpleFunctionSymbolTable.get(sig)

    override fun referenceSimpleFunctionFromLinker(sig: IdSignature): IrSimpleFunctionSymbol {
        return simpleFunctionSymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrSimpleFunctionPublicSymbolImpl(sig) }
            else IrSimpleFunctionSymbolImpl()
        }
    }

    override fun referenceDeclaredFunction(descriptor: FunctionDescriptor) =
        simpleFunctionSymbolTable.referenced(descriptor) { throw AssertionError("Function is not declared: $descriptor") }

    val unboundSimpleFunctions: Set<IrSimpleFunctionSymbol> get() = simpleFunctionSymbolTable.unboundSymbols

    private fun createTypeParameterSymbol(descriptor: TypeParameterDescriptor): IrTypeParameterSymbol {
        return IrTypeParameterSymbolImpl(descriptor)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
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
            { createTypeParameterSymbol(descriptor) },
            typeParameterFactory
        )

    open fun declareGlobalTypeParameter(
        sig: IdSignature,
        symbolFactory: () -> IrTypeParameterSymbol,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
        require(sig.isLocal)
        return globalTypeParameterSymbolTable.declare(sig, symbolFactory, typeParameterFactory)
    }

    fun declareGlobalTypeParameterFromLinker(
        descriptor: TypeParameterDescriptor,
        sig: IdSignature,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
        require(sig.isLocal)
        return globalTypeParameterSymbolTable.declare(descriptor, { IrTypeParameterSymbolImpl(descriptor) }, typeParameterFactory)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
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
            { createTypeParameterSymbol(descriptor) },
            typeParameterFactory
        )

    @Suppress("UNUSED_PARAMETER")
    fun declareScopedTypeParameter(
        sig: IdSignature,
        symbolFactory: () -> IrTypeParameterSymbol,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
//        require(sig.isLocal)
        return typeParameterFactory(symbolFactory())
    }

    fun declareScopedTypeParameterFromLinker(
        descriptor: TypeParameterDescriptor,
        sig: IdSignature,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
        require(sig.isLocal)
        return scopedTypeParameterSymbolTable.declare(descriptor, { IrTypeParameterSymbolImpl(descriptor) }, typeParameterFactory)
    }

    val unboundTypeParameters: Set<IrTypeParameterSymbol> get() = globalTypeParameterSymbolTable.unboundSymbols

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun declareValueParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: ParameterDescriptor,
        type: IrType,
        varargElementType: IrType? = null,
        name: Name? = null,
        valueParameterFactory: (IrValueParameterSymbol) -> IrValueParameter = {
            irFactory.createValueParameter(
                startOffset, endOffset, origin, it, name ?: nameProvider.nameForDeclaration(descriptor),
                descriptor.indexOrMinusOne, type, varargElementType, descriptor.isCrossinline, descriptor.isNoinline,
                isHidden = false, isAssignable = false
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

    override fun referenceValueParameter(descriptor: ParameterDescriptor) =
        valueParameterSymbolTable.referenced(descriptor) {
            throw AssertionError("Undefined parameter referenced: $descriptor\n${valueParameterSymbolTable.dump()}")
        }

    override fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol =
        scopedTypeParameterSymbolTable.get(classifier) ?: globalTypeParameterSymbolTable.referenced(classifier) {
            createTypeParameterSymbol(classifier)
        }

    override fun referenceTypeParameterFromLinker(sig: IdSignature): IrTypeParameterSymbol {
        require(sig.isLocal)
        return IrTypeParameterSymbolImpl()
    }

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

    override fun referenceVariable(descriptor: VariableDescriptor) =
        variableSymbolTable.referenced(descriptor) { throw AssertionError("Undefined variable referenced: $descriptor") }

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

    fun referenceLocalDelegatedProperty(descriptor: VariableDescriptorWithAccessors) =
        localDelegatedPropertySymbolTable.referenced(descriptor) {
            throw AssertionError("Undefined local delegated property referenced: $descriptor")
        }

    override fun enterScope(owner: IrSymbol) {
        scopedSymbolTables.forEach { it.enterScope(owner) }
    }

    override fun enterScope(owner: IrDeclaration) {
        enterScope(owner.symbol)
    }

    override fun leaveScope(owner: IrSymbol) {
        scopedSymbolTables.forEach { it.leaveScope(owner) }
    }

    override fun leaveScope(owner: IrDeclaration) {
        leaveScope(owner.symbol)
    }

    fun referenceValue(value: ValueDescriptor): IrValueSymbol =
        when (value) {
            is ParameterDescriptor ->
                valueParameterSymbolTable.referenced(value) { throw AssertionError("Undefined parameter referenced: $value") }
            is VariableDescriptor ->
                variableSymbolTable.referenced(value) { throw AssertionError("Undefined variable referenced: $value") }
            else ->
                throw IllegalArgumentException("Unexpected value descriptor: $value")
        }

    private inline fun <D : DeclarationDescriptor, IR : IrSymbolOwner, S : IrBindableSymbol<D, IR>> FlatSymbolTable<D, IR, S>.forEachPublicSymbolImpl(
        block: (IrSymbol) -> Unit
    ) {
        idSigToSymbol.forEach { (_, sym) ->
            assert(sym.isPublicApi)
            block(sym)
        }
    }

    fun forEachPublicSymbol(block: (IrSymbol) -> Unit) {
        classSymbolTable.forEachPublicSymbolImpl { block(it) }
        constructorSymbolTable.forEachPublicSymbolImpl { block(it) }
        simpleFunctionSymbolTable.forEachPublicSymbolImpl { block(it) }
        propertySymbolTable.forEachPublicSymbolImpl { block(it) }
        enumEntrySymbolTable.forEachPublicSymbolImpl { block(it) }
        typeAliasSymbolTable.forEachPublicSymbolImpl { block(it) }
        fieldSymbolTable.forEachPublicSymbolImpl { block(it) }
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

@ObsoleteDescriptorBasedAPI
inline fun <T> ReferenceSymbolTable.withReferenceScope(owner: IrSymbol, block: ReferenceSymbolTable.() -> T): T {
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
    get() {
        val r = mutableSetOf<IrSymbol>()
        r.addAll(unboundClasses)
        r.addAll(unboundConstructors)
        r.addAll(unboundEnumEntries)
        r.addAll(unboundFields)
        r.addAll(unboundSimpleFunctions)
        r.addAll(unboundProperties)
        r.addAll(unboundTypeAliases)
        r.addAll(unboundTypeParameters)
        return r.filter { !it.isBound }.toSet()
    }

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun SymbolTable.noUnboundLeft(message: String) {
    val unbound = this.allUnbound
    assert(unbound.isEmpty()) {
        "$message\n" +
                unbound.joinToString("\n") {
                    "$it ${it.signature?.toString() ?: "(NON-PUBLIC API)"}: ${it.descriptor}"
                }
    }
}
