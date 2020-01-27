/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazySymbolTable
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType

interface IrProvider {
    fun getDeclaration(symbol: IrSymbol): IrDeclaration?
}

/**
 * Extension of [IrProvider] which always produces inheritors of [IrLazyDeclarationBase].
 * Thus, it needs [declarationStubGenerator] to be able to produce IR declarations.
 */
interface LazyIrProvider : IrProvider {
    var declarationStubGenerator: DeclarationStubGenerator

    override fun getDeclaration(symbol: IrSymbol): IrLazyDeclarationBase?
}

interface IrDeserializer : IrProvider {
    fun declareForwardDeclarations()
}

interface ReferenceSymbolTable {
    fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol

    fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol

    fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol
    fun referenceField(descriptor: PropertyDescriptor): IrFieldSymbol
    fun referenceProperty(descriptor: PropertyDescriptor, generate: () -> IrProperty): IrProperty

    fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol
    fun referenceDeclaredFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol
    fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol

    fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol
    fun referenceVariable(descriptor: VariableDescriptor): IrVariableSymbol

    fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol

    fun referenceClassFromLinker(descriptor: ClassDescriptor, sig: IdSignature): IrClassSymbol
    fun referenceConstructorFromLinker(descriptor: ClassConstructorDescriptor, sig: IdSignature): IrConstructorSymbol
    fun referenceEnumEntryFromLinker(descriptor: ClassDescriptor, sig: IdSignature): IrEnumEntrySymbol
    fun referenceFieldFromLinker(descriptor: PropertyDescriptor, sig: IdSignature): IrFieldSymbol
    fun referencePropertyFromLinker(descriptor: PropertyDescriptor, sig: IdSignature): IrPropertySymbol
    fun referenceSimpleFunctionFromLinker(descriptor: FunctionDescriptor, sig: IdSignature): IrSimpleFunctionSymbol
    fun referenceTypeParameterFromLinker(classifier: TypeParameterDescriptor, sig: IdSignature): IrTypeParameterSymbol
    fun referenceTypeAliasFromLinker(descriptor: TypeAliasDescriptor, sig: IdSignature): IrTypeAliasSymbol

    fun enterScope(owner: DeclarationDescriptor)

    fun leaveScope(owner: DeclarationDescriptor)
}

open class SymbolTable(private val signaturer: IdSignatureComposer) : ReferenceSymbolTable {

    @Suppress("LeakingThis")
    val lazyWrapper = IrLazySymbolTable(this)

    private abstract inner class SymbolTableBase<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>> {
        val unboundSymbols = linkedSetOf<S>()

        abstract fun get(d: D): S?
        abstract fun set(d: D, s: S)
        abstract fun get(sig: IdSignature): S?

        inline fun declare(d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            @Suppress("UNCHECKED_CAST")
            val d0 = d.original as D
            assert(d0 === d) {
                "Non-original descriptor in declaration: $d\n\tExpected: $d0"
            }
            val existing = get(d0)
            val symbol = if (existing == null) {
                val new = createSymbol()
                set(d0, new)
                new
            } else {
                unboundSymbols.remove(existing)
                existing
            }
            return createOwner(symbol)
        }

        inline fun declare(sig: IdSignature, d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            @Suppress("UNCHECKED_CAST")
            val d0 = d.original as D
            assert(d0 === d) {
                "Non-original descriptor in declaration: $d\n\tExpected: $d0"
            }
            val existing = get(sig)
            val symbol = if (existing == null) {
                val new = createSymbol()
                set(d0, new)
                new
            } else {
                unboundSymbols.remove(existing)
                existing
            }
            return createOwner(symbol)
        }

        inline fun referenced(d: D, orElse: () -> S): S {
            @Suppress("UNCHECKED_CAST")
            val d0 = d.original as D
            assert(d0 === d) {
                "Non-original descriptor in declaration: $d\n\tExpected: $d0"
            }
            val s = get(d0)
            if (s == null) {
                val new = orElse()
                assert(unboundSymbols.add(new)) {
                    "Symbol for ${new.descriptor} was already referenced"
                }
                set(d0, new)
                return new
            }
            return s
        }

        inline fun referenced(sig: IdSignature, orElse: () -> S): S {
            return get(sig) ?: run {
                val new = orElse()
                assert(unboundSymbols.add(new)) {
                    "Symbol for ${new.signature} was already referenced"
                }
                set(new.descriptor, new)
                new
            }
        }
    }

