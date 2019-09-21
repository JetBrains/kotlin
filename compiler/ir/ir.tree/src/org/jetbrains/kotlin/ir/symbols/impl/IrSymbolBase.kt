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
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrSymbolBase<out D : DeclarationDescriptor>(override val descriptor: D) : IrSymbol

abstract class IrBindableSymbolBase<out D : DeclarationDescriptor, B : IrSymbolOwner>(descriptor: D) :
    IrBindableSymbol<D, B>, IrSymbolBase<D>(descriptor), IrSymbolOwner {

    init {
        assert(isOriginalDescriptor(descriptor)) {
            "Substituted descriptor $descriptor for ${descriptor.original}"
        }
    }

    private fun isOriginalDescriptor(descriptor: DeclarationDescriptor): Boolean =
        descriptor is IrBasedDeclarationDescriptor ||
                descriptor is ValueParameterDescriptor && isOriginalDescriptor(descriptor.containingDeclaration) ||
                descriptor == descriptor.original

    private var _owner: B? = null
    override val owner: B
        get() = _owner ?:
        throw IllegalStateException("Symbol for $descriptor is unbound")

    override fun bind(owner: B) {
        if (_owner == null) {
            _owner = owner
        } else {
            throw IllegalStateException("${javaClass.simpleName} for $descriptor is already bound")
        }
    }

    override val isBound: Boolean
        get() = _owner != null

    override val symbol: IrSymbol get() = this
}

