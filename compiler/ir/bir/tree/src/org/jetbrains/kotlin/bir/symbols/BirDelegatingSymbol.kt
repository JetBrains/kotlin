/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
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
abstract class BirDelegatingSymbol<DelegateSymbol, Owner, Descriptor>(var delegate: DelegateSymbol) : BirBindableSymbol<Descriptor, Owner>
        where DelegateSymbol : BirBindableSymbol<Descriptor, Owner>,
              Owner : BirSymbolOwner,
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

class BirDelegatingClassSymbolImpl(delegate: BirClassSymbol) :
    BirClassSymbol, BirDelegatingSymbol<BirClassSymbol, BirClass, ClassDescriptor>(delegate)

class BirDelegatingEnumEntrySymbolImpl(delegate: BirEnumEntrySymbol) :
    BirEnumEntrySymbol, BirDelegatingSymbol<BirEnumEntrySymbol, BirEnumEntry, ClassDescriptor>(delegate)

class BirDelegatingSimpleFunctionSymbolImpl(delegate: BirSimpleFunctionSymbol) :
    BirSimpleFunctionSymbol, BirDelegatingSymbol<BirSimpleFunctionSymbol, BirSimpleFunction, FunctionDescriptor>(delegate)

class BirDelegatingConstructorSymbolImpl(delegate: BirConstructorSymbol) :
    BirConstructorSymbol, BirDelegatingSymbol<BirConstructorSymbol, BirConstructor, ClassConstructorDescriptor>(delegate)

class BirDelegatingPropertySymbolImpl(delegate: BirPropertySymbol) :
    BirPropertySymbol, BirDelegatingSymbol<BirPropertySymbol, BirProperty, PropertyDescriptor>(delegate)

class BirDelegatingTypeAliasSymbolImpl(delegate: BirTypeAliasSymbol) :
    BirTypeAliasSymbol, BirDelegatingSymbol<BirTypeAliasSymbol, BirTypeAlias, TypeAliasDescriptor>(delegate)

fun wrapInDelegatedSymbol(delegate: BirSymbol) = when (delegate) {
    is BirClassSymbol -> BirDelegatingClassSymbolImpl(delegate)
    is BirEnumEntrySymbol -> BirDelegatingEnumEntrySymbolImpl(delegate)
    is BirSimpleFunctionSymbol -> BirDelegatingSimpleFunctionSymbolImpl(delegate)
    is BirConstructorSymbol -> BirDelegatingConstructorSymbolImpl(delegate)
    is BirPropertySymbol -> BirDelegatingPropertySymbolImpl(delegate)
    is BirTypeAliasSymbol -> BirDelegatingTypeAliasSymbolImpl(delegate)
    else -> error("Unexpected symbol to delegate to: $delegate")
}