    private open inner class FlatSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>> : SymbolTableBase<D, B, S>() {
        val descriptorToSymbol = linkedMapOf<D, S>()
        val idSigToSymbol = linkedMapOf<IdSignature, S>()

        protected open fun signature(descriptor: D): IdSignature? = signaturer.composeSignature(descriptor)

        override fun get(d: D): S? {
            return if (d !is WrappedDeclarationDescriptor<*>) {
                val sig = signature(d)
                if (sig != null) {
                    idSigToSymbol[sig]
                } else {
                    descriptorToSymbol[d]
                }
            } else {
                descriptorToSymbol[d]
            }
        }

        override fun set(d: D, s: S) {
            if (s.isPublicApi) {
                idSigToSymbol[s.signature] = s
            } else {
                descriptorToSymbol[d] = s
            }
        }

        override fun get(sig: IdSignature): S? = idSigToSymbol[sig]
    }

    private inner class EnumEntrySymbolTable : FlatSymbolTable<ClassDescriptor, IrEnumEntry, IrEnumEntrySymbol>() {
        override fun signature(descriptor: ClassDescriptor): IdSignature? = signaturer.composeEnumEntrySignature(descriptor)
    }

    private inner class FieldSymbolTable : FlatSymbolTable<PropertyDescriptor, IrField, IrFieldSymbol>() {
        override fun signature(descriptor: PropertyDescriptor): IdSignature? = null
    }

