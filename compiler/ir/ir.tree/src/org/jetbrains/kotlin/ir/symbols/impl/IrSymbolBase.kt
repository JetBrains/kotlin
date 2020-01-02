/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.UniqId

abstract class IrSymbolBase<out D : DeclarationDescriptor>(override val descriptor: D, override var uniqId: UniqId = UniqId.NONE) : IrSymbol

abstract class IrBindableSymbolBase<out D : DeclarationDescriptor, B : IrSymbolOwner>(descriptor: D, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbol<D, B>, IrSymbolBase<D>(descriptor, uniqId) {

    init {
        assert(isOriginalDescriptor(descriptor)) {
            "Substituted descriptor $descriptor for ${descriptor.original}"
        }
        if (descriptor !is WrappedDeclarationDescriptor<*>) {
            val containingDeclaration = descriptor.containingDeclaration
            assert(containingDeclaration == null || isOriginalDescriptor(containingDeclaration)) {
                "Substituted containing declaration: $containingDeclaration\nfor descriptor: $descriptor"
            }
        }
    }

    private fun isOriginalDescriptor(descriptor: DeclarationDescriptor): Boolean =
        descriptor is WrappedDeclarationDescriptor<*> ||
                // TODO fix declaring/referencing value parameters: compute proper original descriptor
                descriptor is ValueParameterDescriptor && isOriginalDescriptor(descriptor.containingDeclaration) ||
                descriptor == descriptor.original

    private var _owner: B? = null
    override val owner: B
        get() = _owner ?: throw IllegalStateException("Symbol for $descriptor is unbound")

    override fun bind(owner: B) {
        if (_owner == null) {
            _owner = owner
        } else {
            throw IllegalStateException("${javaClass.simpleName} for $descriptor is already bound")
        }
    }

    override val isBound: Boolean
        get() = _owner != null
}

class IrFileSymbolImpl(descriptor: PackageFragmentDescriptor) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrFile>(descriptor),
    IrFileSymbol

class IrExternalPackageFragmentSymbolImpl(descriptor: PackageFragmentDescriptor) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrExternalPackageFragment>(descriptor),
    IrExternalPackageFragmentSymbol

class IrAnonymousInitializerSymbolImpl(descriptor: ClassDescriptor) :
    IrBindableSymbolBase<ClassDescriptor, IrAnonymousInitializer>(descriptor),
    IrAnonymousInitializerSymbol {
    constructor(irClassSymbol: IrClassSymbol) : this(irClassSymbol.descriptor) {}
}

class IrClassSymbolImpl(descriptor: ClassDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<ClassDescriptor, IrClass>(descriptor, uniqId),
    IrClassSymbol {
    constructor(uniqId: UniqId) : this(WrappedClassDescriptor(), uniqId)
}

class IrEnumEntrySymbolImpl(descriptor: ClassDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<ClassDescriptor, IrEnumEntry>(descriptor, uniqId),
    IrEnumEntrySymbol {
    constructor(uniqId: UniqId) : this(WrappedEnumEntryDescriptor(), uniqId)
}

class IrFieldSymbolImpl(descriptor: PropertyDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<PropertyDescriptor, IrField>(descriptor, uniqId),
    IrFieldSymbol {
    constructor(uniqId: UniqId) : this(WrappedFieldDescriptor(), uniqId)
}

class IrTypeParameterSymbolImpl(descriptor: TypeParameterDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<TypeParameterDescriptor, IrTypeParameter>(descriptor, uniqId),
    IrTypeParameterSymbol {
    constructor(uniqId: UniqId) : this(WrappedTypeParameterDescriptor(), uniqId)
}

class IrValueParameterSymbolImpl(descriptor: ParameterDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<ParameterDescriptor, IrValueParameter>(descriptor, uniqId),
    IrValueParameterSymbol {
    constructor(uniqId: UniqId) : this(WrappedValueParameterDescriptor(), uniqId)
}

class IrVariableSymbolImpl(descriptor: VariableDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<VariableDescriptor, IrVariable>(descriptor, uniqId),
    IrVariableSymbol {
    constructor(uniqId: UniqId) : this(WrappedVariableDescriptor(), uniqId)
}

class IrSimpleFunctionSymbolImpl(descriptor: FunctionDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<FunctionDescriptor, IrSimpleFunction>(descriptor, uniqId),
    IrSimpleFunctionSymbol {
    constructor(uniqId: UniqId) : this(WrappedSimpleFunctionDescriptor(), uniqId)
}

class IrConstructorSymbolImpl(descriptor: ClassConstructorDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<ClassConstructorDescriptor, IrConstructor>(descriptor, uniqId),
    IrConstructorSymbol {
    constructor(uniqId: UniqId) : this(WrappedClassConstructorDescriptor(), uniqId)
}

class IrReturnableBlockSymbolImpl(descriptor: FunctionDescriptor) :
    IrBindableSymbolBase<FunctionDescriptor, IrReturnableBlock>(descriptor),
    IrReturnableBlockSymbol

class IrPropertySymbolImpl(descriptor: PropertyDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<PropertyDescriptor, IrProperty>(descriptor, uniqId),
    IrPropertySymbol {
    constructor(uniqId: UniqId) : this(WrappedPropertyDescriptor(), uniqId)
}

class IrLocalDelegatedPropertySymbolImpl(descriptor: VariableDescriptorWithAccessors) :
    IrBindableSymbolBase<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>(descriptor),
    IrLocalDelegatedPropertySymbol

class IrTypeAliasSymbolImpl(descriptor: TypeAliasDescriptor, uniqId: UniqId = UniqId.NONE) :
    IrBindableSymbolBase<TypeAliasDescriptor, IrTypeAlias>(descriptor, uniqId),
    IrTypeAliasSymbol {
    constructor(uniqId: UniqId) : this(WrappedTypeAliasDescriptor(), uniqId)
}