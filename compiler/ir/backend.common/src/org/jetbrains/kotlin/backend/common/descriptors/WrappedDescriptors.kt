/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.TypeIntersectionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*


abstract class WrappedDeclarationDescriptor<T : IrDeclaration>(override val annotations: Annotations) : DeclarationDescriptor {
    lateinit var owner: T
    private var bound = false
    fun bind(declaration: T) {
        assert(!bound)
        bound = true
        owner = declaration
    }
}

abstract class WrappedCallableDescriptor<T : IrDeclaration>(
    annotations: Annotations,
    private val sourceElement: SourceElement
) : CallableDescriptor, WrappedDeclarationDescriptor<T>(annotations) {
    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {
        TODO("not implemented")
    }

    override fun getOverriddenDescriptors(): Collection<CallableDescriptor> {
        TODO("not implemented")
    }

    override fun getSource() = sourceElement

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        TODO("not implemented")
    }

    override fun getReturnType(): KotlinType? {
        TODO("not implemented")
    }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> {
        TODO("not implemented")
    }

    override fun hasStableParameterNames(): Boolean {
        TODO("not implemented")
    }

    override fun hasSynthesizedParameterNames() = false

    override fun getVisibility(): Visibility {
        TODO("not implemented")
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
        TODO("not implemented")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        TODO("not implemented")
    }
}

open class WrappedValueParameterDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    sourceElement: SourceElement = SourceElement.NO_SOURCE
) : ValueParameterDescriptor, WrappedCallableDescriptor<IrValueParameter>(annotations, sourceElement) {

    override val index get() = owner.index
    override val isCrossinline get() = owner.isCrossinline
    override val isNoinline get() = owner.isNoinline
    override val varargElementType get() = owner.varargElementType?.toKotlinType()
    override fun isConst() = false
    override fun isVar() = false

    override fun getContainingDeclaration() = (owner.parent as IrFunction).descriptor
    override fun getType() = owner.type.toKotlinType()
    override fun getName() = owner.name
    override fun declaresDefaultValue() = owner.defaultValue != null
    override fun getCompileTimeInitializer(): ConstantValue<*>? = null

    override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int) = object : WrappedValueParameterDescriptor() {
        override fun getContainingDeclaration() = newOwner as FunctionDescriptor
        override fun getName() = newName
        override val index = newIndex
    }.also { it.bind(owner) }


    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor> = emptyList()

    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): ValueParameterDescriptor {
        TODO("")
    }

    override fun getReturnType(): KotlinType? = owner.type.toKotlinType()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitValueParameterDescriptor(this, data)!!

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitValueParameterDescriptor(this, null)
    }
}

open class WrappedTypeParameterDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    sourceElement: SourceElement = SourceElement.NO_SOURCE
) : TypeParameterDescriptor, WrappedCallableDescriptor<IrTypeParameter>(annotations, sourceElement) {
    override fun getName() = owner.name

    override fun isReified() = owner.isReified

    override fun getVariance() = owner.variance

    override fun getUpperBounds() = owner.superTypes.map { it.toKotlinType() }

    private val _typeConstryuctor: TypeConstructor by lazy {
        object : AbstractTypeConstructor(LockBasedStorageManager.NO_LOCKS) {
            override fun computeSupertypes() = upperBounds

            override val supertypeLoopChecker = SupertypeLoopChecker.EMPTY

            override fun getParameters() = emptyList()

            override fun isFinal() = false

            override fun isDenotable() = true

            override fun getDeclarationDescriptor() = this@WrappedTypeParameterDescriptor

            override fun getBuiltIns() = module.builtIns
        }
    }

    override fun getTypeConstructor() = _typeConstryuctor

    override fun getOriginal() = this

    override fun getIndex() = owner.index

    override fun isCapturedFromOuterDeclaration() = false

    private val _defaultType: SimpleType by lazy {
        KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            Annotations.EMPTY, typeConstructor, emptyList(), false,
            TypeIntersectionScope.create(
                "Scope for type parameter " + name.asString(),
                upperBounds
            )
        )
    }

    override fun getDefaultType() = _defaultType

    override fun getContainingDeclaration() = (owner.parent as IrDeclaration).descriptor

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        visitor!!.visitTypeParameterDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitTypeParameterDescriptor(this, null)
    }

}

open class WrappedVariableDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    sourceElement: SourceElement = SourceElement.NO_SOURCE
) : VariableDescriptor, WrappedCallableDescriptor<IrVariable>(annotations, sourceElement) {

    override fun getContainingDeclaration() = (owner.parent as IrFunction).descriptor
    override fun getType() = owner.type.toKotlinType()
    override fun getName() = owner.name
    override fun isConst() = owner.isConst
    override fun isVar() = owner.isVar
    override fun isLateInit() = owner.isLateinit

    override fun getCompileTimeInitializer(): ConstantValue<*>? {
        TODO("")
    }

    override fun getOverriddenDescriptors(): Collection<VariableDescriptor> {
        TODO("Not Implemented")
    }

    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor {
        TODO("")
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        visitor!!.visitVariableDescriptor(this, data)
    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitVariableDescriptor(this, null)
    }
}

open class WrappedSimpleFunctionDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    sourceElement: SourceElement = SourceElement.NO_SOURCE
) : SimpleFunctionDescriptor, WrappedCallableDescriptor<IrSimpleFunction>(annotations, sourceElement) {
    override fun getOverriddenDescriptors() = owner.overriddenSymbols.map { it.descriptor }
    override fun getContainingDeclaration() = (owner.parent as IrSymbolOwner).symbol.descriptor
    override fun getModality() = owner.modality
    override fun getName() = owner.name
    override fun getVisibility() = owner.visibility
    override fun getReturnType() = owner.returnType.toKotlinType()

    override fun getDispatchReceiverParameter() = owner.dispatchReceiverParameter?.run {
        (containingDeclaration as ClassDescriptor).thisAsReceiverParameter
    }

    val extensionReceiver by lazy {
        owner.extensionReceiverParameter?.let {
            ReceiverParameterDescriptorImpl(this, ExtensionReceiver(it.descriptor, it.type.toKotlinType(), null), Annotations.EMPTY)
        }
    }

    override fun getExtensionReceiverParameter() = extensionReceiver
    override fun getTypeParameters() = owner.typeParameters.map { it.descriptor }
    override fun getValueParameters() = owner.valueParameters
        .asSequence()
        .mapNotNull { it.descriptor as? ValueParameterDescriptor }
        .toMutableList()
    override fun isExternal() = owner.isExternal
    override fun isSuspend() = owner.isSuspend
    override fun isTailrec() = owner.isTailrec
    override fun isInline() = owner.isInline

    override fun isExpect() = false
    override fun isActual() = false
    override fun isInfix() = false
    override fun isOperator() = false

    override fun getOriginal() = this
    override fun substitute(substitutor: TypeSubstitutor): SimpleFunctionDescriptor {
        TODO("")
    }

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented")
    }

    override fun getKind() =
        if (owner.origin == IrDeclarationOrigin.FAKE_OVERRIDE) CallableMemberDescriptor.Kind.FAKE_OVERRIDE
        else CallableMemberDescriptor.Kind.SYNTHESIZED

    override fun isHiddenToOvercomeSignatureClash(): Boolean {
        TODO("not implemented")
    }

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: Visibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): SimpleFunctionDescriptor {
        TODO("not implemented")
    }

    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean {
        TODO("not implemented")
    }

    override fun getInitialSignatureDescriptor() = null

    override fun <V : Any?> getUserData(key: FunctionDescriptor.UserDataKey<V>?): V? = null

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out SimpleFunctionDescriptor> {
        TODO("not implemented")
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitFunctionDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitFunctionDescriptor(this, null)
    }
}

open class WrappedClassConstructorDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    sourceElement: SourceElement = SourceElement.NO_SOURCE
) : ClassConstructorDescriptor, WrappedCallableDescriptor<IrConstructor>(annotations, sourceElement) {
    override fun getContainingDeclaration() = (owner.parent as IrClass).descriptor

    override fun getDispatchReceiverParameter() = owner.dispatchReceiverParameter?.run {
        (containingDeclaration.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
    }
    override fun getTypeParameters() = owner.typeParameters.map { it.descriptor }
    override fun getValueParameters() = owner.valueParameters.asSequence()
        .mapNotNull { it.descriptor as? ValueParameterDescriptor }
        .toMutableList()

    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor {
        TODO("not implemented")
    }

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: Visibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): ClassConstructorDescriptor {
        TODO("not implemented")
    }

    override fun getModality() = Modality.FINAL


    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented")
    }

    override fun getKind() = CallableMemberDescriptor.Kind.SYNTHESIZED

    override fun getConstructedClass() = (owner.parent as IrClass).descriptor

    override fun getName() = owner.name

    override fun getOverriddenDescriptors(): MutableCollection<out FunctionDescriptor> = mutableListOf()

    override fun getInitialSignatureDescriptor(): FunctionDescriptor? = null

    override fun getVisibility() = owner.visibility

    override fun isHiddenToOvercomeSignatureClash(): Boolean {
        TODO("not implemented")
    }

    override fun isOperator() = false

    override fun isInline() = owner.isInline

    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean {
        TODO("not implemented")
    }

    override fun getReturnType() = owner.returnType.toKotlinType()

    override fun isPrimary() = owner.isPrimary

    override fun isExpect() = false

    override fun isTailrec() = false

    override fun isActual() = false

    override fun isInfix() = false

    override fun isSuspend() = false

    override fun <V : Any?> getUserData(key: FunctionDescriptor.UserDataKey<V>?): V? = null

    override fun isExternal() = owner.isExternal

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> {
        TODO("not implemented")
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        visitor!!.visitConstructorDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitConstructorDescriptor(this, null)
    }
}

