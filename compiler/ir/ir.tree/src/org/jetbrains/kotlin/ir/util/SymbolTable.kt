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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazySymbolTable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

interface IrDeserializer {
    fun findDeserializedDeclaration(symbol: IrSymbol): IrDeclaration?
    // We need a separate method for properties, because properties
    // are treated differently in the SymbolTable.
    // See SymbolTable.propertyTable and SymbolTable.referenceProperty.
    // There was an attempt to solve this asymmetry in the symbol table
    // using property symbols, but it was not successful.
    // For now we have to live with a special treatment of properties.
    // TODO: eventually get rid of this asymmetry.
    fun findDeserializedDeclaration(propertyDescriptor: PropertyDescriptor): IrProperty?
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

    fun enterScope(owner: DeclarationDescriptor)

    fun leaveScope(owner: DeclarationDescriptor)
}

open class SymbolTable : ReferenceSymbolTable {

    val lazyWrapper = IrLazySymbolTable(this)

    private abstract class SymbolTableBase<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>> {
        val unboundSymbols = linkedSetOf<S>()

        abstract fun get(d: D): S?
        abstract fun set(d: D, s: S)

        inline fun declare(d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            val existing = get(d)
            val symbol = if (existing == null) {
                val new = createSymbol()
                set(d, new)
                new
            } else {
                unboundSymbols.remove(existing)
                existing
            }
            return createOwner(symbol)
        }

        inline fun referenced(d: D, orElse: () -> S): S {
            val s = get(d)
            if (s == null) {
                val new = orElse()
                assert(unboundSymbols.add(new)) {
                    "Symbol for ${new.descriptor} was already referenced"
                }
                set(d, new)
                return new
            }
            return s
        }
    }

    private class FlatSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>>
        : SymbolTableBase<D, B, S>() {
        val descriptorToSymbol = linkedMapOf<D, S>()

        override fun get(d: D): S? = descriptorToSymbol[d]

        override fun set(d: D, s: S) {
            descriptorToSymbol[d] = s
        }

        fun copyTo(other: FlatSymbolTable<D, B, S>) {
            for ((d, s) in descriptorToSymbol) {
                other.descriptorToSymbol[d] = s
            }
            other.unboundSymbols.addAll(unboundSymbols)
        }
    }

