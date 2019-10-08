/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.TypeIntersectionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*


abstract class WrappedDeclarationDescriptor<T : IrDeclaration>(annotations: Annotations) :
    DeclarationDescriptor, IrBasedDeclarationDescriptor {

    private val annotations_ = annotations

    override val annotations: Annotations
        get() = if (annotations_ != Annotations.EMPTY) annotations_ else annotationsFromOwner

    private val annotationsFromOwner by lazy {
        val ownerAnnotations = (owner as? IrAnnotationContainer)?.annotations ?: return@lazy Annotations.EMPTY
        Annotations.create(ownerAnnotations.map { it.toAnnotationDescriptor() })
    }

    private fun IrConstructorCall.toAnnotationDescriptor(): AnnotationDescriptor {
        assert(symbol.owner.parentAsClass.isAnnotationClass) {
            "Expected call to constructor of annotation class but was: ${this.dump()}"
        }
        return AnnotationDescriptorImpl(
            symbol.owner.parentAsClass.defaultType.toKotlinType(),
            symbol.owner.valueParameters.map { it.name to getValueArgument(it.index) }
                .filter { it.second != null }
                .associate { it.first to it.second!!.toConstantValue() },
            /*TODO*/ SourceElement.NO_SOURCE
        )
    }

    private fun IrElement.toConstantValue(): ConstantValue<*> {
        return when {
            this is IrConst<*> -> when (kind) {
                IrConstKind.Null -> NullValue()
                IrConstKind.Boolean -> BooleanValue(value as Boolean)
                IrConstKind.Char -> CharValue(value as Char)
                IrConstKind.Byte -> ByteValue(value as Byte)
                IrConstKind.Short -> ShortValue(value as Short)
                IrConstKind.Int -> IntValue(value as Int)
                IrConstKind.Long -> LongValue(value as Long)
                IrConstKind.String -> StringValue(value as String)
                IrConstKind.Float -> FloatValue(value as Float)
                IrConstKind.Double -> DoubleValue(value as Double)
            }

            this is IrVararg -> {
                val elements = elements.map { if (it is IrSpreadElement) error("$it is not expected") else it.toConstantValue() }
                ArrayValue(elements) { moduleDescriptor ->
                    // TODO: substitute.
                    moduleDescriptor.builtIns.array.defaultType
                }
            }

            this is IrGetEnumValue -> EnumValue(symbol.owner.parentAsClass.descriptor.classId!!, symbol.owner.name)

            this is IrClassReference -> KClassValue(classType.classifierOrFail.descriptor.classId!!, /*TODO*/0)

            this is IrConstructorCall -> AnnotationValue(this.toAnnotationDescriptor())

            else -> error("$this is not expected: ${this.dump()}")
        }
    }

    private var _owner: T? = null

    var owner: T
        get() {
            return _owner ?: error("$this is not bound")
        }
        private set(value) {
            _owner?.let { error("$this is already bound to ${it.dump()}") }
            _owner = value
        }

    fun bind(declaration: T) {
        owner = declaration
    }
}

abstract class WrappedCallableDescriptor<T : IrDeclaration>(
    annotations: Annotations,
    private val sourceElement: SourceElement
) : CallableDescriptor, WrappedDeclarationDescriptor<T>(annotations) {
    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor =
        throw UnsupportedOperationException("Wrapped descriptors SHOULD NOT be substituted")

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

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}

// TODO: (Roman Artemev) do not create this kind of descriptor for dispatch receiver parameters
// WrappedReceiverParameterDescriptor should be used instead
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

    override fun substitute(substitutor: TypeSubstitutor): ValueParameterDescriptor =
        throw UnsupportedOperationException("Wrapped descriptors SHOULD NOT be substituted")

    override fun getReturnType(): KotlinType? = owner.type.toKotlinType()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitValueParameterDescriptor(this, data)!!

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitValueParameterDescriptor(this, null)
    }
}