open class WrappedClassDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    private val sourceElement: SourceElement = SourceElement.NO_SOURCE
) : ClassDescriptor, WrappedDeclarationDescriptor<IrClass>(annotations) {
    override fun getName() = owner.name

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>)= MemberScope.Empty

    override fun getMemberScope(typeSubstitution: TypeSubstitution)= MemberScope.Empty

    override fun getUnsubstitutedMemberScope() = MemberScope.Empty

    override fun getUnsubstitutedInnerClassesScope() = MemberScope.Empty

    override fun getStaticScope() = MemberScope.Empty

    override fun getSource() = sourceElement

    override fun getConstructors() = owner.declarations.asSequence().filterIsInstance<IrConstructor>().map { it.descriptor }.toList()

    override fun getContainingDeclaration() = (owner.parent as IrSymbolOwner).symbol.descriptor


    private val _defaultType: SimpleType by lazy {
        TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope)
    }

    override fun getDefaultType(): SimpleType = _defaultType

    override fun getKind() = owner.kind

    override fun getModality() = owner.modality

    override fun getCompanionObjectDescriptor() = owner.declarations.filterIsInstance<IrClass>().firstOrNull { it.isCompanion }?.descriptor

    override fun getVisibility() = owner.visibility

    override fun isCompanionObject() = owner.isCompanion

    override fun isData() = owner.isData

    override fun isInline() = owner.isInline

    override fun getThisAsReceiverParameter() = owner.thisReceiver?.descriptor as ReceiverParameterDescriptor

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        TODO("not implemented")
    }

    override fun getDeclaredTypeParameters() = owner.typeParameters.map { it.descriptor }

    override fun getSealedSubclasses(): Collection<ClassDescriptor> {
        TODO("not implemented")
    }

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters {
        TODO("not implemented")
    }

    override fun isActual() = false

    private val _typeConstructor: TypeConstructor by lazy {
        ClassTypeConstructorImpl(this, emptyList(), owner.superTypes.map { it.toKotlinType() }, LockBasedStorageManager.NO_LOCKS)
    }

    override fun getTypeConstructor(): TypeConstructor = _typeConstructor

    override fun isInner() = owner.isInner

    override fun isExternal() = owner.isExternal

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        visitor!!.visitClassDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitClassDescriptor(this, null)
    }
}


open class WrappedPropertyDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    private val sourceElement: SourceElement = SourceElement.NO_SOURCE
) : PropertyDescriptor, WrappedDeclarationDescriptor<IrField>(annotations) {
    override fun getModality() = if (owner.isFinal) Modality.FINAL else Modality.OPEN

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented")
    }

    override fun getKind() = CallableMemberDescriptor.Kind.SYNTHESIZED

    override fun getName() = owner.name

    override fun getSource() = sourceElement

    override fun hasSynthesizedParameterNames(): Boolean {
        TODO("not implemented")
    }

    override fun getOverriddenDescriptors(): MutableCollection<out PropertyDescriptor> = mutableListOf()

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: Visibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): CallableMemberDescriptor {
        TODO("not implemented")
    }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> = mutableListOf()

    override fun getCompileTimeInitializer(): ConstantValue<*>? {
        TODO("not implemented")
    }

    override fun isSetterProjectedOut(): Boolean {
        TODO("not implemented")
    }

    override fun getAccessors(): MutableList<PropertyAccessorDescriptor> = mutableListOf()

    override fun getTypeParameters() = emptyList()

    override fun getVisibility() = owner.visibility

    override val setter: PropertySetterDescriptor? get() = null

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor {
        TODO("not implemented")
    }

    override fun isActual() = false

    override fun getReturnType() = owner.type.toKotlinType()

    override fun hasStableParameterNames(): Boolean {
        TODO("not implemented")
    }

    override fun getType(): KotlinType = owner.type.toKotlinType()

    override fun isVar() = owner.isFinal

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        TODO("not implemented")
    }

    override fun isConst() = false

    override fun getContainingDeclaration() = (owner.parent as IrSymbolOwner).symbol.descriptor

    override fun isLateInit() = false

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        TODO("not implemented")
    }

    override fun isExternal() = owner.isExternal

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitPropertyDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitPropertyDescriptor(this, null)
    }

    override val getter: PropertyGetterDescriptor? get() = null

    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out PropertyDescriptor> {
        TODO("not implemented")
    }

    override val isDelegated get() = false
}