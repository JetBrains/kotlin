/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class IrSymbolBase<out Descriptor : DeclarationDescriptor>(
    private val _descriptor: Descriptor?,
    override val signature: IdSignature?,
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
        return if (isPublicApi)
            "Unbound public symbol ${this::class.java.simpleName}: $signature"
        else
            "Unbound private symbol " +
                    if (_descriptor != null) "${this::class.java.simpleName}: $_descriptor" else super.toString()
    }
}

abstract class IrBindableSymbolBase<out Descriptor, Owner>(
    descriptor: Descriptor?,
    signature: IdSignature?,
) : IrSymbolBase<Descriptor>(descriptor, signature), IrBindableSymbol<Descriptor, Owner>
        where Descriptor : DeclarationDescriptor,
              Owner : IrSymbolOwner {

    init {
        assert(descriptor == null || isOriginalDescriptor(descriptor)) {
            "Substituted descriptor $descriptor for ${descriptor!!.original}"
        }
        if (!isPublicApi && descriptor != null) {
            val containingDeclaration = descriptor.containingDeclaration
            assert(containingDeclaration == null || isOriginalDescriptor(containingDeclaration)) {
                "Substituted containing declaration: $containingDeclaration\nfor descriptor: $descriptor"
            }
        }
    }

    private fun isOriginalDescriptor(descriptor: DeclarationDescriptor): Boolean =
        // TODO fix declaring/referencing value parameters: compute proper original descriptor
        descriptor is ValueParameterDescriptor && isOriginalDescriptor(descriptor.containingDeclaration) ||
                descriptor == descriptor.original

    private var _owner: Owner? = null
    override val owner: Owner
        get() = _owner ?: error("${javaClass.simpleName} is unbound. Signature: $signature")

    override fun bind(owner: Owner) {
        if (_owner == null) {
            _owner = owner
        } else {
            error("${javaClass.simpleName} is already bound. Signature: $signature. Owner: ${_owner?.render()}")
        }
    }

    override val isBound: Boolean
        get() = _owner != null

    override var privateSignature: IdSignature? = null
}

class IrFileSymbolImpl(descriptor: PackageFragmentDescriptor? = null) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrFile>(descriptor, signature = null),
    IrFileSymbol

class IrExternalPackageFragmentSymbolImpl(descriptor: PackageFragmentDescriptor? = null) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrExternalPackageFragment>(descriptor, signature = null),
    IrExternalPackageFragmentSymbol

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrAnonymousInitializerSymbolImpl(descriptor: ClassDescriptor? = null) :
    IrBindableSymbolBase<ClassDescriptor, IrAnonymousInitializer>(descriptor, signature = null),
    IrAnonymousInitializerSymbol {
    constructor(irClassSymbol: IrClassSymbol) : this(irClassSymbol.descriptor)
}

class IrClassSymbolImpl(descriptor: ClassDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<ClassDescriptor, IrClass>(descriptor, signature),
    IrClassSymbol

class IrEnumEntrySymbolImpl(descriptor: ClassDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<ClassDescriptor, IrEnumEntry>(descriptor, signature),
    IrEnumEntrySymbol

class IrFieldSymbolImpl(descriptor: PropertyDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<PropertyDescriptor, IrField>(descriptor, signature),
    IrFieldSymbol

class IrTypeParameterSymbolImpl(descriptor: TypeParameterDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<TypeParameterDescriptor, IrTypeParameter>(descriptor, signature),
    IrTypeParameterSymbol

class IrValueParameterSymbolImpl(descriptor: ParameterDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<ParameterDescriptor, IrValueParameter>(descriptor, signature),
    IrValueParameterSymbol

class IrVariableSymbolImpl(descriptor: VariableDescriptor? = null) :
    IrBindableSymbolBase<VariableDescriptor, IrVariable>(descriptor, signature = null),
    IrVariableSymbol

class IrSimpleFunctionSymbolImpl(descriptor: FunctionDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<FunctionDescriptor, IrSimpleFunction>(descriptor, signature),
    IrSimpleFunctionSymbol

class IrConstructorSymbolImpl(descriptor: ClassConstructorDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<ClassConstructorDescriptor, IrConstructor>(descriptor, signature),
    IrConstructorSymbol

class IrReturnableBlockSymbolImpl(descriptor: FunctionDescriptor? = null) :
    IrBindableSymbolBase<FunctionDescriptor, IrReturnableBlock>(descriptor, signature = null),
    IrReturnableBlockSymbol

class IrPropertySymbolImpl(descriptor: PropertyDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<PropertyDescriptor, IrProperty>(descriptor, signature),
    IrPropertySymbol

class IrLocalDelegatedPropertySymbolImpl(descriptor: VariableDescriptorWithAccessors? = null) :
    IrBindableSymbolBase<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>(descriptor, signature = null),
    IrLocalDelegatedPropertySymbol

class IrTypeAliasSymbolImpl(descriptor: TypeAliasDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<TypeAliasDescriptor, IrTypeAlias>(descriptor, signature),
    IrTypeAliasSymbol

class IrScriptSymbolImpl(descriptor: ScriptDescriptor? = null, signature: IdSignature? = null) :
    IrScriptSymbol, IrBindableSymbolBase<ScriptDescriptor, IrScript>(descriptor, signature)