open class WrappedReceiverParameterDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    sourceElement: SourceElement = SourceElement.NO_SOURCE
) : ReceiverParameterDescriptor, WrappedCallableDescriptor<IrValueParameter>(annotations, sourceElement) {

    override fun getValue(): ReceiverValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContainingDeclaration(): DeclarationDescriptor =
        (owner.parent as? IrFunction)?.descriptor ?: (owner.parent as IrClass).descriptor

    override fun getType() = owner.type.toKotlinType()
    override fun getName() = owner.name

    override fun copy(newOwner: DeclarationDescriptor) = object : WrappedReceiverParameterDescriptor() {
        override fun getContainingDeclaration() = newOwner
    }.also { it.bind(owner) }

    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor> = emptyList()

    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): ReceiverParameterDescriptor {
        TODO("")
    }

    override fun getReturnType(): KotlinType? = owner.type.toKotlinType()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitReceiverParameterDescriptor(this, data)!!

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitReceiverParameterDescriptor(this, null)
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

            override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

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
            LazyScopeAdapter(LockBasedStorageManager.NO_LOCKS.createLazyValue {
                TypeIntersectionScope.create(
                    "Scope for type parameter " + name.asString(),
                    upperBounds
                )
            })
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
    override fun getReturnType() = getType()
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

open class WrappedVariableDescriptorWithAccessor() : VariableDescriptorWithAccessors,
    WrappedCallableDescriptor<IrLocalDelegatedProperty>(Annotations.EMPTY, SourceElement.NO_SOURCE) {
    override fun getName(): Name = owner.name

    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isVar() = owner.isVar

    override fun getCompileTimeInitializer(): ConstantValue<*>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(): KotlinType = owner.type.toKotlinType()

    override fun isConst(): Boolean = false

    override fun getContainingDeclaration() = (owner.parent as IrFunction).descriptor

    override fun isLateInit(): Boolean = false

    override val getter: VariableAccessorDescriptor?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val setter: VariableAccessorDescriptor?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val isDelegated: Boolean = true

}

open class WrappedSimpleFunctionDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    sourceElement: SourceElement = SourceElement.NO_SOURCE
) : SimpleFunctionDescriptor, WrappedCallableDescriptor<IrSimpleFunction>(annotations, sourceElement) {

    // TODO: Remove as soon as all IR declarations have their originalDescriptor.
    constructor(originalDescriptor: FunctionDescriptor) : this(originalDescriptor.annotations, originalDescriptor.source) {
        this.originalDescriptor = originalDescriptor
    }

    var originalDescriptor: FunctionDescriptor? = null

    override fun getOverriddenDescriptors() = owner.overriddenSymbols.map { it.descriptor }

    override fun getContainingDeclaration(): DeclarationDescriptor = getContainingDeclaration(owner)

    override fun getModality() = owner.modality
    override fun getName() = owner.name
    override fun getVisibility() = owner.visibility
    override fun getReturnType() = owner.returnType.toKotlinType()

    override fun getDispatchReceiverParameter() = owner.dispatchReceiverParameter?.run {
        (containingDeclaration as ClassDescriptor).thisAsReceiverParameter
    }

    override fun getExtensionReceiverParameter() = owner.extensionReceiverParameter?.descriptor as? ReceiverParameterDescriptor
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
    ): Nothing {
        TODO("not implemented")
    }

    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean {
        TODO("not implemented")
    }

    override fun getInitialSignatureDescriptor() = null

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out SimpleFunctionDescriptor> {
        TODO("not implemented")
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitFunctionDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitFunctionDescriptor(this, null)
    }
}

class WrappedFunctionDescriptorWithContainerSource(
    override val containerSource: DeserializedContainerSource?
) : WrappedSimpleFunctionDescriptor(), DescriptorWithContainerSource