    private inner class ScopedSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>>
        : SymbolTableBase<D, B, S>() {
        inner class Scope(val owner: DeclarationDescriptor, val parent: Scope?) {
            private val descriptorToSymbol = linkedMapOf<D, S>()
            private val idSigToSymbol = linkedMapOf<IdSignature, S>()

            private fun getByDescriptor(d: D): S? {
                return descriptorToSymbol[d] ?: parent?.getByDescriptor(d)
            }

            private fun getByIdSignature(sig: IdSignature): S? {
                return idSigToSymbol[sig] ?: parent?.getByIdSignature(sig)
            }

            operator fun get(d: D): S? {
                return if (d !is WrappedDeclarationDescriptor<*>) {
                    val sig = signaturer.composeSignature(d)
                    if (sig != null) {
                        getByIdSignature(sig)
                    } else {
                        getByDescriptor(d)
                    }
                } else {
                    getByDescriptor(d)
                }
            }

            fun getLocal(d: D) = descriptorToSymbol[d]

            operator fun set(d: D, s: S) {
                if (s.isPublicApi) {
                    require(d is TypeParameterDescriptor)
                    idSigToSymbol[s.signature] = s
                } else {
                    descriptorToSymbol[d] = s
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

        private var currentScope: Scope? = null

        override fun get(d: D): S? {
            val scope = currentScope ?: return null
            return scope[d]
        }

        override fun set(d: D, s: S) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[d] = s
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

        fun enterScope(owner: DeclarationDescriptor) {
            currentScope = Scope(owner, currentScope)
        }

        fun leaveScope(owner: DeclarationDescriptor) {
            currentScope?.owner.let {
                assert(it == owner) { "Unexpected leaveScope: owner=$owner, currentScope.owner=$it" }
            }

            currentScope = currentScope?.parent

            if (currentScope != null && unboundSymbols.isNotEmpty()) {
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
    private val scopedTypeParameterSymbolTable = ScopedSymbolTable<TypeParameterDescriptor, IrTypeParameter, IrTypeParameterSymbol>()
    private val valueParameterSymbolTable = ScopedSymbolTable<ParameterDescriptor, IrValueParameter, IrValueParameterSymbol>()
    private val variableSymbolTable = ScopedSymbolTable<VariableDescriptor, IrVariable, IrVariableSymbol>()
    private val localDelegatedPropertySymbolTable =
        ScopedSymbolTable<VariableDescriptorWithAccessors, IrLocalDelegatedProperty, IrLocalDelegatedPropertySymbol>()
    private val scopedSymbolTables =
        listOf(valueParameterSymbolTable, variableSymbolTable, scopedTypeParameterSymbolTable, localDelegatedPropertySymbolTable)

    fun referenceExternalPackageFragment(descriptor: PackageFragmentDescriptor) =
        externalPackageFragmentTable.referenced(descriptor) { IrExternalPackageFragmentSymbolImpl(descriptor) }

    fun declareExternalPackageFragment(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        return externalPackageFragmentTable.declare(
            descriptor,
            { IrExternalPackageFragmentSymbolImpl(descriptor) },
            { IrExternalPackageFragmentImpl(it) }
        )
    }

    fun declareAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: ClassDescriptor
    ): IrAnonymousInitializer =
        IrAnonymousInitializerImpl(
            startOffset, endOffset, origin,
            IrAnonymousInitializerSymbolImpl(descriptor)
        )

    fun listExistedScripts() = scriptSymbolTable.descriptorToSymbol.map { it.value }

    fun declareScript(
        descriptor: ScriptDescriptor,
        scriptFactory: (IrScriptSymbol) -> IrScript = { symbol: IrScriptSymbol ->
            IrScriptImpl(symbol, descriptor.name)
        }
    ): IrScript {
        return scriptSymbolTable.declare(
            descriptor,
            { IrScriptSymbolImpl(descriptor) },
            scriptFactory
        )
    }

    private fun createClassSymbol(descriptor: ClassDescriptor): IrClassSymbol {
        return signaturer.composeSignature(descriptor)?.let { IrClassPublicSymbolImpl(descriptor, it) } ?: IrClassSymbolImpl(descriptor)
    }

    fun declareClass(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
        modality: Modality = descriptor.modality, visibility: Visibility = descriptor.visibility,
        classFactory: (IrClassSymbol) -> IrClass = {
            IrClassImpl(startOffset, endOffset, origin, it, modality, visibility).apply { metadata = MetadataSource.Class(it.descriptor) }
        }
    ): IrClass {
        return classSymbolTable.declare(
            descriptor,
            { createClassSymbol(descriptor) },
            classFactory
        )
    }

    fun declareClassFromLinker(descriptor: ClassDescriptor, sig: IdSignature, factory: (IrClassSymbol) -> IrClass): IrClass {
        return classSymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrClassPublicSymbolImpl(descriptor, sig) }, factory)
            } else {
                declare(descriptor, { IrClassSymbolImpl(descriptor) }, factory)
            }
        }
    }

    override fun referenceClass(descriptor: ClassDescriptor) =
        classSymbolTable.referenced(descriptor) { createClassSymbol(descriptor) }

    override fun referenceClassFromLinker(descriptor: ClassDescriptor, sig: IdSignature): IrClassSymbol =
        classSymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrClassPublicSymbolImpl(descriptor, sig) }
            else referenced(descriptor) { IrClassSymbolImpl(descriptor) }
        }

    val unboundClasses: Set<IrClassSymbol> get() = classSymbolTable.unboundSymbols

    private fun createConstructorSymbol(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return signaturer.composeSignature(descriptor)?.let { IrConstructorPublicSymbolImpl(descriptor, it) } ?: IrConstructorSymbolImpl(
            descriptor
        )
    }

    fun declareConstructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: ClassConstructorDescriptor,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor = {
            IrConstructorImpl(startOffset, endOffset, origin, it, IrUninitializedType).apply {
                metadata = MetadataSource.Function(it.descriptor)
            }
        }
    ): IrConstructor =
        constructorSymbolTable.declare(
            descriptor,
            { createConstructorSymbol(descriptor) },
            constructorFactory
        )

    override fun referenceConstructor(descriptor: ClassConstructorDescriptor) =
        constructorSymbolTable.referenced(descriptor) { createConstructorSymbol(descriptor) }

    fun declareConstructorFromLinker(
        descriptor: ClassConstructorDescriptor,
        sig: IdSignature,
        constructorFactory: (IrConstructorSymbol) -> IrConstructor
    ): IrConstructor {
        return constructorSymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrConstructorPublicSymbolImpl(descriptor, sig) }, constructorFactory)
            } else {
                declare(descriptor, { IrConstructorSymbolImpl(descriptor) }, constructorFactory)
            }
        }
    }

    override fun referenceConstructorFromLinker(descriptor: ClassConstructorDescriptor, sig: IdSignature): IrConstructorSymbol =
        constructorSymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrConstructorPublicSymbolImpl(descriptor, sig) }
            else referenced(descriptor) { IrConstructorSymbolImpl(descriptor) }
        }

    val unboundConstructors: Set<IrConstructorSymbol> get() = constructorSymbolTable.unboundSymbols

    private fun createEnumEntrySymbol(descriptor: ClassDescriptor): IrEnumEntrySymbol {
        return signaturer.composeEnumEntrySignature(descriptor)?.let { IrEnumEntryPublicSymbolImpl(descriptor, it) }
            ?: IrEnumEntrySymbolImpl(descriptor)
    }

    fun declareEnumEntry(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry = { IrEnumEntryImpl(startOffset, endOffset, origin, it) }
    ): IrEnumEntry =
        enumEntrySymbolTable.declare(
            descriptor,
            { createEnumEntrySymbol(descriptor) },
            factory
        )

    fun declareEnumEntryFromLinker(
        descriptor: ClassDescriptor,
        sig: IdSignature,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry
    ): IrEnumEntry {
        return enumEntrySymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrEnumEntryPublicSymbolImpl(descriptor, sig) }, factory)
            } else {
                declare(descriptor, { IrEnumEntrySymbolImpl(descriptor) }, factory)
            }
        }
    }

    override fun referenceEnumEntry(descriptor: ClassDescriptor) =
        enumEntrySymbolTable.referenced(descriptor) { createEnumEntrySymbol(descriptor) }

    override fun referenceEnumEntryFromLinker(descriptor: ClassDescriptor, sig: IdSignature) =
        enumEntrySymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrEnumEntryPublicSymbolImpl(descriptor, sig) } else
                referenced(descriptor) { IrEnumEntrySymbolImpl(descriptor) }
        }

    val unboundEnumEntries: Set<IrEnumEntrySymbol> get() = enumEntrySymbolTable.unboundSymbols

    private fun createFieldSymbol(descriptor: PropertyDescriptor): IrFieldSymbol {
        return IrFieldSymbolImpl(descriptor)
    }

    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType,
        visibility: Visibility? = null,
        fieldFactory: (IrFieldSymbol) -> IrField = {
            IrFieldImpl(startOffset, endOffset, origin, it, type, visibility ?: it.descriptor.visibility).apply {
                metadata = MetadataSource.Property(it.descriptor)
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

    fun declareFieldFromLinker(descriptor: PropertyDescriptor, sig: IdSignature, factory: (IrFieldSymbol) -> IrField): IrField {
        return fieldSymbolTable.run {
            require(sig.isLocal)
            declare(descriptor, { IrFieldSymbolImpl(descriptor) }, factory)
        }
    }

    override fun referenceField(descriptor: PropertyDescriptor) =
        fieldSymbolTable.referenced(descriptor) { createFieldSymbol(descriptor) }

    override fun referenceFieldFromLinker(descriptor: PropertyDescriptor, sig: IdSignature) =
        fieldSymbolTable.run {
            require(sig.isLocal)
            referenced(descriptor) { IrFieldSymbolImpl(descriptor) }
        }

    val unboundFields: Set<IrFieldSymbol> get() = fieldSymbolTable.unboundSymbols

    @Deprecated(message = "Use declareProperty/referenceProperty", level = DeprecationLevel.WARNING)
    val propertyTable = HashMap<PropertyDescriptor, IrProperty>()

    override fun referenceProperty(descriptor: PropertyDescriptor, generate: () -> IrProperty): IrProperty =
        propertyTable.getOrPut(descriptor, generate)

    private fun createPropertySymbol(descriptor: PropertyDescriptor): IrPropertySymbol {
        return signaturer.composeSignature(descriptor)?.let { IrPropertyPublicSymbolImpl(descriptor, it) } ?: IrPropertySymbolImpl(
            descriptor
        )

    }

    fun declareProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        @Suppress("DEPRECATION") isDelegated: Boolean = descriptor.isDelegated,
        propertyFactory: (IrPropertySymbol) -> IrProperty = { symbol ->
            IrPropertyImpl(startOffset, endOffset, origin, symbol, isDelegated = isDelegated).apply {
                metadata = MetadataSource.Property(symbol.descriptor)
            }
        }
    ): IrProperty =
        propertySymbolTable.declare(
            descriptor,
            { createPropertySymbol(descriptor) },
            propertyFactory
        )

    fun declarePropertyFromLinker(descriptor: PropertyDescriptor, sig: IdSignature, factory: (IrPropertySymbol) -> IrProperty): IrProperty {
        return propertySymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrPropertyPublicSymbolImpl(descriptor, sig) }, factory)
            } else {
                declare(descriptor, { IrPropertySymbolImpl(descriptor) }, factory)
            }
        }
    }

    fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol =
        propertySymbolTable.referenced(descriptor) { createPropertySymbol(descriptor) }

    override fun referencePropertyFromLinker(descriptor: PropertyDescriptor, sig: IdSignature): IrPropertySymbol =
        propertySymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrPropertyPublicSymbolImpl(descriptor, sig) }
            else referenced(descriptor) { IrPropertySymbolImpl(descriptor) }
        }

    val unboundProperties: Set<IrPropertySymbol> get() = propertySymbolTable.unboundSymbols

    private fun createTypeAliasSymbol(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol {
        return signaturer.composeSignature(descriptor)?.let { IrTypeAliasPublicSymbolImpl(descriptor, it) } ?: IrTypeAliasSymbolImpl(
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
                declare(sig, descriptor, { IrTypeAliasPublicSymbolImpl(descriptor, sig) }, factory)
            } else {
                declare(descriptor, { IrTypeAliasSymbolImpl(descriptor) }, factory)
            }
        }
    }

    override fun referenceTypeAliasFromLinker(descriptor: TypeAliasDescriptor, sig: IdSignature) =
        typeAliasSymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrTypeAliasPublicSymbolImpl(descriptor, sig) } else
                referenced(descriptor) { IrTypeAliasSymbolImpl(descriptor) }
        }

    fun declareTypeAlias(descriptor: TypeAliasDescriptor, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias =
        typeAliasSymbolTable.declare(descriptor, { createTypeAliasSymbol(descriptor) }, factory)

    val unboundTypeAliases: Set<IrTypeAliasSymbol> get() = typeAliasSymbolTable.unboundSymbols

    private fun createSimpleFunctionSymbol(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
        return signaturer.composeSignature(descriptor)?.let { IrSimpleFunctionPublicSymbolImpl(descriptor, it) }
            ?: IrSimpleFunctionSymbolImpl(descriptor)
    }

    fun declareSimpleFunction(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction = {
            IrFunctionImpl(startOffset, endOffset, origin, it, IrUninitializedType).apply {
                metadata = MetadataSource.Function(it.descriptor)
            }
        }
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.declare(
            descriptor,
            { createSimpleFunctionSymbol(descriptor) },
            functionFactory
        )
    }

    fun declareSimpleFunctionFromLinker(
        descriptor: FunctionDescriptor,
        sig: IdSignature,
        functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        return simpleFunctionSymbolTable.run {
            if (sig.isPublic) {
                declare(sig, descriptor, { IrSimpleFunctionPublicSymbolImpl(descriptor, sig) }, functionFactory)
            } else {
                declare(descriptor, { IrSimpleFunctionSymbolImpl(descriptor) }, functionFactory)
            }
        }
    }

    override fun referenceSimpleFunction(descriptor: FunctionDescriptor) =
        simpleFunctionSymbolTable.referenced(descriptor) { createSimpleFunctionSymbol(descriptor) }

    override fun referenceSimpleFunctionFromLinker(descriptor: FunctionDescriptor, sig: IdSignature): IrSimpleFunctionSymbol {
        return simpleFunctionSymbolTable.run {
            if (sig.isPublic) referenced(sig) { IrSimpleFunctionPublicSymbolImpl(descriptor, sig) } else
                referenced(descriptor) { IrSimpleFunctionSymbolImpl(descriptor) }
        }
    }

    override fun referenceDeclaredFunction(descriptor: FunctionDescriptor) =
        simpleFunctionSymbolTable.referenced(descriptor) { throw AssertionError("Function is not declared: $descriptor") }

    val unboundSimpleFunctions: Set<IrSimpleFunctionSymbol> get() = simpleFunctionSymbolTable.unboundSymbols

    private fun createTypeParameterSymbol(descriptor: TypeParameterDescriptor): IrTypeParameterSymbol {
        return IrTypeParameterSymbolImpl(descriptor)
    }

    fun declareGlobalTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter = { IrTypeParameterImpl(startOffset, endOffset, origin, it) }
    ): IrTypeParameter =
        globalTypeParameterSymbolTable.declare(
            descriptor,
            { createTypeParameterSymbol(descriptor) },
            typeParameterFactory
        )

    fun declareGlobalTypeParameterFromLinker(
        descriptor: TypeParameterDescriptor,
        sig: IdSignature,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
        require(sig.isLocal)
        return globalTypeParameterSymbolTable.declare(descriptor, { IrTypeParameterSymbolImpl(descriptor) }, typeParameterFactory)
    }

    fun declareScopedTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter = { IrTypeParameterImpl(startOffset, endOffset, origin, it) }
    ): IrTypeParameter =
        scopedTypeParameterSymbolTable.declare(
            descriptor,
            { createTypeParameterSymbol(descriptor) },
            typeParameterFactory
        )

    fun declareScopedTypeParameterFromLinker(
        descriptor: TypeParameterDescriptor,
        sig: IdSignature,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
        require(sig.isLocal)
        return scopedTypeParameterSymbolTable.declare(descriptor, { IrTypeParameterSymbolImpl(descriptor) }, typeParameterFactory)
    }

    val unboundTypeParameters: Set<IrTypeParameterSymbol> get() = globalTypeParameterSymbolTable.unboundSymbols

    fun declareValueParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: ParameterDescriptor,
        type: IrType,
        varargElementType: IrType? = null,
        valueParameterFactory: (IrValueParameterSymbol) -> IrValueParameter = {
            IrValueParameterImpl(startOffset, endOffset, origin, it, type, varargElementType)
        }
    ): IrValueParameter =
        valueParameterSymbolTable.declareLocal(
            descriptor,
            { IrValueParameterSymbolImpl(descriptor) },
            valueParameterFactory
        )

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

    override fun referenceTypeParameterFromLinker(classifier: TypeParameterDescriptor, sig: IdSignature): IrTypeParameterSymbol {
        require(sig.isLocal)
        return scopedTypeParameterSymbolTable.get(classifier)
            ?: globalTypeParameterSymbolTable.referenced(classifier) { IrTypeParameterSymbolImpl(classifier) }
    }

    fun declareVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptor,
        type: IrType,
        variableFactory: (IrVariableSymbol) -> IrVariable = {
            IrVariableImpl(startOffset, endOffset, origin, it, type)
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
        type: IrType
    ): IrLocalDelegatedProperty =
        localDelegatedPropertySymbolTable.declareLocal(
            descriptor,
            { IrLocalDelegatedPropertySymbolImpl(descriptor) },
            { IrLocalDelegatedPropertyImpl(startOffset, endOffset, origin, it, type) }
        )

    fun referenceLocalDelegatedProperty(descriptor: VariableDescriptorWithAccessors) =
        localDelegatedPropertySymbolTable.referenced(descriptor) {
            throw AssertionError("Undefined local delegated property referenced: $descriptor")
        }

    override fun enterScope(owner: DeclarationDescriptor) {
        scopedSymbolTables.forEach { it.enterScope(owner) }
    }

    override fun leaveScope(owner: DeclarationDescriptor) {
        scopedSymbolTables.forEach { it.leaveScope(owner) }
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
}

inline fun <T, D : DeclarationDescriptor> SymbolTable.withScope(owner: D, block: SymbolTable.(D) -> T): T {
    enterScope(owner)
    val result = block(owner)
    leaveScope(owner)
    return result
}