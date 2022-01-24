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

abstract class IrPublicSymbolBase<out D : DeclarationDescriptor> @OptIn(ObsoleteDescriptorBasedAPI::class) constructor(
    override val signature: IdSignature,
    private val _descriptor: D?
) : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    @Suppress("UNCHECKED_CAST")
    override val descriptor: D
        get() = _descriptor ?: (owner as IrDeclaration).toIrBasedDescriptor() as D

    @ObsoleteDescriptorBasedAPI
    override val hasDescriptor: Boolean
        get() = _descriptor != null

    override fun toString(): String {
        if (isBound) return owner.render()
        return "Unbound public symbol ${this::class.java.simpleName}: $signature"
    }
}

abstract class IrBindablePublicSymbolBase<out D : DeclarationDescriptor, B : IrSymbolOwner>(sig: IdSignature, descriptor: D?) :
    IrBindableSymbol<D, B>, IrPublicSymbolBase<D>(sig, descriptor) {

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

    private var _owner: B? = null
    override val owner: B
        get() = _owner ?: throw IllegalStateException("Symbol for $signature is unbound")

    override fun bind(owner: B) {
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

class IrClassPublicSymbolImpl(sig: IdSignature, descriptor: ClassDescriptor? = null) :
    IrBindablePublicSymbolBase<ClassDescriptor, IrClass>(sig, descriptor),
    IrClassSymbol

class IrEnumEntryPublicSymbolImpl(sig: IdSignature, descriptor: ClassDescriptor? = null) :
    IrBindablePublicSymbolBase<ClassDescriptor, IrEnumEntry>(sig, descriptor),
    IrEnumEntrySymbol

class IrSimpleFunctionPublicSymbolImpl(sig: IdSignature, descriptor: FunctionDescriptor? = null) :
    IrBindablePublicSymbolBase<FunctionDescriptor, IrSimpleFunction>(sig, descriptor),
    IrSimpleFunctionSymbol

class IrConstructorPublicSymbolImpl(sig: IdSignature, descriptor: ClassConstructorDescriptor? = null) :
    IrBindablePublicSymbolBase<ClassConstructorDescriptor, IrConstructor>(sig, descriptor),
    IrConstructorSymbol

class IrPropertyPublicSymbolImpl(sig: IdSignature, descriptor: PropertyDescriptor? = null) :
    IrBindablePublicSymbolBase<PropertyDescriptor, IrProperty>(sig, descriptor),
    IrPropertySymbol

class IrTypeAliasPublicSymbolImpl(sig: IdSignature, descriptor: TypeAliasDescriptor? = null) :
    IrBindablePublicSymbolBase<TypeAliasDescriptor, IrTypeAlias>(sig, descriptor),
    IrTypeAliasSymbol

class IrFieldPublicSymbolImpl(sig: IdSignature, descriptor: PropertyDescriptor? = null) :
    IrBindablePublicSymbolBase<PropertyDescriptor, IrField>(sig, descriptor),
    IrFieldSymbol

class IrTypeParameterPublicSymbolImpl(sig: IdSignature, descriptor: TypeParameterDescriptor? = null) :
    IrBindablePublicSymbolBase<TypeParameterDescriptor, IrTypeParameter>(sig, descriptor),
    IrTypeParameterSymbol