open class WrappedClassConstructorDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    sourceElement: SourceElement = SourceElement.NO_SOURCE
) : ClassConstructorDescriptor, WrappedCallableDescriptor<IrConstructor>(annotations, sourceElement) {
    override fun getContainingDeclaration() = (owner.parent as IrClass).descriptor

    override fun getDispatchReceiverParameter() = owner.dispatchReceiverParameter?.run {
        (containingDeclaration.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
    }

    override fun getTypeParameters() =
        (owner.constructedClass.typeParameters + owner.typeParameters).map { it.descriptor }

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
        throw UnsupportedOperationException()
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

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null

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

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>) = MemberScope.Empty

    override fun getMemberScope(typeSubstitution: TypeSubstitution) = MemberScope.Empty

    override fun getUnsubstitutedMemberScope() = MemberScope.Empty

    override fun getUnsubstitutedInnerClassesScope() = MemberScope.Empty

    override fun getStaticScope() = MemberScope.Empty

    override fun getSource() = sourceElement

    override fun getConstructors() = owner.declarations.asSequence().filterIsInstance<IrConstructor>().map { it.descriptor }.toList()

    override fun getContainingDeclaration() = (owner.parent as IrSymbolOwner).symbol.descriptor


    private val _defaultType: SimpleType by lazy {
        TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope, KotlinTypeFactory.EMPTY_REFINED_TYPE_FACTORY)
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

    override fun getUnsubstitutedPrimaryConstructor() =
        owner.declarations.filterIsInstance<IrConstructor>().singleOrNull { it.isPrimary }?.descriptor

    override fun getDeclaredTypeParameters() = owner.typeParameters.map { it.descriptor }

    override fun getSealedSubclasses(): Collection<ClassDescriptor> {
        TODO("not implemented")
    }

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters =
        throw UnsupportedOperationException("Wrapped descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    private val _typeConstructor: TypeConstructor by lazy {
        LazyTypeConstructor(
            this,
            { declaredTypeParameters },
            { owner.superTypes.map { it.toKotlinType() } },
            LockBasedStorageManager.NO_LOCKS
        )
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

class LazyTypeConstructor(
    val classDescriptor: ClassDescriptor,
    val parametersBuilder: () -> List<TypeParameterDescriptor>,
    val superTypesBuilder: () -> List<KotlinType>,
    storageManager: StorageManager
) : AbstractClassTypeConstructor(storageManager) {
    val parameters_ by lazy { parametersBuilder() }
    val superTypes_ by lazy { superTypesBuilder() }

    override fun getParameters() = parameters_

    override fun computeSupertypes() = superTypes_

    override fun isDenotable() = true

    override fun getDeclarationDescriptor() = classDescriptor

    override val supertypeLoopChecker: SupertypeLoopChecker
        get() = SupertypeLoopChecker.EMPTY
}

open class WrappedEnumEntryDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    private val sourceElement: SourceElement = SourceElement.NO_SOURCE
) : ClassDescriptor, WrappedDeclarationDescriptor<IrEnumEntry>(annotations) {
    override fun getName() = owner.name

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>) = MemberScope.Empty

    override fun getMemberScope(typeSubstitution: TypeSubstitution) = MemberScope.Empty

    override fun getUnsubstitutedMemberScope() = MemberScope.Empty

    override fun getUnsubstitutedInnerClassesScope() = MemberScope.Empty

    override fun getStaticScope() = MemberScope.Empty

    override fun getSource() = sourceElement

    override fun getConstructors() =
        getCorrespondingClass().declarations.asSequence().filterIsInstance<IrConstructor>().map { it.descriptor }.toList()

    private fun getCorrespondingClass() = owner.correspondingClass ?: (owner.parent as IrClass)

    override fun getContainingDeclaration() = (owner.parent as IrSymbolOwner).symbol.descriptor


    private val _defaultType: SimpleType by lazy {
        TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope, KotlinTypeFactory.EMPTY_REFINED_TYPE_FACTORY)
    }

    override fun getDefaultType(): SimpleType = _defaultType

    override fun getKind() = ClassKind.ENUM_ENTRY

    override fun getModality() = Modality.FINAL

    override fun getCompanionObjectDescriptor() = null

    override fun getVisibility() = Visibilities.DEFAULT_VISIBILITY

    override fun isCompanionObject() = false

    override fun isData() = false

    override fun isInline() = false

    override fun getThisAsReceiverParameter() = (owner.parent as IrClass).descriptor.thisAsReceiverParameter

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        TODO("not implemented")
    }

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSealedSubclasses(): Collection<ClassDescriptor> {
        TODO("not implemented")
    }

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters =
        throw UnsupportedOperationException("Wrapped descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    private val _typeConstructor: TypeConstructor by lazy {
        ClassTypeConstructorImpl(
            this,
            emptyList(),
            getCorrespondingClass().superTypes.map { it.toKotlinType() },
            LockBasedStorageManager.NO_LOCKS
        )
    }

    override fun getTypeConstructor(): TypeConstructor = _typeConstructor

    override fun isInner() = false

    override fun isExternal() = false

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        visitor!!.visitClassDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitClassDescriptor(this, null)
    }
}