class IrFileSymbolImpl(descriptor: PackageFragmentDescriptor) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrFile>(descriptor),
    IrFileSymbol, IrFile {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val packageFragmentDescriptor get() = owner.packageFragmentDescriptor
    override val fqName get() = owner.fqName
    override val fileEntry get() = owner.fileEntry
    override val metadata get() = owner.metadata
    override val declarations get() = owner.declarations
    override val annotations get() = owner.annotations

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrExternalPackageFragmentSymbolImpl(descriptor: PackageFragmentDescriptor) :
    IrBindableSymbolBase<PackageFragmentDescriptor, IrExternalPackageFragment>(descriptor),
    IrExternalPackageFragmentSymbol, IrExternalPackageFragment {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val packageFragmentDescriptor get() = owner.packageFragmentDescriptor
    override val fqName get() = owner.fqName
    override val declarations get() = owner.declarations

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrAnonymousInitializerSymbolImpl(descriptor: ClassDescriptor) :
    IrBindableSymbolBase<ClassDescriptor, IrAnonymousInitializer>(descriptor),
    IrAnonymousInitializerSymbol, IrAnonymousInitializer {
    constructor(irClassSymbol: IrClassSymbol) : this(irClassSymbol.descriptor) {}
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val isStatic get() = owner.isStatic
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var body get() = owner.body; set(value) { owner.body = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrClassSymbolImpl(descriptor: ClassDescriptor) :
    IrBindableSymbolBase<ClassDescriptor, IrClass>(descriptor),
    IrClassSymbol, IrClass {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val typeParameters get() = owner.typeParameters
    override val declarations get() = owner.declarations
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val kind get() = owner.kind
    override val modality get() = owner.modality
    override val isCompanion get() = owner.isCompanion
    override val isInner get() = owner.isInner
    override val isData get() = owner.isData
    override val isExternal get() = owner.isExternal
    override val isInline get() = owner.isInline
    override val superTypes get() = owner.superTypes

    override var visibility get() = owner.visibility; set(value) { owner.visibility = value }
    override var thisReceiver get() = owner.thisReceiver; set(value) { owner.thisReceiver = value }
    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var attributeOwnerId get() = owner.attributeOwnerId; set(value) { owner.attributeOwnerId = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrEnumEntrySymbolImpl(descriptor: ClassDescriptor) :
    IrBindableSymbolBase<ClassDescriptor, IrEnumEntry>(descriptor),
    IrEnumEntrySymbol, IrEnumEntry {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var correspondingClass get() = owner.correspondingClass; set(value) { owner.correspondingClass = value }
    override var initializerExpression get() = owner.initializerExpression; set(value) { owner.initializerExpression = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrFieldSymbolImpl(descriptor: PropertyDescriptor) :
    IrBindableSymbolBase<PropertyDescriptor, IrField>(descriptor),
    IrFieldSymbol, IrField {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val type get() = owner.type
    override val isFinal get() = owner.isFinal
    override val isExternal get() = owner.isExternal
    override val isStatic get() = owner.isStatic
    override val overriddenSymbols get() = owner.overriddenSymbols
    override val visibility get() = owner.visibility

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var initializer get() = owner.initializer; set(value) { owner.initializer = value }
    override var correspondingPropertySymbol get() = owner.correspondingPropertySymbol; set(value) { owner.correspondingPropertySymbol = value }
    override var correspondingProperty get() = owner.correspondingProperty; set(value) { owner.correspondingProperty = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrTypeParameterSymbolImpl(descriptor: TypeParameterDescriptor) :
    IrBindableSymbolBase<TypeParameterDescriptor, IrTypeParameter>(descriptor),
    IrTypeParameterSymbol, IrTypeParameter {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val variance get() = owner.variance
    override val index get() = owner.index
    override val isReified get() = owner.isReified
    override val superTypes get() = owner.superTypes

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrValueParameterSymbolImpl(descriptor: ParameterDescriptor) :
    IrBindableSymbolBase<ParameterDescriptor, IrValueParameter>(descriptor),
    IrValueParameterSymbol, IrValueParameter {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val index get() = owner.index
    override val varargElementType get() = owner.varargElementType
    override val isCrossinline get() = owner.isCrossinline
    override val isNoinline get() = owner.isNoinline
    override val type get() = owner.type

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var defaultValue get() = owner.defaultValue; set(value) { owner.defaultValue = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrVariableSymbolImpl(descriptor: VariableDescriptor) :
    IrBindableSymbolBase<VariableDescriptor, IrVariable>(descriptor),
    IrVariableSymbol, IrVariable {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val type get() = owner.type
    override val isVar get() = owner.isVar
    override val isConst get() = owner.isConst
    override val isLateinit get() = owner.isLateinit

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var initializer get() = owner.initializer; set(value) { owner.initializer = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrSimpleFunctionSymbolImpl(descriptor: FunctionDescriptor) :
    IrBindableSymbolBase<FunctionDescriptor, IrSimpleFunction>(descriptor),
    IrSimpleFunctionSymbol, IrSimpleFunction {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val modality get() = owner.modality
    override val visibility get() = owner.visibility
    override val typeParameters get() = owner.typeParameters
    override val valueParameters get() = owner.valueParameters
    override val overriddenSymbols get() = owner.overriddenSymbols
    override val isTailrec get() = owner.isTailrec
    override val isSuspend get() = owner.isSuspend
    override val isInline get() = owner.isInline
    override val isExternal get() = owner.isExternal

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var body get() = owner.body; set(value) { owner.body = value }
    override var correspondingPropertySymbol get() = owner.correspondingPropertySymbol; set(value) { owner.correspondingPropertySymbol = value }
    override var correspondingProperty get() = owner.correspondingProperty; set(value) { owner.correspondingProperty = value }
    override var returnType get() = owner.returnType; set(value) { owner.returnType = value }
    override var dispatchReceiverParameter get() = owner.dispatchReceiverParameter; set(value) { owner.dispatchReceiverParameter = value }
    override var extensionReceiverParameter get() = owner.extensionReceiverParameter; set(value) { owner.extensionReceiverParameter = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrConstructorSymbolImpl(descriptor: ClassConstructorDescriptor) :
    IrBindableSymbolBase<ClassConstructorDescriptor, IrConstructor>(descriptor),
    IrConstructorSymbol, IrConstructor {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val visibility get() = owner.visibility
    override val typeParameters get() = owner.typeParameters
    override val valueParameters get() = owner.valueParameters
    override val isInline get() = owner.isInline
    override val isExternal get() = owner.isExternal
    override val isPrimary get() = owner.isPrimary

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var body get() = owner.body; set(value) { owner.body = value }
    override var returnType get() = owner.returnType; set(value) { owner.returnType = value }
    override var dispatchReceiverParameter get() = owner.dispatchReceiverParameter; set(value) { owner.dispatchReceiverParameter = value }
    override var extensionReceiverParameter get() = owner.extensionReceiverParameter; set(value) { owner.extensionReceiverParameter = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrReturnableBlockSymbolImpl(descriptor: FunctionDescriptor) :
    IrBindableSymbolBase<FunctionDescriptor, IrReturnableBlock>(descriptor),
    IrReturnableBlockSymbol, IrReturnableBlock {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val type get() = owner.type
    override val inlineFunctionSymbol get() = owner.inlineFunctionSymbol
    override val statements get() = owner.statements
    override val origin get() = owner.origin

    override var attributeOwnerId get() = owner.attributeOwnerId; set(value) { owner.attributeOwnerId = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrPropertySymbolImpl(descriptor: PropertyDescriptor) :
    IrBindableSymbolBase<PropertyDescriptor, IrProperty>(descriptor),
    IrPropertySymbol, IrProperty {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val modality get() = owner.modality
    override val visibility get() = owner.visibility
    override val isExternal get() = owner.isExternal
    override val isVar get() = owner.isVar
    override val isConst get() = owner.isConst
    override val isLateinit get() = owner.isLateinit
    override val isDelegated get() = owner.isDelegated

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var backingField get() = owner.backingField; set(value) { owner.backingField = value }
    override var getter get() = owner.getter; set(value) { owner.getter = value }
    override var setter get() = owner.setter; set(value) { owner.setter = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrLocalDelegatedPropertySymbolImpl(descriptor: VariableDescriptorWithAccessors) :
    IrBindableSymbolBase<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>(descriptor),
    IrLocalDelegatedPropertySymbol, IrLocalDelegatedProperty {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val type get() = owner.type
    override val isVar get() = owner.isVar

    override var delegate get() = owner.delegate; set(value) { owner.delegate = value }
    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }
    override var getter get() = owner.getter; set(value) { owner.getter = value }
    override var setter get() = owner.setter; set(value) { owner.setter = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}

class IrTypeAliasSymbolImpl(descriptor: TypeAliasDescriptor) :
    IrBindableSymbolBase<TypeAliasDescriptor, IrTypeAlias>(descriptor),
    IrTypeAliasSymbol, IrTypeAlias {
    override val symbol get() = this
    override val startOffset get() = owner.startOffset
    override val endOffset get() = owner.endOffset
    override val annotations get() = owner.annotations
    override val metadata get() = owner.metadata
    override val name get() = owner.name
    override val isActual get() = owner.isActual
    override val expandedType get() = owner.expandedType
    override val visibility get() = owner.visibility
    override val typeParameters get() = owner.typeParameters

    override var parent get() = owner.parent; set(value) { owner.parent = value }
    override var origin get() = owner.origin; set(value) { owner.origin = value }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = owner.accept(visitor, data)
    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) = owner.transform(transformer, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) = owner.acceptChildren(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) = owner.transformChildren(transformer, data)
}