/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render

@OptIn(DescriptorBasedIr::class)
abstract class IrPublicSymbolBase<out D : DeclarationDescriptor>(
    override val descriptor: D,
    override val signature: IdSignature
) : IrSymbol {
    override fun toString(): String {
        if (isBound) return owner.render()
        return "Unbound public symbol for $signature"
    }
}

abstract class IrBindablePublicSymbolBase<out D : DeclarationDescriptor, B : IrSymbolOwner>(descriptor: D, sig: IdSignature) :
    IrBindableSymbol<D, B>, IrPublicSymbolBase<D>(descriptor, sig) {

    init {
        assert(isOriginalDescriptor(descriptor)) {
            "Substituted descriptor $descriptor for ${descriptor.original}"
        }
        assert(sig.isPublic)
    }

    private fun isOriginalDescriptor(descriptor: DeclarationDescriptor): Boolean =
        descriptor is WrappedDeclarationDescriptor<*> ||
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
            throw IllegalStateException("${javaClass.simpleName} for $signature is already bound: ${owner.render()}")
        }
    }

    override val isPublicApi: Boolean = true

    override val isBound: Boolean
        get() = _owner != null
}

class IrClassPublicSymbolImpl(descriptor: ClassDescriptor, sig: IdSignature) :
    IrBindablePublicSymbolBase<ClassDescriptor, IrClass>(descriptor, sig),
    IrClassSymbol {
}

class IrEnumEntryPublicSymbolImpl(descriptor: ClassDescriptor, sig: IdSignature) :
    IrBindablePublicSymbolBase<ClassDescriptor, IrEnumEntry>(descriptor, sig),
    IrEnumEntrySymbol {
}

class IrSimpleFunctionPublicSymbolImpl(descriptor: FunctionDescriptor, sig: IdSignature) :
    IrBindablePublicSymbolBase<FunctionDescriptor, IrSimpleFunction>(descriptor, sig),
    IrSimpleFunctionSymbol {
}

class IrConstructorPublicSymbolImpl(descriptor: ClassConstructorDescriptor, sig: IdSignature) :
    IrBindablePublicSymbolBase<ClassConstructorDescriptor, IrConstructor>(descriptor, sig),
    IrConstructorSymbol {
}

class IrPropertyPublicSymbolImpl(descriptor: PropertyDescriptor, sig: IdSignature) :
    IrBindablePublicSymbolBase<PropertyDescriptor, IrProperty>(descriptor, sig),
    IrPropertySymbol {
}

class IrTypeAliasPublicSymbolImpl(descriptor: TypeAliasDescriptor, sig: IdSignature) :
    IrBindablePublicSymbolBase<TypeAliasDescriptor, IrTypeAlias>(descriptor, sig),
    IrTypeAliasSymbol {
}