open class WrappedPropertyDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    private val sourceElement: SourceElement = SourceElement.NO_SOURCE
) : PropertyDescriptor, WrappedDeclarationDescriptor<IrProperty>(annotations) {
    override fun getModality() = owner.modality

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
        return null
    }

    override fun isSetterProjectedOut(): Boolean {
        TODO("not implemented")
    }

    override fun getAccessors(): List<PropertyAccessorDescriptor> = listOfNotNull(getter, setter)

    override fun getTypeParameters(): List<TypeParameterDescriptor> = getter?.typeParameters.orEmpty()

    override fun getVisibility() = owner.visibility

    override val setter: PropertySetterDescriptor? get() = owner.setter?.descriptor as? PropertySetterDescriptor

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor =
        throw UnsupportedOperationException("Wrapped descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    override fun getReturnType() = (owner.getter?.returnType ?: owner.backingField?.type!!).toKotlinType()

    override fun hasStableParameterNames() = false

    override fun getType(): KotlinType = returnType

    override fun isVar() = owner.isVar

    override fun getDispatchReceiverParameter() = owner.getter?.dispatchReceiverParameter?.descriptor as? ReceiverParameterDescriptor

    override fun isConst() = owner.isConst

    override fun getContainingDeclaration(): DeclarationDescriptor = getContainingDeclaration(owner)

    override fun isLateInit() = owner.isLateinit

    override fun getExtensionReceiverParameter() = owner.getter?.extensionReceiverParameter?.descriptor as? ReceiverParameterDescriptor

    override fun isExternal() = owner.isExternal

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitPropertyDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitPropertyDescriptor(this, null)
    }

    override val getter: PropertyGetterDescriptor? get() = owner.getter?.descriptor as? PropertyGetterDescriptor

    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out PropertyDescriptor> {
        TODO("not implemented")
    }

    override val isDelegated get() = owner.isDelegated

    override fun getBackingField(): FieldDescriptor? {
        TODO("not implemented")
    }

    override fun getDelegateField(): FieldDescriptor? {
        TODO("not implemented")
    }

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}

class WrappedPropertyDescriptorWithContainerSource(
    override var containerSource: DeserializedContainerSource
) : WrappedPropertyDescriptor(), DescriptorWithContainerSource

abstract class WrappedPropertyAccessorDescriptor(annotations: Annotations, sourceElement: SourceElement) :
    WrappedSimpleFunctionDescriptor(annotations, sourceElement), PropertyAccessorDescriptor {
    override fun isDefault(): Boolean = false

    override fun getOriginal(): WrappedPropertyAccessorDescriptor = this

    override fun getOverriddenDescriptors() = super.getOverriddenDescriptors().map { it as PropertyAccessorDescriptor }

    override fun getCorrespondingProperty(): PropertyDescriptor = owner.correspondingPropertySymbol!!.descriptor

    override val correspondingVariable: VariableDescriptorWithAccessors get() = correspondingProperty
}

class WrappedPropertyGetterDescriptor(annotations: Annotations, sourceElement: SourceElement) :
    WrappedPropertyAccessorDescriptor(annotations, sourceElement), PropertyGetterDescriptor {
    override fun getOverriddenDescriptors() = super.getOverriddenDescriptors().map { it as PropertyGetterDescriptor }

    override fun getOriginal(): WrappedPropertyGetterDescriptor = this
}

class WrappedPropertySetterDescriptor(annotations: Annotations, sourceElement: SourceElement) :
    WrappedPropertyAccessorDescriptor(annotations, sourceElement), PropertySetterDescriptor {
    override fun getOverriddenDescriptors() = super.getOverriddenDescriptors().map { it as PropertySetterDescriptor }

    override fun getOriginal(): WrappedPropertySetterDescriptor = this
}

