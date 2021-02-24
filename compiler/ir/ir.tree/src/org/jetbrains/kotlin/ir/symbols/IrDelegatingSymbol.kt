/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.IdSignature

abstract class IrDelegatingSymbol<S : IrBindableSymbol<D, B>, B : IrSymbolOwner, D : DeclarationDescriptor>(var delegate: S) :
    IrBindableSymbol<D, B> {
    override val owner: B get() = delegate.owner

    @ObsoleteDescriptorBasedAPI
    override val descriptor: D get() = delegate.descriptor

    @ObsoleteDescriptorBasedAPI
    override val hasDescriptor: Boolean
        get() = delegate.hasDescriptor

    override val isBound: Boolean get() = delegate.isBound

    override val signature: IdSignature?
        get() = delegate.signature

    override fun bind(owner: B) = delegate.bind(owner)
    override fun hashCode() = delegate.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (delegate === other) return true
        return false
    }
}

class IrDelegatingClassSymbolImpl(delegate: IrClassSymbol) :
    IrClassSymbol, IrDelegatingSymbol<IrClassSymbol, IrClass, ClassDescriptor>(delegate)

class IrDelegatingEnumEntrySymbolImpl(delegate: IrEnumEntrySymbol) :
    IrEnumEntrySymbol, IrDelegatingSymbol<IrEnumEntrySymbol, IrEnumEntry, ClassDescriptor>(delegate)

class IrDelegatingSimpleFunctionSymbolImpl(delegate: IrSimpleFunctionSymbol) :
    IrSimpleFunctionSymbol, IrDelegatingSymbol<IrSimpleFunctionSymbol, IrSimpleFunction, FunctionDescriptor>(delegate)

class IrDelegatingConstructorSymbolImpl(delegate: IrConstructorSymbol) :
    IrConstructorSymbol, IrDelegatingSymbol<IrConstructorSymbol, IrConstructor, ClassConstructorDescriptor>(delegate)

class IrDelegatingPropertySymbolImpl(delegate: IrPropertySymbol) :
    IrPropertySymbol, IrDelegatingSymbol<IrPropertySymbol, IrProperty, PropertyDescriptor>(delegate)

class IrDelegatingTypeAliasSymbolImpl(delegate: IrTypeAliasSymbol) :
    IrTypeAliasSymbol, IrDelegatingSymbol<IrTypeAliasSymbol, IrTypeAlias, TypeAliasDescriptor>(delegate)

fun wrapInDelegatedSymbol(delegate: IrSymbol) = when(delegate) {
    is IrClassSymbol -> IrDelegatingClassSymbolImpl(delegate)
    is IrEnumEntrySymbol -> IrDelegatingEnumEntrySymbolImpl(delegate)
    is IrSimpleFunctionSymbol -> IrDelegatingSimpleFunctionSymbolImpl(delegate)
    is IrConstructorSymbol -> IrDelegatingConstructorSymbolImpl(delegate)
    is IrPropertySymbol -> IrDelegatingPropertySymbolImpl(delegate)
    is IrTypeAliasSymbol -> IrDelegatingTypeAliasSymbolImpl(delegate)
    else -> error("Unexpected symbol to delegate to: $delegate")
}
