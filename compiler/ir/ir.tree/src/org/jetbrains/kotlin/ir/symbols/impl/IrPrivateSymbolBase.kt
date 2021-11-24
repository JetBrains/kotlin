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
abstract class IrSymbolBase<out D : DeclarationDescriptor>(
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
        return "Unbound private symbol ${super.toString()}"
    }
}

abstract class IrBindableSymbolBase<out D : DeclarationDescriptor, B : IrSymbolOwner>(descriptor: D?, override val signature: IdSignature?) :
    IrBindableSymbol<D, B>, IrSymbolBase<D>(descriptor) {

    init {
        assert(descriptor == null || isOriginalDescriptor(descriptor)) {
            "Substituted descriptor $descriptor for ${descriptor!!.original}"
        }
        if (descriptor != null) {
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

    private var _owner: B? = null
    override val owner: B
        get() = _owner ?: throw IllegalStateException("Symbol with ${javaClass.simpleName} is unbound")

    override fun bind(owner: B) {
        if (_owner == null) {
            _owner = owner
        } else {
            throw IllegalStateException("${javaClass.simpleName} is already bound: ${_owner?.render()}")
        }
    }

    override val isBound: Boolean
        get() = _owner != null

    override val isPublicApi: Boolean
        get() = false
}

class IrFileSymbolImpl(descriptor: PackageFragmentDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrFile>(descriptor, signature),
    IrFileSymbol

class IrExternalPackageFragmentSymbolImpl(descriptor: PackageFragmentDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrExternalPackageFragment>(descriptor, signature),
    IrExternalPackageFragmentSymbol

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrAnonymousInitializerSymbolImpl(descriptor: ClassDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<ClassDescriptor, IrAnonymousInitializer>(descriptor, signature),
    IrAnonymousInitializerSymbol {
    constructor(irClassSymbol: IrClassSymbol, signature: IdSignature? = null) : this(irClassSymbol.descriptor, signature)
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

class IrVariableSymbolImpl(descriptor: VariableDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<VariableDescriptor, IrVariable>(descriptor, signature),
    IrVariableSymbol

class IrSimpleFunctionSymbolImpl(descriptor: FunctionDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<FunctionDescriptor, IrSimpleFunction>(descriptor, signature),
    IrSimpleFunctionSymbol

class IrConstructorSymbolImpl(descriptor: ClassConstructorDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<ClassConstructorDescriptor, IrConstructor>(descriptor, signature),
    IrConstructorSymbol

class IrReturnableBlockSymbolImpl(descriptor: FunctionDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<FunctionDescriptor, IrReturnableBlock>(descriptor, signature),
    IrReturnableBlockSymbol

class IrPropertySymbolImpl(descriptor: PropertyDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<PropertyDescriptor, IrProperty>(descriptor, signature),
    IrPropertySymbol

class IrLocalDelegatedPropertySymbolImpl(descriptor: VariableDescriptorWithAccessors? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>(descriptor, signature),
    IrLocalDelegatedPropertySymbol

class IrTypeAliasSymbolImpl(descriptor: TypeAliasDescriptor? = null, signature: IdSignature? = null) :
    IrBindableSymbolBase<TypeAliasDescriptor, IrTypeAlias>(descriptor, signature),
    IrTypeAliasSymbol

class IrScriptSymbolImpl(descriptor: ScriptDescriptor? = null, signature: IdSignature? = null) :
    IrScriptSymbol, IrBindableSymbolBase<ScriptDescriptor, IrScript>(descriptor, signature)
