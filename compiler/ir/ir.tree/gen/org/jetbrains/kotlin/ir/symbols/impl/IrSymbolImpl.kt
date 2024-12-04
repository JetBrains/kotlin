/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature

class IrFileSymbolImpl(
    descriptor: PackageFragmentDescriptor? = null,
) : IrSymbolBase<PackageFragmentDescriptor, IrFile>(descriptor), IrFileSymbol

class IrExternalPackageFragmentSymbolImpl(
    descriptor: PackageFragmentDescriptor? = null,
) : IrSymbolBase<PackageFragmentDescriptor, IrExternalPackageFragment>(descriptor), IrExternalPackageFragmentSymbol

class IrAnonymousInitializerSymbolImpl(
    descriptor: ClassDescriptor? = null,
) : IrSymbolBase<ClassDescriptor, IrAnonymousInitializer>(descriptor), IrAnonymousInitializerSymbol {

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    constructor(irClassSymbol: IrClassSymbol) : this(irClassSymbol.descriptor)
}

class IrEnumEntrySymbolImpl(
    descriptor: ClassDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ClassDescriptor, IrEnumEntry>(descriptor, signature), IrEnumEntrySymbol

class IrFieldSymbolImpl(
    descriptor: PropertyDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<PropertyDescriptor, IrField>(descriptor, signature), IrFieldSymbol

class IrClassSymbolImpl(
    descriptor: ClassDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ClassDescriptor, IrClass>(descriptor, signature), IrClassSymbol

class IrScriptSymbolImpl(
    descriptor: ScriptDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ScriptDescriptor, IrScript>(descriptor, signature), IrScriptSymbol

class IrTypeParameterSymbolImpl(
    descriptor: TypeParameterDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<TypeParameterDescriptor, IrTypeParameter>(descriptor, signature), IrTypeParameterSymbol

class IrValueParameterSymbolImpl(
    descriptor: ParameterDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ParameterDescriptor, IrValueParameter>(descriptor, signature), IrValueParameterSymbol

class IrVariableSymbolImpl(
    descriptor: VariableDescriptor? = null,
) : IrSymbolBase<VariableDescriptor, IrVariable>(descriptor), IrVariableSymbol

class IrConstructorSymbolImpl(
    descriptor: ClassConstructorDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ClassConstructorDescriptor, IrConstructor>(descriptor, signature), IrConstructorSymbol

class IrSimpleFunctionSymbolImpl(
    descriptor: FunctionDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<FunctionDescriptor, IrSimpleFunction>(descriptor, signature), IrSimpleFunctionSymbol

class IrReturnableBlockSymbolImpl(
    descriptor: FunctionDescriptor? = null,
) : IrSymbolBase<FunctionDescriptor, IrReturnableBlock>(descriptor), IrReturnableBlockSymbol

class IrPropertySymbolImpl(
    descriptor: PropertyDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<PropertyDescriptor, IrProperty>(descriptor, signature), IrPropertySymbol

class IrLocalDelegatedPropertySymbolImpl(
    descriptor: VariableDescriptorWithAccessors? = null,
) : IrSymbolBase<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>(descriptor), IrLocalDelegatedPropertySymbol

class IrTypeAliasSymbolImpl(
    descriptor: TypeAliasDescriptor? = null,
    signature: IdSignature? = null,
) : IrSymbolWithSignature<TypeAliasDescriptor, IrTypeAlias>(descriptor, signature), IrTypeAliasSymbol
