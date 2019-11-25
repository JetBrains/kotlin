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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazySymbolTable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

interface IrProvider {
    fun getDeclaration(symbol: IrSymbol): IrDeclaration?
}

/**
 * Extension of [IrProvider] which always produces inheritors of [IrLazyDeclarationBase].
 * Thus, it needs [declarationStubGenerator] to be able to produce IR declarations.
 */
interface LazyIrProvider: IrProvider {
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

    fun enterScope(owner: DeclarationDescriptor)

    fun leaveScope(owner: DeclarationDescriptor)

    // Referencing by UniqId produces symbols with WrappedDescriptor
    fun referenceClass(uniqId: UniqId): IrClassSymbol
    fun referenceConstructor(uniqId: UniqId): IrConstructorSymbol
    fun referenceEnumEntry(uniqId: UniqId): IrEnumEntrySymbol
    fun referenceField(uniqId: UniqId): IrFieldSymbol
    fun referenceProperty(uniqId: UniqId): IrPropertySymbol
    fun referenceSimpleFunction(uniqId: UniqId): IrSimpleFunctionSymbol
    fun referenceTypeParameter(uniqId: UniqId): IrTypeParameterSymbol
    fun referenceTypeAlias(uniqId: UniqId): IrTypeAliasSymbol

    // Should only be called when the declaration is in a `ready` state -- with a full chain of parents,
    // type and value parameters etc.
    fun computeUniqId(declaration: IrDeclaration)
}

open class SymbolTable(val mangler: KotlinMangler? = null) : ReferenceSymbolTable {

    @Suppress("LeakingThis")
    val lazyWrapper = IrLazySymbolTable(this)

    private fun IrSymbolOwner.getUniqId() = mangler?.run {
            (this@getUniqId as? IrDeclaration)?.hashedMangle?.let { UniqId(it) } ?: UniqId.NONE
    } ?: UniqId.NONE

    fun IrSymbol.setUniqId() {
        if (isBound) {
            val oldUid = uniqId
            val newUid = owner.getUniqId()
            assert(oldUid == UniqId.NONE || oldUid == newUid)
            uniqId = newUid
        }
    }

    private abstract inner class SymbolTableBase<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>> {
        val unboundSymbols = linkedSetOf<S>()
        val unboundUniqIds = linkedSetOf<UniqId>()

        abstract fun get(d: D): S?
        abstract fun set(d: D, s: S)
        abstract fun get(uid: UniqId): S?
        abstract fun set(uid: UniqId, s: S)

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

        fun computeUniqId(b: B) {
            if (b !is IrDeclaration) return
            val symbol = b.symbol as S
            symbol.setUniqId()
            set(symbol.uniqId, symbol)
            unboundSymbols.remove(symbol)
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

        inline fun referenced(uid: UniqId, orElse: () -> S): S {
            return get(uid) ?: run {
                val new = orElse()
                assert(unboundSymbols.add(new)) {
                    "Symbol for ${new.uniqId} was already referenced"
                }
                set(uid, new)
                set(new.descriptor, new)
                new
            }
        }
    }

    private inner class FlatSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>>
        : SymbolTableBase<D, B, S>() {
        val descriptorToSymbol = linkedMapOf<D, S>()
        val uniqIdToSymbol = linkedMapOf<UniqId, S>()

        override fun get(d: D): S? = descriptorToSymbol[d]

        override fun set(d: D, s: S) {
            descriptorToSymbol[d] = s
        }

        override fun get(uid: UniqId): S? = uniqIdToSymbol[uid]
        override fun set(uid: UniqId, s: S) {
            if (uid != UniqId.NONE) {
                uniqIdToSymbol[uid] = s
            }
        }
    }

    private inner class ScopedSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>>
        : SymbolTableBase<D, B, S>() {
        inner class Scope(val owner: DeclarationDescriptor, val parent: Scope?) {
            private val descriptorToSymbol = linkedMapOf<D, S>()
            private val uniqIdToSymbol = linkedMapOf<UniqId, S>()

            operator fun get(d: D): S? =
                descriptorToSymbol[d] ?: parent?.get(d)

            fun getLocal(d: D) = descriptorToSymbol[d]

            operator fun set(d: D, s: S) {
                descriptorToSymbol[d] = s
            }

            operator fun get(uid: UniqId): S? =
                uniqIdToSymbol[uid] ?: parent?.get(uid)

            operator fun set(uid: UniqId, s: S) {
                if (uid != UniqId.NONE) {
                    uniqIdToSymbol[uid] = s
                }
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

        override fun get(d: D): S? {
            val scope = currentScope ?: return null
            return scope[d]
        }

        override fun set(d: D, s: S) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[d] = s
        }

        override fun get(uid: UniqId): S? {
            val scope = currentScope ?: return null
            return scope[uid]
        }

        override fun set(uid: UniqId, s: S) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[uid] = s
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
    private val enumEntrySymbolTable = FlatSymbolTable<ClassDescriptor, IrEnumEntry, IrEnumEntrySymbol>()
    private val fieldSymbolTable = FlatSymbolTable<PropertyDescriptor, IrField, IrFieldSymbol>()
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

    fun declareClass(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
        modality: Modality = descriptor.modality,
        classFactory: (IrClassSymbol) -> IrClass = {
            IrClassImpl(startOffset, endOffset, origin, it, modality).apply { metadata = MetadataSource.Class(it.descriptor) }
        }
    ): IrClass {
        return classSymbolTable.declare(
            descriptor,
            { IrClassSymbolImpl(descriptor) },
            classFactory
        )
    }

    override fun referenceClass(descriptor: ClassDescriptor) =
        classSymbolTable.referenced(descriptor) { IrClassSymbolImpl(descriptor) }

    override fun referenceClass(uniqId: UniqId): IrClassSymbol =
        classSymbolTable.referenced(uniqId) { IrClassSymbolImpl(uniqId) }

    val unboundClasses: Set<IrClassSymbol> get() = classSymbolTable.unboundSymbols

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
            { IrConstructorSymbolImpl(descriptor) },
            constructorFactory
        )

    override fun referenceConstructor(descriptor: ClassConstructorDescriptor) =
        constructorSymbolTable.referenced(descriptor) { IrConstructorSymbolImpl(descriptor) }

    override fun referenceConstructor(uniqId: UniqId): IrConstructorSymbol =
        constructorSymbolTable.referenced(uniqId) { IrConstructorSymbolImpl(uniqId) }

    val unboundConstructors: Set<IrConstructorSymbol> get() = constructorSymbolTable.unboundSymbols

    fun declareEnumEntry(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
        factory: (IrEnumEntrySymbol) -> IrEnumEntry = { IrEnumEntryImpl(startOffset, endOffset, origin, it) }
    ): IrEnumEntry =
        enumEntrySymbolTable.declare(
            descriptor,
            { IrEnumEntrySymbolImpl(descriptor) },
            factory
        )

    override fun referenceEnumEntry(descriptor: ClassDescriptor) =
        enumEntrySymbolTable.referenced(descriptor) { IrEnumEntrySymbolImpl(descriptor) }

    override fun referenceEnumEntry(uniqId: UniqId): IrEnumEntrySymbol =
        enumEntrySymbolTable.referenced(uniqId) { IrEnumEntrySymbolImpl(uniqId) }

    val unboundEnumEntries: Set<IrEnumEntrySymbol> get() = enumEntrySymbolTable.unboundSymbols

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
            { IrFieldSymbolImpl(descriptor) },
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

    override fun referenceField(descriptor: PropertyDescriptor) =
        fieldSymbolTable.referenced(descriptor) { IrFieldSymbolImpl(descriptor) }

    override fun referenceField(uniqId: UniqId) =
        fieldSymbolTable.referenced(uniqId) { IrFieldSymbolImpl(uniqId) }

    val unboundFields: Set<IrFieldSymbol> get() = fieldSymbolTable.unboundSymbols

    @Deprecated(message = "Use declareProperty/referenceProperty", level = DeprecationLevel.WARNING)
    val propertyTable = HashMap<PropertyDescriptor, IrProperty>()

    override fun referenceProperty(descriptor: PropertyDescriptor, generate: () -> IrProperty): IrProperty =
        propertyTable.getOrPut(descriptor, generate)

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
            { IrPropertySymbolImpl(descriptor) },
            propertyFactory
        )

    fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol =
        propertySymbolTable.referenced(descriptor) { IrPropertySymbolImpl(descriptor) }

    override fun referenceProperty(uniqId: UniqId): IrPropertySymbol =
        propertySymbolTable.referenced(uniqId) { IrPropertySymbolImpl(uniqId) }

    val unboundProperties: Set<IrPropertySymbol> get() = propertySymbolTable.unboundSymbols

    override fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol =
        typeAliasSymbolTable.referenced(descriptor) { IrTypeAliasSymbolImpl(descriptor) }

    override fun referenceTypeAlias(uniqId: UniqId): IrTypeAliasSymbol =
        typeAliasSymbolTable.referenced(uniqId) { IrTypeAliasSymbolImpl(uniqId) }

    fun declareTypeAlias(descriptor: TypeAliasDescriptor, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias =
        typeAliasSymbolTable.declare(descriptor, { IrTypeAliasSymbolImpl(descriptor) }, factory)

    val unboundTypeAliases: Set<IrTypeAliasSymbol> get() = typeAliasSymbolTable.unboundSymbols

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
            { IrSimpleFunctionSymbolImpl(descriptor) },
            functionFactory
        )
    }