open class WrappedTypeAliasDescriptor(
    annotations: Annotations = Annotations.EMPTY,
    private val sourceElement: SourceElement = SourceElement.NO_SOURCE
) : WrappedDeclarationDescriptor<IrTypeAlias>(annotations), TypeAliasDescriptor {

    override val underlyingType: SimpleType
        get() = throw UnsupportedOperationException("Unexpected use of WrappedTypeAliasDescriptor $this")

    override val constructors: Collection<TypeAliasConstructorDescriptor>
        get() = throw UnsupportedOperationException("Unexpected use of WrappedTypeAliasDescriptor $this")

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters =
        throw UnsupportedOperationException("Wrapped descriptors should not be substituted")

    override fun getDefaultType(): SimpleType =
        throw UnsupportedOperationException("Unexpected use of WrappedTypeAliasDescriptor $this")

    override fun getTypeConstructor(): TypeConstructor =
        throw UnsupportedOperationException("Unexpected use of WrappedTypeAliasDescriptor $this")

    override val expandedType: SimpleType
        get() = owner.expandedType.toKotlinType() as SimpleType

    override val classDescriptor: ClassDescriptor?
        get() = expandedType.constructor.declarationDescriptor as ClassDescriptor?

    override fun getOriginal(): TypeAliasDescriptor = this

    override fun isInner(): Boolean = false

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = owner.typeParameters.map { it.descriptor }

    override fun getContainingDeclaration(): DeclarationDescriptor = getContainingDeclaration(owner)

    override fun getName(): Name = owner.name

    override fun getModality(): Modality = Modality.FINAL

    override fun getSource(): SourceElement = sourceElement

    override fun getVisibility(): Visibility = owner.visibility

    override fun isExpect(): Boolean = false

    override fun isActual(): Boolean = owner.isActual

    override fun isExternal(): Boolean = false

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
        visitor.visitTypeAliasDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitTypeAliasDescriptor(this, null)
    }
}

open class WrappedFieldDescriptor(
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

    override fun hasSynthesizedParameterNames() = false

    override fun getOverriddenDescriptors(): MutableCollection<out PropertyDescriptor> =
        owner.overriddenSymbols.map { it.descriptor }.toMutableList()

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

    override fun getTypeParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getVisibility() = owner.visibility

    override val setter: PropertySetterDescriptor? get() = null

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor =
        throw UnsupportedOperationException("Wrapped descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    override fun getReturnType() = owner.type.toKotlinType()

    override fun hasStableParameterNames(): Boolean {
        TODO("not implemented")
    }

    override fun getType(): KotlinType = owner.type.toKotlinType()

    override fun isVar() = !owner.isFinal

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? =
        if (owner.isStatic) null else (owner.parentAsClass.thisReceiver?.descriptor as ReceiverParameterDescriptor)

    override fun isConst() = false

    override fun getContainingDeclaration() = (owner.parent as IrSymbolOwner).symbol.descriptor

    override fun isLateInit() = owner.correspondingPropertySymbol?.owner?.isLateinit ?: false

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? =
        owner.correspondingPropertySymbol?.owner?.descriptor?.extensionReceiverParameter

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

    override fun getBackingField(): FieldDescriptor? {
        TODO("not implemented")
    }

    override fun getDelegateField(): FieldDescriptor? {
        TODO("not implemented")
    }

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}

private fun getContainingDeclaration(declaration: IrDeclarationWithName): DeclarationDescriptor {
    val parent = declaration.parent
    return if (parent is IrClass && parent.origin == IrDeclarationOrigin.FILE_CLASS && parent.parent is IrExternalPackageFragment) {
        // JVM IR adds facade classes for IR of functions/properties loaded from dependencies. However, these shouldn't exist
        // in the descriptor hierarchy, since this is what the old backend (dealing with descriptors) expects.
        return (parent.parent as IrExternalPackageFragment).packageFragmentDescriptor
    } else {
        (parent as IrSymbolOwner).symbol.descriptor
    }
}
