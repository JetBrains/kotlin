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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*

class SymbolTable {
    private class SpecializedSymbolTable<D : DeclarationDescriptor, B : IrSymbolOwner, S : IrBindableSymbol<D, B>> {
        val descriptorToSymbol = linkedMapOf<D, S>()
        val unboundSymbols = linkedSetOf<S>()

        inline fun declare(d: D, createSymbol: () -> S, createOwner: (S) -> B): B {
            val symbol = declared(d, createSymbol)
            val owner = createOwner(symbol)
            symbol.bind(owner)
            return owner
        }

        inline fun declared(d: D, createSymbol: () -> S): S {
            val existing = descriptorToSymbol[d]
            return if (existing == null) {
                val new = createSymbol()
                descriptorToSymbol[d] = new
                new
            }
            else {
                assert(unboundSymbols.remove(existing)) {
                    "Symbol for $d is already bound"
                }
                existing
            }
        }

        inline fun referenced(d: D, createSymbol: () -> S): S =
                descriptorToSymbol.getOrPut(d) {
                    createSymbol().also {
                        unboundSymbols.add(it)
                    }
                }
    }

    private val classSymbolTable = SpecializedSymbolTable<ClassDescriptor, IrClass, IrClassSymbol>()
    private val constructorSymbolTable = SpecializedSymbolTable<ClassConstructorDescriptor, IrConstructor, IrConstructorSymbol>()
    private val enumEntrySymbolTable = SpecializedSymbolTable<ClassDescriptor, IrEnumEntry, IrEnumEntrySymbol>()
    private val fieldSymbolTable = SpecializedSymbolTable<PropertyDescriptor, IrField, IrFieldSymbol>()
    private val simpleFunctionSymbolTable = SpecializedSymbolTable<FunctionDescriptor, IrSimpleFunction, IrSimpleFunctionSymbol>()
    private val typeParameterSymbolTable = SpecializedSymbolTable<TypeParameterDescriptor, IrTypeParameter, IrTypeParameterSymbol>()
    private val valueParameterSymbolTable = SpecializedSymbolTable<ParameterDescriptor, IrValueParameter, IrValueParameterSymbol>()
    private val variableSymbolTable = SpecializedSymbolTable<VariableDescriptor, IrVariable, IrVariableSymbol>()

    fun declareFile(fileEntry: SourceManager.FileEntry, packageFragmentDescriptor: PackageFragmentDescriptor): IrFile =
            IrFileImpl(
                    fileEntry, packageFragmentDescriptor,
                    IrFileSymbolImpl(packageFragmentDescriptor)
            )

    fun declareAnonymousInitializer(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor): IrAnonymousInitializer =
            IrAnonymousInitializerImpl(
                    startOffset, endOffset, origin, descriptor,
                    IrAnonymousInitializerSymbolImpl(descriptor)
            )

    fun declareClass(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor): IrClass =
            classSymbolTable.declare(
                    descriptor,
                    { IrClassSymbolImpl(descriptor) },
                    { IrClassImpl(startOffset, endOffset, origin, descriptor, it) }
            )


    fun referenceClass(descriptor: ClassDescriptor) =
            classSymbolTable.referenced(descriptor) { IrClassSymbolImpl(descriptor) }

    val unboundClasses: Set<IrClassSymbol> get() = classSymbolTable.unboundSymbols

    fun declareConstructor(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassConstructorDescriptor): IrConstructor =
            constructorSymbolTable.declare(
                    descriptor,
                    { IrConstructorSymbolImpl(descriptor) },
                    { IrConstructorImpl(startOffset, endOffset, origin, descriptor, it) }
            )

    fun referenceConstructor(descriptor: ClassConstructorDescriptor) =
            constructorSymbolTable.referenced(descriptor) { IrConstructorSymbolImpl(descriptor) }

    val unboundConstructors: Set<IrConstructorSymbol> get() = constructorSymbolTable.unboundSymbols

    fun declareEnumEntry(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor): IrEnumEntry =
            enumEntrySymbolTable.declare(
                    descriptor,
                    { IrEnumEntrySymbolImpl(descriptor) },
                    { IrEnumEntryImpl(startOffset, endOffset, origin, descriptor, it) }
            )

    fun referenceEnumEntry(descriptor: ClassDescriptor) =
            enumEntrySymbolTable.referenced(descriptor) { IrEnumEntrySymbolImpl(descriptor) }

    val unboundEnumEntries: Set<IrEnumEntrySymbol> get() = enumEntrySymbolTable.unboundSymbols

    fun declareField(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: PropertyDescriptor): IrField =
            fieldSymbolTable.declare(
                    descriptor,
                    { IrFieldSymbolImpl(descriptor) },
                    { IrFieldImpl(startOffset, endOffset, origin, descriptor, it) }
            )

    fun referenceField(descriptor: PropertyDescriptor) =
            fieldSymbolTable.referenced(descriptor) { IrFieldSymbolImpl(descriptor) }

    val unboundFields: Set<IrFieldSymbol> get() = fieldSymbolTable.unboundSymbols

    fun declareSimpleFunction(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: FunctionDescriptor): IrSimpleFunction =
            simpleFunctionSymbolTable.declare(
                    descriptor,
                    { IrSimpleFunctionSymbolImpl(descriptor) },
                    { IrFunctionImpl(startOffset, endOffset, origin, descriptor, it) }
            )

    fun referenceSimpleFunction(descriptor: FunctionDescriptor) =
            simpleFunctionSymbolTable.referenced(descriptor) { IrSimpleFunctionSymbolImpl(descriptor) }

    val unboundSimpleFunctions: Set<IrSimpleFunctionSymbol> get() = simpleFunctionSymbolTable.unboundSymbols

    fun declareTypeParameter(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: TypeParameterDescriptor) : IrTypeParameter =
            typeParameterSymbolTable.declare(
                    descriptor,
                    { IrTypeParameterSymbolImpl(descriptor) },
                    { IrTypeParameterImpl(startOffset, endOffset, origin, descriptor, it) }
            )

    fun referenceTypeParameter(descriptor: TypeParameterDescriptor) =
            typeParameterSymbolTable.referenced(descriptor) { IrTypeParameterSymbolImpl(descriptor) }

    val unboundTypeParameters: Set<IrTypeParameterSymbol> get() = typeParameterSymbolTable.unboundSymbols

    fun declareValueParameter(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ParameterDescriptor): IrValueParameter =
            valueParameterSymbolTable.declare(
                    descriptor,
                    { IrValueParameterSymbolImpl(descriptor) },
                    { IrValueParameterImpl(startOffset, endOffset, origin, descriptor, it) }
            )

    fun referenceValueParameter(descriptor: ParameterDescriptor) =
            valueParameterSymbolTable.referenced(descriptor) { IrValueParameterSymbolImpl(descriptor) }

    val unboundValueParameters: Set<IrValueParameterSymbol> get() = valueParameterSymbolTable.unboundSymbols

    fun declareVariable(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: VariableDescriptor): IrVariable =
            variableSymbolTable.declare(
                    descriptor,
                    { IrVariableSymbolImpl(descriptor) },
                    { IrVariableImpl(startOffset, endOffset, origin, descriptor, it) }
            )

    fun referenceVariable(descriptor: VariableDescriptor) =
            variableSymbolTable.referenced(descriptor) { IrVariableSymbolImpl(descriptor) }

    val unboundVariables: Set<IrVariableSymbol> get() = variableSymbolTable.unboundSymbols
}