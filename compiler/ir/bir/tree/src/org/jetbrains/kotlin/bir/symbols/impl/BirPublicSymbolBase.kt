/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.toBirBasedDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.bir.util.render

/**
 * The base class for all public (wrt linkage) symbols.
 *
 * Its [signature] is never `null`.
 *
 * TODO: Merge with [BirSymbolBase] ([KT-44721](https://youtrack.jetbrains.com/issue/KT-44721))
 */
abstract class BirPublicSymbolBase<out Descriptor : DeclarationDescriptor>(
    override val signature: IdSignature,
    private val _descriptor: Descriptor?
) : BirSymbol {
    @ObsoleteDescriptorBasedAPI
    @Suppress("UNCHECKED_CAST")
    override val descriptor: Descriptor
        get() = _descriptor ?: (owner as BirDeclaration).toBirBasedDescriptor() as Descriptor

    @ObsoleteDescriptorBasedAPI
    override val hasDescriptor: Boolean
        get() = _descriptor != null

    override fun toString(): String {
        if (isBound) return owner.render()
        return "Unbound public symbol ${this::class.java.simpleName}: $signature"
    }
}

abstract class BirBindablePublicSymbolBase<out Descriptor, Owner>(
    sig: IdSignature,
    descriptor: Descriptor?,
) : BirPublicSymbolBase<Descriptor>(sig, descriptor), BirBindableSymbol<Descriptor, Owner>
        where Descriptor : DeclarationDescriptor,
              Owner : BirSymbolOwner {

    init {
        assert(descriptor == null || isOriginalDescriptor(descriptor)) {
            "Substituted descriptor $descriptor for ${descriptor!!.original}"
        }
//        assert(sig.isPubliclyVisible)
    }

    private fun isOriginalDescriptor(descriptor: DeclarationDescriptor): Boolean =
        // TODO fix declaring/referencing value parameters: compute proper original descriptor
        descriptor is ValueParameterDescriptor && isOriginalDescriptor(descriptor.containingDeclaration) ||
                descriptor == descriptor.original

    private var _owner: Owner? = null
    override val owner: Owner
        get() = _owner ?: throw IllegalStateException("Symbol for $signature is unbound")

    override fun bind(owner: Owner) {
        if (_owner == null) {
            _owner = owner
        } else {
            throw IllegalStateException("${javaClass.simpleName} for $signature is already bound: ${_owner?.render()}")
        }
    }

    override val isBound: Boolean
        get() = _owner != null

    override var privateSignature: IdSignature? = null
}

class BirClassPublicSymbolImpl(sig: IdSignature, descriptor: ClassDescriptor? = null) :
    BirBindablePublicSymbolBase<ClassDescriptor, BirClass>(sig, descriptor),
    BirClassSymbol

class BirEnumEntryPublicSymbolImpl(sig: IdSignature, descriptor: ClassDescriptor? = null) :
    BirBindablePublicSymbolBase<ClassDescriptor, BirEnumEntry>(sig, descriptor),
    BirEnumEntrySymbol

class BirSimpleFunctionPublicSymbolImpl(sig: IdSignature, descriptor: FunctionDescriptor? = null) :
    BirBindablePublicSymbolBase<FunctionDescriptor, BirSimpleFunction>(sig, descriptor),
    BirSimpleFunctionSymbol

class BirConstructorPublicSymbolImpl(sig: IdSignature, descriptor: ClassConstructorDescriptor? = null) :
    BirBindablePublicSymbolBase<ClassConstructorDescriptor, BirConstructor>(sig, descriptor),
    BirConstructorSymbol

class BirPropertyPublicSymbolImpl(sig: IdSignature, descriptor: PropertyDescriptor? = null) :
    BirBindablePublicSymbolBase<PropertyDescriptor, BirProperty>(sig, descriptor),
    BirPropertySymbol

class BirTypeAliasPublicSymbolImpl(sig: IdSignature, descriptor: TypeAliasDescriptor? = null) :
    BirBindablePublicSymbolBase<TypeAliasDescriptor, BirTypeAlias>(sig, descriptor),
    BirTypeAliasSymbol

class BirFieldPublicSymbolImpl(sig: IdSignature, descriptor: PropertyDescriptor? = null) :
    BirBindablePublicSymbolBase<PropertyDescriptor, BirField>(sig, descriptor),
    BirFieldSymbol

class BirTypeParameterPublicSymbolImpl(sig: IdSignature, descriptor: TypeParameterDescriptor? = null) :
    BirBindablePublicSymbolBase<TypeParameterDescriptor, BirTypeParameter>(sig, descriptor),
    BirTypeParameterSymbol

class BirScriptPublicSymbolImpl(sig: IdSignature, descriptor: ScriptDescriptor? = null) :
    BirBindablePublicSymbolBase<ScriptDescriptor, BirScript>(sig, descriptor), BirScriptSymbol
