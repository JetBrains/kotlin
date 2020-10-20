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
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

interface IrSymbol {
    val owner: IrSymbolOwner

    @ObsoleteDescriptorBasedAPI
    val descriptor: DeclarationDescriptor

    val isBound: Boolean

    val signature: IdSignature?
}

val IrSymbol.isPublicApi: Boolean
    get() = signature != null

interface IrBindableSymbol<out D : DeclarationDescriptor, B : IrSymbolOwner> : IrSymbol {
    override val owner: B

    @ObsoleteDescriptorBasedAPI
    override val descriptor: D

    fun bind(owner: B)
}

interface IrPackageFragmentSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor
}

interface IrFileSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrFile>

interface IrExternalPackageFragmentSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrExternalPackageFragment>

interface IrAnonymousInitializerSymbol : IrBindableSymbol<ClassDescriptor, IrAnonymousInitializer>

interface IrEnumEntrySymbol : IrBindableSymbol<ClassDescriptor, IrEnumEntry>

interface IrFieldSymbol : IrBindableSymbol<PropertyDescriptor, IrField>

interface IrClassifierSymbol : IrSymbol, TypeConstructorMarker {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassifierDescriptor
}

interface IrClassSymbol : IrClassifierSymbol, IrBindableSymbol<ClassDescriptor, IrClass>

interface IrScriptSymbol : IrSymbol, IrBindableSymbol<ScriptDescriptor, IrScript>

interface IrTypeParameterSymbol : IrClassifierSymbol, IrBindableSymbol<TypeParameterDescriptor, IrTypeParameter>, TypeParameterMarker

interface IrValueSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ValueDescriptor

    override val owner: IrValueDeclaration
}

interface IrValueParameterSymbol : IrValueSymbol, IrBindableSymbol<ParameterDescriptor, IrValueParameter>

interface IrVariableSymbol : IrValueSymbol, IrBindableSymbol<VariableDescriptor, IrVariable>

interface IrReturnTargetSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor

    override val owner: IrReturnTarget
}

interface IrFunctionSymbol : IrReturnTargetSymbol {
    override val owner: IrFunction
}

interface IrConstructorSymbol : IrFunctionSymbol, IrBindableSymbol<ClassConstructorDescriptor, IrConstructor>

interface IrSimpleFunctionSymbol : IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrSimpleFunction>

interface IrReturnableBlockSymbol : IrReturnTargetSymbol, IrBindableSymbol<FunctionDescriptor, IrReturnableBlock>

interface IrPropertySymbol : IrBindableSymbol<PropertyDescriptor, IrProperty>

interface IrLocalDelegatedPropertySymbol : IrBindableSymbol<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>

interface IrTypeAliasSymbol : IrBindableSymbol<TypeAliasDescriptor, IrTypeAlias>
