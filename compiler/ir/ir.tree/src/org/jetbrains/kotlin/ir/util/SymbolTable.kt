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
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*

class SymbolTable {
    private abstract class SymbolTableBase<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>> {
        val unboundSymbols = linkedSetOf<S>()

        protected abstract fun get(d: D): S?
        protected abstract fun set(d: D, s: S)

        inline fun declare(d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            val existing = get(d)
            val symbol = if (existing == null) {
                val new = createSymbol()
                set(d, new)
                new
            }
            else {
                unboundSymbols.remove(existing)
                existing
            }
            val owner = createOwner(symbol)
            return owner
        }

        inline fun referenced(d: D, createSymbol: () -> S): S {
            val s = get(d)
            if (s == null) {
                val new = createSymbol()
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
        : SymbolTableBase<D, B, S>()
    {
        val descriptorToSymbol = linkedMapOf<D, S>()

        override fun get(d: D): S? = descriptorToSymbol[d]

        override fun set(d: D, s: S) {
            descriptorToSymbol[d] = s
        }
    }

    private class ScopedSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>>
        : SymbolTableBase<D, B, S>()
    {
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
            val scope = currentScope ?: throw AssertionError("No active scope")
            return scope[d]
        }
        override fun set(d: D, s: S) {
            val scope = currentScope ?: throw AssertionError("No active scope")
            scope[d] = s
        }

        inline fun declareLocal(d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            val scope = currentScope ?: throw AssertionError("No active scope")
            val symbol = scope.getLocal(d) ?: createSymbol().also { scope[d] = it }
            val owner = createOwner(symbol)
            return owner
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

    private val classSymbolTable = FlatSymbolTable<ClassDescriptor, IrClass, IrClassSymbol>()
    private val constructorSymbolTable = FlatSymbolTable<ClassConstructorDescriptor, IrConstructor, IrConstructorSymbol>()
    private val enumEntrySymbolTable = FlatSymbolTable<ClassDescriptor, IrEnumEntry, IrEnumEntrySymbol>()
    private val fieldSymbolTable = FlatSymbolTable<PropertyDescriptor, IrField, IrFieldSymbol>()
    private val simpleFunctionSymbolTable = FlatSymbolTable<FunctionDescriptor, IrSimpleFunction, IrSimpleFunctionSymbol>()

    private val typeParameterSymbolTable = ScopedSymbolTable<TypeParameterDescriptor, IrTypeParameter, IrTypeParameterSymbol>()
    private val valueParameterSymbolTable = ScopedSymbolTable<ParameterDescriptor, IrValueParameter, IrValueParameterSymbol>()
    private val variableSymbolTable = ScopedSymbolTable<VariableDescriptor, IrVariable, IrVariableSymbol>()
    private val scopedSymbolTables = listOf(typeParameterSymbolTable, valueParameterSymbolTable, variableSymbolTable)

    fun declareFile(fileEntry: SourceManager.FileEntry, packageFragmentDescriptor: PackageFragmentDescriptor): IrFile =
            IrFileImpl(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor))

    fun declareExternalPackageFragment(packageFragmentDescriptor: PackageFragmentDescriptor): IrExternalPackageFragment =
            IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(packageFragmentDescriptor))

    fun declareAnonymousInitializer(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor): IrAnonymousInitializer =
            IrAnonymousInitializerImpl(
                    startOffset, endOffset, origin,
                    IrAnonymousInitializerSymbolImpl(descriptor)
            )

    fun declareClass(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor): IrClass =
            classSymbolTable.declare(
                    descriptor,
                    { IrClassSymbolImpl(descriptor) },
                    { IrClassImpl(startOffset, endOffset, origin, it) }
            )

    fun referenceClass(descriptor: ClassDescriptor) =
            classSymbolTable.referenced(descriptor) { IrClassSymbolImpl(descriptor) }

    val unboundClasses: Set<IrClassSymbol> get() = classSymbolTable.unboundSymbols

    fun declareConstructor(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassConstructorDescriptor): IrConstructor =
            constructorSymbolTable.declare(
                    descriptor,
                    { IrConstructorSymbolImpl(descriptor) },
                    { IrConstructorImpl(startOffset, endOffset, origin, it) }
            )

    fun referenceConstructor(descriptor: ClassConstructorDescriptor) =
            constructorSymbolTable.referenced(descriptor) { IrConstructorSymbolImpl(descriptor) }

    val unboundConstructors: Set<IrConstructorSymbol> get() = constructorSymbolTable.unboundSymbols

    fun declareEnumEntry(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor): IrEnumEntry =
            enumEntrySymbolTable.declare(
                    descriptor,
                    { IrEnumEntrySymbolImpl(descriptor) },
                    { IrEnumEntryImpl(startOffset, endOffset, origin, it) }
            )

    fun referenceEnumEntry(descriptor: ClassDescriptor) =
            enumEntrySymbolTable.referenced(descriptor) { IrEnumEntrySymbolImpl(descriptor) }

    val unboundEnumEntries: Set<IrEnumEntrySymbol> get() = enumEntrySymbolTable.unboundSymbols

    fun declareField(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: PropertyDescriptor): IrField =
            fieldSymbolTable.declare(
                    descriptor,
                    { IrFieldSymbolImpl(descriptor) },
                    { IrFieldImpl(startOffset, endOffset, origin, it) }
            )

    fun declareField(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: PropertyDescriptor,
                     irInitializer: IrExpressionBody?) : IrField =
            declareField(startOffset, endOffset, origin, descriptor).apply { initializer = irInitializer }

    fun referenceField(descriptor: PropertyDescriptor) =
            fieldSymbolTable.referenced(descriptor) { IrFieldSymbolImpl(descriptor) }

    val unboundFields: Set<IrFieldSymbol> get() = fieldSymbolTable.unboundSymbols

    fun declareSimpleFunction(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: FunctionDescriptor): IrSimpleFunction =
            simpleFunctionSymbolTable.declare(
                    descriptor,
                    { IrSimpleFunctionSymbolImpl(descriptor) },
                    { IrFunctionImpl(startOffset, endOffset, origin, it) }
            )

    fun referenceSimpleFunction(descriptor: FunctionDescriptor) =
            simpleFunctionSymbolTable.referenced(descriptor) { IrSimpleFunctionSymbolImpl(descriptor) }

    fun referenceDeclaredFunction(descriptor: FunctionDescriptor) =
            simpleFunctionSymbolTable.referenced(descriptor) { throw AssertionError("Function is not declared: $descriptor") }

    val unboundSimpleFunctions: Set<IrSimpleFunctionSymbol> get() = simpleFunctionSymbolTable.unboundSymbols

    fun declareTypeParameter(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: TypeParameterDescriptor) : IrTypeParameter =
            typeParameterSymbolTable.declareLocal(
                    descriptor,
                    { IrTypeParameterSymbolImpl(descriptor) },
                    { IrTypeParameterImpl(startOffset, endOffset, origin, it) }
            )

    fun referenceTypeParameter(descriptor: TypeParameterDescriptor) =
            typeParameterSymbolTable.referenced(descriptor) { throw AssertionError("Undefined type parameter referenced: $descriptor") }

    val unboundTypeParameters: Set<IrTypeParameterSymbol> get() = typeParameterSymbolTable.unboundSymbols

    fun declareValueParameter(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ParameterDescriptor): IrValueParameter =
            valueParameterSymbolTable.declareLocal(
                    descriptor,
                    { IrValueParameterSymbolImpl(descriptor) },
                    { IrValueParameterImpl(startOffset, endOffset, origin, it) }
            )

    fun introduceValueParameter(irValueParameter: IrValueParameter) {
        valueParameterSymbolTable.introduceLocal(irValueParameter.descriptor, irValueParameter.symbol)
    }

    fun referenceValueParameter(descriptor: ParameterDescriptor) =
            valueParameterSymbolTable.referenced(descriptor) {
                throw AssertionError("Undefined parameter referenced: $descriptor\n${valueParameterSymbolTable.dump()}")
            }

    val unboundValueParameters: Set<IrValueParameterSymbol> get() = valueParameterSymbolTable.unboundSymbols

    fun declareVariable(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: VariableDescriptor): IrVariable =
            variableSymbolTable.declareLocal(
                    descriptor,
                    { IrVariableSymbolImpl(descriptor) },
                    { IrVariableImpl(startOffset, endOffset, origin, it) }
            )

    fun declareVariable(
            startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin,
            descriptor: VariableDescriptor,
            irInitializerExpression: IrExpression?
    ): IrVariable =
            declareVariable(startOffset, endOffset, origin, descriptor).apply {
                initializer = irInitializerExpression
            }

    fun referenceVariable(descriptor: VariableDescriptor) =
            variableSymbolTable.referenced(descriptor) { throw AssertionError("Undefined variable referenced: $descriptor") }

    val unboundVariables: Set<IrVariableSymbol> get() = variableSymbolTable.unboundSymbols

    fun enterScope(owner: DeclarationDescriptor) {
        scopedSymbolTables.forEach { it.enterScope(owner) }
    }

    fun leaveScope(owner: DeclarationDescriptor) {
        scopedSymbolTables.forEach { it.leaveScope(owner) }
    }

    fun referenceFunction(callable: CallableDescriptor): IrFunctionSymbol =
            when (callable) {
                is ClassConstructorDescriptor ->
                    constructorSymbolTable.referenced(callable) { IrConstructorSymbolImpl(callable) }
                is FunctionDescriptor ->
                    simpleFunctionSymbolTable.referenced(callable) { IrSimpleFunctionSymbolImpl(callable) }
                else ->
                    throw IllegalArgumentException("Unexpected callable descriptor: $callable")
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

    fun referenceClassifier(classifier: ClassifierDescriptor): IrClassifierSymbol =
            when (classifier) {
                is TypeParameterDescriptor ->
                    typeParameterSymbolTable.referenced(classifier) { throw AssertionError("Undefined type parameter referenced: $classifier") }
                is ClassDescriptor ->
                    classSymbolTable.referenced(classifier) { IrClassSymbolImpl(classifier) }
                else ->
                    throw IllegalArgumentException("Unexpected classifier descriptor: $classifier")
            }
}

inline fun <T> SymbolTable.withScope(owner: DeclarationDescriptor, block: () -> T): T {
    enterScope(owner)
    val result = block()
    leaveScope(owner)
    return result
}