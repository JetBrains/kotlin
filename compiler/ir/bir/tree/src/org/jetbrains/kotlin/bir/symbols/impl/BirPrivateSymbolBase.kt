/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols.impl

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirReturnableBlock
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.toBirBasedDescriptor
import org.jetbrains.kotlin.bir.util.render
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.IdSignature

/**
 * The base class for all non-public (wrt linkage) symbols.
 *
 * Its [signature] is always `null`.
 *
 * TODO: Merge with [BirPublicSymbolBase] ([KT-44721](https://youtrack.jetbrains.com/issue/KT-44721))
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class BirSymbolBase<out Descriptor : DeclarationDescriptor>(
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
        return "Unbound private symbol " +
                if (_descriptor != null) "${this::class.java.simpleName}: $_descriptor" else super.toString()
    }
}

abstract class BirBindableSymbolBase<out Descriptor, Owner>(
    descriptor: Descriptor?,
) : BirSymbolBase<Descriptor>(descriptor), BirBindableSymbol<Descriptor, Owner>
        where Descriptor : DeclarationDescriptor,
              Owner : BirSymbolOwner {

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

    override val signature: IdSignature?
        get() = null

    override val isBound: Boolean
        get() = _owner != null

    override var privateSignature: IdSignature? = null
}

class BirFileSymbolImpl(descriptor: PackageFragmentDescriptor? = null) :
    BirBindableSymbolBase<PackageFragmentDescriptor, BirFile>(descriptor),
    BirFileSymbol

class BirExternalPackageFragmentSymbolImpl(descriptor: PackageFragmentDescriptor? = null) :
    BirBindableSymbolBase<PackageFragmentDescriptor, BirExternalPackageFragment>(descriptor),
    BirExternalPackageFragmentSymbol

@OptIn(ObsoleteDescriptorBasedAPI::class)
class BirAnonymousInitializerSymbolImpl(descriptor: ClassDescriptor? = null) :
    BirBindableSymbolBase<ClassDescriptor, BirAnonymousInitializer>(descriptor),
    BirAnonymousInitializerSymbol {
    constructor(irClassSymbol: BirClassSymbol) : this(irClassSymbol.descriptor)
}

class BirClassSymbolImpl(descriptor: ClassDescriptor? = null) :
    BirBindableSymbolBase<ClassDescriptor, BirClass>(descriptor),
    BirClassSymbol

class BirEnumEntrySymbolImpl(descriptor: ClassDescriptor? = null) :
    BirBindableSymbolBase<ClassDescriptor, BirEnumEntry>(descriptor),
    BirEnumEntrySymbol

class BirFieldSymbolImpl(descriptor: PropertyDescriptor? = null) :
    BirBindableSymbolBase<PropertyDescriptor, BirField>(descriptor),
    BirFieldSymbol

class BirTypeParameterSymbolImpl(descriptor: TypeParameterDescriptor? = null) :
    BirBindableSymbolBase<TypeParameterDescriptor, BirTypeParameter>(descriptor),
    BirTypeParameterSymbol

class BirValueParameterSymbolImpl(descriptor: ParameterDescriptor? = null) :
    BirBindableSymbolBase<ParameterDescriptor, BirValueParameter>(descriptor),
    BirValueParameterSymbol

class BirVariableSymbolImpl(descriptor: VariableDescriptor? = null) :
    BirBindableSymbolBase<VariableDescriptor, BirVariable>(descriptor),
    BirVariableSymbol

class BirSimpleFunctionSymbolImpl(descriptor: FunctionDescriptor? = null) :
    BirBindableSymbolBase<FunctionDescriptor, BirSimpleFunction>(descriptor),
    BirSimpleFunctionSymbol

class BirConstructorSymbolImpl(descriptor: ClassConstructorDescriptor? = null) :
    BirBindableSymbolBase<ClassConstructorDescriptor, BirConstructor>(descriptor),
    BirConstructorSymbol

class BirReturnableBlockSymbolImpl(descriptor: FunctionDescriptor? = null) :
    BirBindableSymbolBase<FunctionDescriptor, BirReturnableBlock>(descriptor),
    BirReturnableBlockSymbol

class BirPropertySymbolImpl(descriptor: PropertyDescriptor? = null) :
    BirBindableSymbolBase<PropertyDescriptor, BirProperty>(descriptor),
    BirPropertySymbol

class BirLocalDelegatedPropertySymbolImpl(descriptor: VariableDescriptorWithAccessors? = null) :
    BirBindableSymbolBase<VariableDescriptorWithAccessors, BirLocalDelegatedProperty>(descriptor),
    BirLocalDelegatedPropertySymbol

class BirTypeAliasSymbolImpl(descriptor: TypeAliasDescriptor? = null) :
    BirBindableSymbolBase<TypeAliasDescriptor, BirTypeAlias>(descriptor),
    BirTypeAliasSymbol

class BirScriptSymbolImpl(descriptor: ScriptDescriptor? = null) :
    BirScriptSymbol, BirBindableSymbolBase<ScriptDescriptor, BirScript>(descriptor)
