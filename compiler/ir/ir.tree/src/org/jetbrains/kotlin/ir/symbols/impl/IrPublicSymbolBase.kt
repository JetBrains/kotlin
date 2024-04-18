/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render

/**
 * The base class for all public (wrt linkage) symbols.
 *
 * Its [signature] is never `null`.
 *
 * TODO: Merge with [IrSymbolBase] ([KT-44721](https://youtrack.jetbrains.com/issue/KT-44721))
 */
abstract class IrPublicSymbolBase<out Descriptor : DeclarationDescriptor>(
    override val signature: IdSignature,
    private val _descriptor: Descriptor?
) : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    @Suppress("UNCHECKED_CAST")
    override val descriptor: Descriptor
        get() = _descriptor ?: (owner as IrDeclaration).toIrBasedDescriptor() as Descriptor

    @ObsoleteDescriptorBasedAPI
    override val hasDescriptor: Boolean
        get() = _descriptor != null

    override fun toString(): String {
        if (isBound) return owner.render()
        return "Unbound public symbol ${this::class.java.simpleName}: $signature"
    }
}

abstract class IrBindablePublicSymbolBase<out Descriptor, Owner>(
    sig: IdSignature,
    descriptor: Descriptor?,
) : IrPublicSymbolBase<Descriptor>(sig, descriptor), IrBindableSymbol<Descriptor, Owner>
        where Descriptor : DeclarationDescriptor,
              Owner : IrSymbolOwner {

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

class IrClassPublicSymbolImpl(descriptor: ClassDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<ClassDescriptor, IrClass>(signature, descriptor),
    IrClassSymbol

class IrEnumEntryPublicSymbolImpl(descriptor: ClassDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<ClassDescriptor, IrEnumEntry>(signature, descriptor),
    IrEnumEntrySymbol

class IrSimpleFunctionPublicSymbolImpl(descriptor: FunctionDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<FunctionDescriptor, IrSimpleFunction>(signature, descriptor),
    IrSimpleFunctionSymbol

class IrConstructorPublicSymbolImpl(descriptor: ClassConstructorDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<ClassConstructorDescriptor, IrConstructor>(signature, descriptor),
    IrConstructorSymbol

class IrPropertyPublicSymbolImpl(descriptor: PropertyDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<PropertyDescriptor, IrProperty>(signature, descriptor),
    IrPropertySymbol

class IrTypeAliasPublicSymbolImpl(descriptor: TypeAliasDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<TypeAliasDescriptor, IrTypeAlias>(signature, descriptor),
    IrTypeAliasSymbol

class IrFieldPublicSymbolImpl(descriptor: PropertyDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<PropertyDescriptor, IrField>(signature, descriptor),
    IrFieldSymbol

class IrTypeParameterPublicSymbolImpl(descriptor: TypeParameterDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<TypeParameterDescriptor, IrTypeParameter>(signature, descriptor),
    IrTypeParameterSymbol

class IrScriptPublicSymbolImpl(descriptor: ScriptDescriptor? = null, signature: IdSignature) :
    IrBindablePublicSymbolBase<ScriptDescriptor, IrScript>(signature, descriptor), IrScriptSymbol
