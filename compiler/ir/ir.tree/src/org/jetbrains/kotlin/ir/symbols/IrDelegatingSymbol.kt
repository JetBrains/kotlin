/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.IdSignature

/**
 * Used to handle `expect` declarations.
 *
 * Before actualization, [delegate] refers to the symbol of the `expect` declaration. After actualization,
 * [delegate] is replaced with the symbol of the corresponding `actual` declaration.
 *
 * A delegating symbol behaves exactly the same as its [delegate].
 *
 * [About `expect` and `actual` declarations.](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html)
 *
 * @property delegate Before actualization — the symbol of the `expect` declaration,
 * after actualization — the symbol of the corresponding `actual` declaration.
 *
 * @see org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker.handleExpectActualMapping
 * @see org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker.finalizeExpectActual
 */
abstract class IrDelegatingSymbol<DelegateSymbol, Owner, Descriptor>(var delegate: DelegateSymbol) : IrBindableSymbol<Descriptor, Owner>
        where DelegateSymbol : IrBindableSymbol<Descriptor, Owner>,
              Owner : IrSymbolOwner,
              Descriptor : DeclarationDescriptor {
    override val owner: Owner get() = delegate.owner

    @ObsoleteDescriptorBasedAPI
    override val descriptor: Descriptor get() = delegate.descriptor

    @ObsoleteDescriptorBasedAPI
    override val hasDescriptor: Boolean
        get() = delegate.hasDescriptor

    override val isBound: Boolean get() = delegate.isBound

    override val signature: IdSignature?
        get() = delegate.signature

    override fun bind(owner: Owner) = delegate.bind(owner)
    override fun hashCode() = delegate.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (delegate === other) return true
        return false
    }

    override var privateSignature: IdSignature?
        get() = delegate.privateSignature
        set(value) {
            delegate.privateSignature = value
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

fun wrapInDelegatedSymbol(delegate: IrSymbol) = when (delegate) {
    is IrClassSymbol -> IrDelegatingClassSymbolImpl(delegate)
    is IrEnumEntrySymbol -> IrDelegatingEnumEntrySymbolImpl(delegate)
    is IrSimpleFunctionSymbol -> IrDelegatingSimpleFunctionSymbolImpl(delegate)
    is IrConstructorSymbol -> IrDelegatingConstructorSymbolImpl(delegate)
    is IrPropertySymbol -> IrDelegatingPropertySymbolImpl(delegate)
    is IrTypeAliasSymbol -> IrDelegatingTypeAliasSymbolImpl(delegate)
    else -> error("Unexpected symbol to delegate to: $delegate")
}
