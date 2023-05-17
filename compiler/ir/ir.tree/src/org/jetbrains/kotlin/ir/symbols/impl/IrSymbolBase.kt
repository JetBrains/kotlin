/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

abstract class IrSymbolBase<out Descriptor : DeclarationDescriptor>(
    final override val signature: IdSignature?,
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

        return if (signature == null) {
            "Unbound private symbol " +
                    if (_descriptor != null) "${this::class.java.simpleName}: $_descriptor" else super.toString()
        } else {
            "Unbound public symbol ${this::class.java.simpleName}: $signature"
        }
    }
}

abstract class IrBindableSymbolBase<out Descriptor, Owner>(
    signature: IdSignature?,
    descriptor: Descriptor?,
) : IrSymbolBase<Descriptor>(signature, descriptor), IrBindableSymbol<Descriptor, Owner>
        where Descriptor : DeclarationDescriptor,
              Owner : IrSymbolOwner {

    init {
        assert(descriptor == null || isOriginalDescriptor(descriptor)) {
            "Substituted descriptor $descriptor for ${descriptor!!.original}"
        }
//        assert(sig.isPubliclyVisible)
        if (descriptor != null && signature == null) {
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
        get() = _owner ?: throw IllegalStateException("Symbol for ${signature ?: javaClass.simpleName} is unbound")

    override fun bind(owner: Owner) {
        _owner?.let { existingOwner ->
            throw IllegalStateException(
                buildString {
                    append(javaClass.simpleName)
                    signature?.let {
                        append(" for ")
                        append(it)
                    }
                    append(" is already bound: ")
                    append(existingOwner.render())
                },
            )
        }
        _owner = owner
    }

    override val isBound: Boolean
        get() = _owner != null

    override var privateSignature: IdSignature? = null
}

class IrFileSymbolImpl(descriptor: PackageFragmentDescriptor? = null) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrFile>(null, descriptor),
    IrFileSymbol

class IrExternalPackageFragmentSymbolImpl(descriptor: PackageFragmentDescriptor? = null) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrExternalPackageFragment>(null, descriptor),
    IrExternalPackageFragmentSymbol

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrAnonymousInitializerSymbolImpl(descriptor: ClassDescriptor? = null) :
    IrBindableSymbolBase<ClassDescriptor, IrAnonymousInitializer>(null, descriptor),
    IrAnonymousInitializerSymbol {
    constructor(irClassSymbol: IrClassSymbol) : this(irClassSymbol.descriptor)
}

class IrClassSymbolImpl(sig: IdSignature?, descriptor: ClassDescriptor? = null) :
    IrBindableSymbolBase<ClassDescriptor, IrClass>(sig, descriptor),
    IrClassSymbol {
    constructor(descriptor: ClassDescriptor? = null) : this(null, descriptor)
}

class IrEnumEntrySymbolImpl(sig: IdSignature?, descriptor: ClassDescriptor? = null) :
    IrBindableSymbolBase<ClassDescriptor, IrEnumEntry>(sig, descriptor),
    IrEnumEntrySymbol {
    constructor(descriptor: ClassDescriptor? = null) : this(null, descriptor)
}

class IrFieldSymbolImpl(sig: IdSignature?, descriptor: PropertyDescriptor? = null) :
    IrBindableSymbolBase<PropertyDescriptor, IrField>(sig, descriptor),
    IrFieldSymbol {
    constructor(descriptor: PropertyDescriptor? = null) : this(null, descriptor)
}

class IrTypeParameterSymbolImpl(sig: IdSignature?, descriptor: TypeParameterDescriptor? = null) :
    IrBindableSymbolBase<TypeParameterDescriptor, IrTypeParameter>(sig, descriptor),
    IrTypeParameterSymbol {
    constructor(descriptor: TypeParameterDescriptor? = null) : this(null, descriptor)
}

class IrValueParameterSymbolImpl(descriptor: ParameterDescriptor? = null) :
    IrBindableSymbolBase<ParameterDescriptor, IrValueParameter>(null, descriptor),
    IrValueParameterSymbol

class IrVariableSymbolImpl(descriptor: VariableDescriptor? = null) :
    IrBindableSymbolBase<VariableDescriptor, IrVariable>(null, descriptor),
    IrVariableSymbol

class IrSimpleFunctionSymbolImpl(sig: IdSignature?, descriptor: FunctionDescriptor? = null) :
    IrBindableSymbolBase<FunctionDescriptor, IrSimpleFunction>(sig, descriptor),
    IrSimpleFunctionSymbol {
    constructor(descriptor: FunctionDescriptor? = null) : this(null, descriptor)
}

class IrConstructorSymbolImpl(sig: IdSignature?, descriptor: ClassConstructorDescriptor? = null) :
    IrBindableSymbolBase<ClassConstructorDescriptor, IrConstructor>(sig, descriptor),
    IrConstructorSymbol {
    constructor(descriptor: ClassConstructorDescriptor? = null) : this(null, descriptor)
}

class IrReturnableBlockSymbolImpl(descriptor: FunctionDescriptor? = null) :
    IrBindableSymbolBase<FunctionDescriptor, IrReturnableBlock>(null, descriptor),
    IrReturnableBlockSymbol

class IrPropertySymbolImpl(sig: IdSignature?, descriptor: PropertyDescriptor? = null) :
    IrBindableSymbolBase<PropertyDescriptor, IrProperty>(sig, descriptor),
    IrPropertySymbol {
    constructor(descriptor: PropertyDescriptor? = null) : this(null, descriptor)
}

class IrLocalDelegatedPropertySymbolImpl(descriptor: VariableDescriptorWithAccessors? = null) :
    IrBindableSymbolBase<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>(null, descriptor),
    IrLocalDelegatedPropertySymbol

class IrTypeAliasSymbolImpl(sig: IdSignature?, descriptor: TypeAliasDescriptor? = null) :
    IrBindableSymbolBase<TypeAliasDescriptor, IrTypeAlias>(sig, descriptor),
    IrTypeAliasSymbol {
    constructor(descriptor: TypeAliasDescriptor? = null) : this(null, descriptor)
}

class IrScriptSymbolImpl(descriptor: ScriptDescriptor? = null) :
    IrBindableSymbolBase<ScriptDescriptor, IrScript>(null, descriptor),
    IrScriptSymbol
