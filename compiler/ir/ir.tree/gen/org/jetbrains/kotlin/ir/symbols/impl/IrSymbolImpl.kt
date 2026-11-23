/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature

class IrFileSymbolImpl : IrSymbolBase<IrFile>(), IrFileSymbol

class IrExternalPackageFragmentSymbolImpl : IrSymbolBase<IrExternalPackageFragment>(), IrExternalPackageFragmentSymbol

class IrAnonymousInitializerSymbolImpl : IrSymbolBase<IrAnonymousInitializer>(), IrAnonymousInitializerSymbol

class IrEnumEntrySymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrEnumEntry>(signature), IrEnumEntrySymbol

class IrFieldSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrField>(signature), IrFieldSymbol

class IrClassSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrClass>(signature), IrClassSymbol

class IrScriptSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrScript>(signature), IrScriptSymbol

class IrReplSnippetSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrReplSnippet>(signature), IrReplSnippetSymbol

class IrTypeParameterSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrTypeParameter>(signature), IrTypeParameterSymbol

class IrValueParameterSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrValueParameter>(signature), IrValueParameterSymbol

class IrVariableSymbolImpl : IrSymbolBase<IrVariable>(), IrVariableSymbol

class IrConstructorSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrConstructor>(signature), IrConstructorSymbol

class IrSimpleFunctionSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrSimpleFunction>(signature), IrSimpleFunctionSymbol

class IrReturnableBlockSymbolImpl : IrSymbolBase<IrReturnableBlock>(), IrReturnableBlockSymbol

class IrPropertySymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrProperty>(signature), IrPropertySymbol

class IrLocalDelegatedPropertySymbolImpl : IrSymbolBase<IrLocalDelegatedProperty>(), IrLocalDelegatedPropertySymbol

class IrTypeAliasSymbolImpl(
    signature: IdSignature? = null,
) : IrSymbolWithSignature<IrTypeAlias>(signature), IrTypeAliasSymbol
