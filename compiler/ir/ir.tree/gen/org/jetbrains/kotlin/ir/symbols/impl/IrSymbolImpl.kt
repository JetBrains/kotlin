/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature

class IrFileSymbolImpl : IrSymbolBase<PackageFragmentDescriptor, IrFile>(null), IrFileSymbol

class IrExternalPackageFragmentSymbolImpl : IrSymbolBase<PackageFragmentDescriptor, IrExternalPackageFragment>(null), IrExternalPackageFragmentSymbol

class IrAnonymousInitializerSymbolImpl : IrSymbolBase<ClassDescriptor, IrAnonymousInitializer>(null), IrAnonymousInitializerSymbol

class IrEnumEntrySymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ClassDescriptor, IrEnumEntry>(null, signature), IrEnumEntrySymbol

class IrFieldSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<PropertyDescriptor, IrField>(null, signature), IrFieldSymbol

class IrClassSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ClassDescriptor, IrClass>(null, signature), IrClassSymbol

class IrScriptSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ScriptDescriptor, IrScript>(null, signature), IrScriptSymbol

class IrReplSnippetSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<Nothing, IrReplSnippet>(null, signature), IrReplSnippetSymbol

class IrTypeParameterSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<TypeParameterDescriptor, IrTypeParameter>(null, signature), IrTypeParameterSymbol

class IrValueParameterSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ParameterDescriptor, IrValueParameter>(null, signature), IrValueParameterSymbol

class IrVariableSymbolImpl : IrSymbolBase<VariableDescriptor, IrVariable>(null), IrVariableSymbol

class IrConstructorSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<ClassConstructorDescriptor, IrConstructor>(null, signature), IrConstructorSymbol

class IrSimpleFunctionSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<FunctionDescriptor, IrSimpleFunction>(null, signature), IrSimpleFunctionSymbol

class IrReturnableBlockSymbolImpl : IrSymbolBase<FunctionDescriptor, IrReturnableBlock>(null), IrReturnableBlockSymbol

class IrPropertySymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<PropertyDescriptor, IrProperty>(null, signature), IrPropertySymbol

class IrLocalDelegatedPropertySymbolImpl : IrSymbolBase<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>(null), IrLocalDelegatedPropertySymbol

class IrTypeAliasSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<TypeAliasDescriptor, IrTypeAlias>(null, signature), IrTypeAliasSymbol
