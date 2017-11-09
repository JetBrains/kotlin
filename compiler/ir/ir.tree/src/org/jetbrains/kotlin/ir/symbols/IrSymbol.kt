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
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock

interface IrSymbol {
    val owner : IrSymbolOwner
    val descriptor: DeclarationDescriptor
    val isBound: Boolean
}

interface IrBindableSymbol<out D : DeclarationDescriptor, B : IrSymbolOwner> : IrSymbol {
    override val owner: B
    override val descriptor: D

    fun bind(owner: B)
}

interface IrPackageFragmentSymbol : IrSymbol {
    override val descriptor: PackageFragmentDescriptor
}
interface IrFileSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrFile>
interface IrExternalPackageFragmentSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrExternalPackageFragment>

interface IrAnonymousInitializerSymbol : IrBindableSymbol<ClassDescriptor, IrAnonymousInitializer>
interface IrEnumEntrySymbol : IrBindableSymbol<ClassDescriptor, IrEnumEntry>

interface IrFieldSymbol : IrBindableSymbol<PropertyDescriptor, IrField>

interface IrClassifierSymbol : IrSymbol {
    override val descriptor: ClassifierDescriptor
}
interface IrClassSymbol : IrClassifierSymbol, IrBindableSymbol<ClassDescriptor, IrClass>
interface IrTypeParameterSymbol : IrClassifierSymbol, IrBindableSymbol<TypeParameterDescriptor, IrTypeParameter>

interface IrValueSymbol : IrSymbol {
    override val descriptor: ValueDescriptor
}
interface IrValueParameterSymbol : IrValueSymbol, IrBindableSymbol<ParameterDescriptor, IrValueParameter>
interface IrVariableSymbol : IrValueSymbol, IrBindableSymbol<VariableDescriptor, IrVariable>

interface IrFunctionSymbol : IrSymbol {
    override val descriptor: FunctionDescriptor
}
interface IrConstructorSymbol : IrFunctionSymbol, IrBindableSymbol<ClassConstructorDescriptor, IrConstructor>
interface IrSimpleFunctionSymbol : IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrSimpleFunction>

interface IrReturnableBlockSymbol : IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrReturnableBlock>