    private class ScopedSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>>
        : SymbolTableBase<D, B, S>() {
        inner class Scope(val owner: DeclarationDescriptor, val parent: Scope?) {
            private val descriptorToSymbol = linkedMapOf<D, S>()

            operator fun get(d: D): S? =
                descriptorToSymbol[d] ?: parent?.get(d)

            fun getLocal(d: D) = descriptorToSymbol[d]

            operator fun set(d: D, s: S) {
                descriptorToSymbol[d] = s
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

    private val externalPackageFragmentTable = FlatSymbolTable<PackageFragmentDescriptor, IrExternalPackageFragment, IrExternalPackageFragmentSymbol>()
    private val classSymbolTable = FlatSymbolTable<ClassDescriptor, IrClass, IrClassSymbol>()
    private val constructorSymbolTable = FlatSymbolTable<ClassConstructorDescriptor, IrConstructor, IrConstructorSymbol>()
    private val enumEntrySymbolTable = FlatSymbolTable<ClassDescriptor, IrEnumEntry, IrEnumEntrySymbol>()
    private val fieldSymbolTable = FlatSymbolTable<PropertyDescriptor, IrField, IrFieldSymbol>()
    private val simpleFunctionSymbolTable = FlatSymbolTable<FunctionDescriptor, IrSimpleFunction, IrSimpleFunctionSymbol>()

    private val globalTypeParameterSymbolTable = FlatSymbolTable<TypeParameterDescriptor, IrTypeParameter, IrTypeParameterSymbol>()
    private val scopedTypeParameterSymbolTable = ScopedSymbolTable<TypeParameterDescriptor, IrTypeParameter, IrTypeParameterSymbol>()
    private val valueParameterSymbolTable = ScopedSymbolTable<ParameterDescriptor, IrValueParameter, IrValueParameterSymbol>()
    private val variableSymbolTable = ScopedSymbolTable<VariableDescriptor, IrVariable, IrVariableSymbol>()
    private val scopedSymbolTables = listOf(valueParameterSymbolTable, variableSymbolTable, scopedTypeParameterSymbolTable)

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

    val unboundEnumEntries: Set<IrEnumEntrySymbol> get() = enumEntrySymbolTable.unboundSymbols

    fun declareField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        type: IrType,
        fieldFactory: (IrFieldSymbol) -> IrField = {
            IrFieldImpl(startOffset, endOffset, origin, it, type).apply {
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

    val unboundFields: Set<IrFieldSymbol> get() = fieldSymbolTable.unboundSymbols

    val propertyTable = HashMap<PropertyDescriptor, IrProperty>()
    override fun referenceProperty(descriptor: PropertyDescriptor, generate: () -> IrProperty): IrProperty =
        propertyTable.getOrPut(descriptor, generate)

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
        varargElementType: IrType? = null
    ): IrValueParameter =
        valueParameterSymbolTable.declareLocal(
            descriptor,
            { IrValueParameterSymbolImpl(descriptor) },
            { IrValueParameterImpl(startOffset, endOffset, origin, it, type, varargElementType) }
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

    val unboundValueParameters: Set<IrValueParameterSymbol> get() = valueParameterSymbolTable.unboundSymbols

    fun declareVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptor,
        type: IrType
    ): IrVariable =
        variableSymbolTable.declareLocal(
            descriptor,
            { IrVariableSymbolImpl(descriptor) },
            { IrVariableImpl(startOffset, endOffset, origin, it, type) }
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

    val unboundVariables: Set<IrVariableSymbol> get() = variableSymbolTable.unboundSymbols

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

    fun loadModule(module: IrModuleFragment) {
        module.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                // TODO should we check there are no conflicts?
                classSymbolTable.descriptorToSymbol[declaration.descriptor] = declaration.symbol
                super.visitClass(declaration)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                constructorSymbolTable.descriptorToSymbol[declaration.descriptor] = declaration.symbol
                super.visitConstructor(declaration)
            }

            override fun visitEnumEntry(declaration: IrEnumEntry) {
                enumEntrySymbolTable.descriptorToSymbol[declaration.descriptor] = declaration.symbol
                super.visitEnumEntry(declaration)
            }

            override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) {
                externalPackageFragmentTable.descriptorToSymbol[declaration.symbol.descriptor] = declaration.symbol
                super.visitExternalPackageFragment(declaration)
            }

            override fun visitField(declaration: IrField) {
                fieldSymbolTable.descriptorToSymbol[declaration.descriptor] = declaration.symbol
                super.visitField(declaration)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                simpleFunctionSymbolTable.descriptorToSymbol[declaration.descriptor] = declaration.symbol
                super.visitSimpleFunction(declaration)
            }

            override fun visitTypeParameter(declaration: IrTypeParameter) {
                // What about scoped type parameters?
                globalTypeParameterSymbolTable.descriptorToSymbol[declaration.descriptor] = declaration.symbol
                super.visitTypeParameter(declaration)
            }

            override fun visitCall(expression: IrCall) {
                expression.symbol.let {
                    when (it) {
                        is IrSimpleFunctionSymbol -> simpleFunctionSymbolTable.descriptorToSymbol[it.descriptor] = it
                        is IrConstructorSymbol -> constructorSymbolTable.descriptorToSymbol[it.descriptor] = it
                    }
                }

                super.visitCall(expression)
            }
        })
    }
}

inline fun <T, D: DeclarationDescriptor> SymbolTable.withScope(owner: D, block: SymbolTable.(D) -> T): T {
    enterScope(owner)
    val result = block(owner)
    leaveScope(owner)
    return result
}
