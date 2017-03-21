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

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*

interface IrSymbol {
    val owner : IrSymbolOwner
    val descriptor: DeclarationDescriptor
}

interface IrBindableSymbol<out D : DeclarationDescriptor, B : IrSymbolOwner> : IrSymbol{
    override val owner: B
    override val descriptor: D

    fun bind(owner: B)
}

interface IrAnonymousInitializerSymbol : IrBindableSymbol<ClassDescriptor, IrAnonymousInitializer>
interface IrClassSymbol : IrBindableSymbol<ClassDescriptor, IrClass>
interface IrEnumEntrySymbol : IrBindableSymbol<ClassDescriptor, IrEnumEntry>
interface IrFileSymbol : IrBindableSymbol<PackageFragmentDescriptor, IrFile>
interface IrFieldSymbol : IrBindableSymbol<PropertyDescriptor, IrField>

interface IrTypeParameterSymbol : IrBindableSymbol<TypeParameterDescriptor, IrTypeParameter>
interface IrValueParameterSymbol : IrBindableSymbol<ParameterDescriptor, IrValueParameter>
interface IrVariableSymbol : IrBindableSymbol<VariableDescriptor, IrVariable>

interface IrFunctionSymbol : IrSymbol {
    override val descriptor: FunctionDescriptor
}

interface IrConstructorSymbol : IrFunctionSymbol, IrBindableSymbol<ClassConstructorDescriptor, IrConstructor>
interface IrSimpleFunctionSymbol : IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrSimpleFunction>