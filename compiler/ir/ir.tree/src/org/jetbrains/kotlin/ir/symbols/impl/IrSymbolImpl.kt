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
abstract class IrSymbolBase<out Descriptor : DeclarationDescriptor, Owner : IrSymbolOwner>(
    descriptor: Descriptor?,
    override val signature: IdSignature?,
) : IrSymbol {
    private val _descriptor: Descriptor? = descriptor

    @ObsoleteDescriptorBasedAPI
    @Suppress("UNCHECKED_CAST")
    override val descriptor: Descriptor
        get() = _descriptor ?: (owner as IrDeclaration).toIrBasedDescriptor() as Descriptor

    @ObsoleteDescriptorBasedAPI
    override val hasDescriptor: Boolean
        get() = _descriptor != null

    private var _owner: Owner? = null
    override val owner: Owner
        get() = _owner ?: error("${javaClass.simpleName} is unbound. Signature: $signature")

    override var privateSignature: IdSignature? = null

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

    override val isBound: Boolean
        get() = _owner != null

    fun bind(owner: Owner) {
        if (_owner == null) {
            _owner = owner
        } else {
            error("${javaClass.simpleName} is already bound. Signature: $signature. Owner: ${_owner?.render()}")
        }
    }

    override fun toString(): String {
        if (isBound) return owner.render()
        return if (isPublicApi)
            "Unbound public symbol ${this::class.java.simpleName}: $signature"
        else
            "Unbound private symbol " +
                    if (_descriptor != null) "${this::class.java.simpleName}: $_descriptor" else super.toString()
    }
}

class IrFileSymbolImpl(descriptor: PackageFragmentDescriptor? = null) :
    IrSymbolBase<PackageFragmentDescriptor, IrFile>(descriptor, signature = null),
    IrFileSymbol

class IrExternalPackageFragmentSymbolImpl(descriptor: PackageFragmentDescriptor? = null) :
    IrSymbolBase<PackageFragmentDescriptor, IrExternalPackageFragment>(descriptor, signature = null),
    IrExternalPackageFragmentSymbol

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrAnonymousInitializerSymbolImpl(descriptor: ClassDescriptor? = null) :
    IrSymbolBase<ClassDescriptor, IrAnonymousInitializer>(descriptor, signature = null),
    IrAnonymousInitializerSymbol {
    constructor(irClassSymbol: IrClassSymbol) : this(irClassSymbol.descriptor)
}

class IrClassSymbolImpl(descriptor: ClassDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<ClassDescriptor, IrClass>(descriptor, signature),
    IrClassSymbol

class IrEnumEntrySymbolImpl(descriptor: ClassDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<ClassDescriptor, IrEnumEntry>(descriptor, signature),
    IrEnumEntrySymbol

class IrFieldSymbolImpl(descriptor: PropertyDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<PropertyDescriptor, IrField>(descriptor, signature),
    IrFieldSymbol

class IrTypeParameterSymbolImpl(descriptor: TypeParameterDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<TypeParameterDescriptor, IrTypeParameter>(descriptor, signature),
    IrTypeParameterSymbol

class IrValueParameterSymbolImpl(descriptor: ParameterDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<ParameterDescriptor, IrValueParameter>(descriptor, signature),
    IrValueParameterSymbol

class IrVariableSymbolImpl(descriptor: VariableDescriptor? = null) :
    IrSymbolBase<VariableDescriptor, IrVariable>(descriptor, signature = null),
    IrVariableSymbol

class IrSimpleFunctionSymbolImpl(descriptor: FunctionDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<FunctionDescriptor, IrSimpleFunction>(descriptor, signature),
    IrSimpleFunctionSymbol

class IrConstructorSymbolImpl(descriptor: ClassConstructorDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<ClassConstructorDescriptor, IrConstructor>(descriptor, signature),
    IrConstructorSymbol

class IrReturnableBlockSymbolImpl(descriptor: FunctionDescriptor? = null) :
    IrSymbolBase<FunctionDescriptor, IrReturnableBlock>(descriptor, signature = null),
    IrReturnableBlockSymbol

class IrPropertySymbolImpl(descriptor: PropertyDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<PropertyDescriptor, IrProperty>(descriptor, signature),
    IrPropertySymbol

class IrLocalDelegatedPropertySymbolImpl(descriptor: VariableDescriptorWithAccessors? = null) :
    IrSymbolBase<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>(descriptor, signature = null),
    IrLocalDelegatedPropertySymbol

class IrTypeAliasSymbolImpl(descriptor: TypeAliasDescriptor? = null, signature: IdSignature? = null) :
    IrSymbolBase<TypeAliasDescriptor, IrTypeAlias>(descriptor, signature),
    IrTypeAliasSymbol

class IrScriptSymbolImpl(descriptor: ScriptDescriptor? = null, signature: IdSignature? = null) :
    IrScriptSymbol, IrSymbolBase<ScriptDescriptor, IrScript>(descriptor, signature)
