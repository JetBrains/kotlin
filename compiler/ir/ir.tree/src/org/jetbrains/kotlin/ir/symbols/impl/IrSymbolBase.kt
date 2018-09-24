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
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*

abstract class IrSymbolBase<out D : DeclarationDescriptor>(override val descriptor: D) : IrSymbol

abstract class IrBindableSymbolBase<out D : DeclarationDescriptor, B : IrSymbolOwner>(descriptor: D) :
    IrBindableSymbol<D, B>, IrSymbolBase<D>(descriptor) {
    init {
//        assert(isOriginalDescriptor(descriptor)) {
//            "Substituted descriptor $descriptor for ${descriptor.original}"
//        }
    }

    private fun isOriginalDescriptor(descriptor: DeclarationDescriptor): Boolean =
        if (descriptor is ValueParameterDescriptor)
            isOriginalDescriptor(descriptor.containingDeclaration)
        else
            descriptor == descriptor.original

    private var _owner: B? = null
    override val owner: B
        get() = _owner ?: throw IllegalStateException("Symbol for $descriptor is unbound")

    override fun bind(owner: B) {
        if (_owner == null)
            _owner = owner
        else
            throw IllegalStateException("${javaClass.simpleName} for $descriptor is already bound")
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
    IrAnonymousInitializerSymbol

class IrClassSymbolImpl(descriptor: ClassDescriptor) :
    IrBindableSymbolBase<ClassDescriptor, IrClass>(descriptor),
    IrClassSymbol

fun createClassSymbolOrNull(descriptor: ClassDescriptor?) =
    descriptor?.let { IrClassSymbolImpl(it) }

class IrEnumEntrySymbolImpl(descriptor: ClassDescriptor) :
    IrBindableSymbolBase<ClassDescriptor, IrEnumEntry>(descriptor),
    IrEnumEntrySymbol

class IrFieldSymbolImpl(descriptor: PropertyDescriptor) :
    IrBindableSymbolBase<PropertyDescriptor, IrField>(descriptor),
    IrFieldSymbol

class IrTypeParameterSymbolImpl(descriptor: TypeParameterDescriptor) :
    IrBindableSymbolBase<TypeParameterDescriptor, IrTypeParameter>(descriptor),
    IrTypeParameterSymbol

class IrValueParameterSymbolImpl(descriptor: ParameterDescriptor) :
    IrBindableSymbolBase<ParameterDescriptor, IrValueParameter>(descriptor),
    IrValueParameterSymbol

class IrVariableSymbolImpl(descriptor: VariableDescriptor) :
    IrBindableSymbolBase<VariableDescriptor, IrVariable>(descriptor),
    IrVariableSymbol

fun createValueSymbol(descriptor: ValueDescriptor): IrValueSymbol =
    when (descriptor) {
        is ParameterDescriptor -> IrValueParameterSymbolImpl(descriptor)
        is VariableDescriptor -> IrVariableSymbolImpl(descriptor)
        else -> throw IllegalArgumentException("Unexpected descriptor kind: $descriptor")
    }

class IrSimpleFunctionSymbolImpl(descriptor: FunctionDescriptor) :
    IrBindableSymbolBase<FunctionDescriptor, IrSimpleFunction>(descriptor),
    IrSimpleFunctionSymbol

class IrConstructorSymbolImpl(descriptor: ClassConstructorDescriptor) :
    IrBindableSymbolBase<ClassConstructorDescriptor, IrConstructor>(descriptor),
    IrConstructorSymbol

fun createFunctionSymbol(descriptor: CallableMemberDescriptor): IrFunctionSymbol =
    when (descriptor) {
        is ClassConstructorDescriptor -> IrConstructorSymbolImpl(descriptor.original)
        is FunctionDescriptor -> IrSimpleFunctionSymbolImpl(descriptor.original)
        else -> throw IllegalArgumentException("Unexpected descriptor kind: $descriptor")
    }

class IrReturnableBlockSymbolImpl(descriptor: FunctionDescriptor) :
    IrBindableSymbolBase<FunctionDescriptor, IrReturnableBlock>(descriptor),
    IrReturnableBlockSymbol