    override fun referenceSimpleFunction(descriptor: FunctionDescriptor) =
        simpleFunctionSymbolTable.referenced(descriptor) { IrSimpleFunctionSymbolImpl(descriptor) }

    override fun referenceSimpleFunction(uniqId: UniqId): IrSimpleFunctionSymbol =
        simpleFunctionSymbolTable.referenced(uniqId) { IrSimpleFunctionSymbolImpl(uniqId) }

    override fun referenceDeclaredFunction(descriptor: FunctionDescriptor) =
        simpleFunctionSymbolTable.referenced(descriptor) { throw AssertionError("Function is not declared: $descriptor") }

    val unboundSimpleFunctions: Set<IrSimpleFunctionSymbol> get() = simpleFunctionSymbolTable.unboundSymbols

    fun declareGlobalTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter = { IrTypeParameterImpl(startOffset, endOffset, origin, it) }
    ): IrTypeParameter =
        globalTypeParameterSymbolTable.declare(
            descriptor,
            { IrTypeParameterSymbolImpl(descriptor) },
            typeParameterFactory
        )

    fun declareScopedTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: TypeParameterDescriptor,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter = { IrTypeParameterImpl(startOffset, endOffset, origin, it) }
    ): IrTypeParameter =
        scopedTypeParameterSymbolTable.declare(
            descriptor,
            { IrTypeParameterSymbolImpl(descriptor) },
            typeParameterFactory
        )


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
            IrTypeParameterSymbolImpl(classifier)
        }

    override fun referenceTypeParameter(uniqId: UniqId): IrTypeParameterSymbol =
        scopedTypeParameterSymbolTable.get(uniqId) ?: globalTypeParameterSymbolTable.referenced(uniqId) {
            IrTypeParameterSymbolImpl(uniqId)
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

    override fun computeUniqId(declaration: IrDeclaration) {
        if (mangler == null) return
        with(mangler) {
            if (!declaration.isExported()) return
        }
        when(declaration) {
            is IrConstructor -> constructorSymbolTable.computeUniqId(declaration)
            is IrClass -> classSymbolTable.computeUniqId(declaration)
            is IrEnumEntry -> enumEntrySymbolTable.computeUniqId(declaration)
            is IrField -> fieldSymbolTable.computeUniqId(declaration)
            is IrProperty -> propertySymbolTable.computeUniqId(declaration)
            is IrSimpleFunction -> simpleFunctionSymbolTable.computeUniqId(declaration)
            is IrTypeAlias -> typeAliasSymbolTable.computeUniqId(declaration)
            is IrTypeParameter -> globalTypeParameterSymbolTable.computeUniqId(declaration)
            else -> { /* do nothing */ }
        }
    }
}

inline fun <T, D: DeclarationDescriptor> SymbolTable.withScope(owner: D, block: SymbolTable.(D) -> T): T {
    enterScope(owner)
    val result = block(owner)
    leaveScope(owner)
    return result
}

fun IrElement.computeUniqIdForDeclarations(symbolTable: SymbolTable) {
    acceptVoid(ComputeUniqIdVisitor(symbolTable))
}

private class ComputeUniqIdVisitor(val symbolTable: SymbolTable) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        symbolTable.computeUniqId(declaration)
        super.visitDeclaration(declaration)
    }
}
