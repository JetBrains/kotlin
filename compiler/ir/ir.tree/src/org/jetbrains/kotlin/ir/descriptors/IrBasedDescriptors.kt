/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
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

/* Descriptors that serve purely as a view into IR structures.
   Created each time at the borderline between IR-based and descriptor-based code (such as inliner).
   Compared to WrappedDescriptors, no method calls ever return true descriptors, except when
   unbound symbols are encountered (see `Ir...Symbol.toIrBasedDescriptorIfPossible()`).
 */

abstract class IrBasedDeclarationDescriptor<T : IrDeclaration>(val owner: T) : DeclarationDescriptor {
    override val annotations: Annotations by lazy {
        val ownerAnnotations = (owner as? IrAnnotationContainer)?.annotations ?: return@lazy Annotations.EMPTY
        Annotations.create(ownerAnnotations.map { it.toAnnotationDescriptor() })
    }

    private fun IrConstructorCall.toAnnotationDescriptor(): AnnotationDescriptor {
        assert(symbol.owner.parentAsClass.isAnnotationClass) {
            "Expected call to constructor of annotation class but was: ${this.dump()}"
        }
        return AnnotationDescriptorImpl(
            symbol.owner.parentAsClass.defaultType.toIrBasedKotlinType(),
            symbol.owner.valueParameters.map { it.name to getValueArgument(it.index) }
                .filter { it.second != null }
                .associate { it.first to it.second!!.toConstantValue() },
            /*TODO*/ SourceElement.NO_SOURCE
        )
    }

    private fun IrElement.toConstantValue(): ConstantValue<*> {
        return when (this) {
            is IrConst<*> -> when (kind) {
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

            is IrVararg -> {
                val elements = elements.map { if (it is IrSpreadElement) error("$it is not expected") else it.toConstantValue() }
                ArrayValue(elements) { moduleDescriptor ->
                    // TODO: substitute.
                    moduleDescriptor.builtIns.array.defaultType
                }
            }

            is IrGetEnumValue -> EnumValue(symbol.owner.parentAsClass.toIrBasedDescriptor().classId!!, symbol.owner.name)

            is IrClassReference -> KClassValue((classType.classifierOrFail.owner as IrClass).toIrBasedDescriptor().classId!!, /*TODO*/0)

            is IrConstructorCall -> AnnotationValue(this.toAnnotationDescriptor())

            else -> error("$this is not expected: ${this.dump()}")
        }
    }

    override fun getContainingDeclaration(): DeclarationDescriptor =
        getContainingDeclaration(owner)

    override fun equals(other: Any?): Boolean =
        other is IrBasedDeclarationDescriptor<*> && owner == other.owner

    override fun hashCode(): Int = owner.hashCode()

    override fun toString(): String = javaClass.simpleName + ": " + owner.render()
}

fun IrDeclaration.toIrBasedDescriptor(): DeclarationDescriptor = when (this) {
    is IrValueParameter -> toIrBasedDescriptor()
    is IrTypeParameter -> toIrBasedDescriptor()
    is IrVariable -> toIrBasedDescriptor()
    is IrLocalDelegatedProperty -> toIrBasedDescriptor()
    is IrFunction -> toIrBasedDescriptor()
    is IrClass -> toIrBasedDescriptor()
    is IrAnonymousInitializer -> parentAsClass.toIrBasedDescriptor()
    is IrEnumEntry -> toIrBasedDescriptor()
    is IrProperty -> toIrBasedDescriptor()
    is IrField -> toIrBasedDescriptor()
    is IrTypeAlias -> toIrBasedDescriptor()
    is IrErrorDeclaration -> toIrBasedDescriptor()
    else -> error("Unknown declaration kind")
}

abstract class IrBasedCallableDescriptor<T : IrDeclaration>(owner: T) : CallableDescriptor, IrBasedDeclarationDescriptor<T>(owner) {

    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor =
        throw UnsupportedOperationException("IrBased descriptors SHOULD NOT be substituted")

    override fun getOverriddenDescriptors(): Collection<CallableDescriptor> {
        TODO("not implemented")
    }

    override fun getSource() = SourceElement.NO_SOURCE

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> = emptyList()

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        TODO("not implemented")
    }

    override fun getReturnType(): KotlinType? {
        TODO("not implemented")
    }

    override fun getValueParameters(): List<ValueParameterDescriptor> {
        TODO("not implemented")
    }

    override fun hasStableParameterNames(): Boolean {
        TODO("not implemented")
    }

    override fun hasSynthesizedParameterNames() = false

    override fun getVisibility(): DescriptorVisibility {
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

// Do not create this kind of descriptor for dispatch receiver parameters
// IrBasedReceiverParameterDescriptor should be used instead
open class IrBasedValueParameterDescriptor(owner: IrValueParameter) : ValueParameterDescriptor,
    IrBasedCallableDescriptor<IrValueParameter>(owner) {

    override val index get() = owner.index
    override val isCrossinline get() = owner.isCrossinline
    override val isNoinline get() = owner.isNoinline
    override val varargElementType get() = owner.varargElementType?.toIrBasedKotlinType()
    override fun isConst() = false
    override fun isVar() = false

    override fun getContainingDeclaration() = (owner.parent as IrFunction).toIrBasedDescriptor()
    override fun getType() = owner.type.toIrBasedKotlinType()
    override fun getName() = owner.name
    override fun declaresDefaultValue() = owner.defaultValue != null
    override fun getCompileTimeInitializer(): ConstantValue<*>? = null

    override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int) = TODO("not implemented")

    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor> = emptyList()
    override fun getTypeParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getValueParameters(): List<ValueParameterDescriptor> = emptyList()

    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): ValueParameterDescriptor =
        throw UnsupportedOperationException("IrBased descriptors SHOULD NOT be substituted")

    override fun getReturnType(): KotlinType? = owner.type.toIrBasedKotlinType()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitValueParameterDescriptor(this, data)!!

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitValueParameterDescriptor(this, null)
    }
}

open class IrBasedReceiverParameterDescriptor(owner: IrValueParameter) : ReceiverParameterDescriptor,
    IrBasedCallableDescriptor<IrValueParameter>(owner) {

    override fun getValue(): ReceiverValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType() = owner.type.toIrBasedKotlinType()
    override fun getName() = owner.name

    override fun copy(newOwner: DeclarationDescriptor) = TODO("not implemented")

    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor> = emptyList()

    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): ReceiverParameterDescriptor {
        TODO("")
    }

    override fun getReturnType(): KotlinType? = owner.type.toIrBasedKotlinType()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitReceiverParameterDescriptor(this, data)!!

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitReceiverParameterDescriptor(this, null)
    }
}

fun IrValueParameter.toIrBasedDescriptor() =
    if (index < 0)
        IrBasedReceiverParameterDescriptor(this)
    else
        IrBasedValueParameterDescriptor(this)

open class IrBasedTypeParameterDescriptor(owner: IrTypeParameter) : TypeParameterDescriptor,
    IrBasedDeclarationDescriptor<IrTypeParameter>(owner) {
    override fun getName() = owner.name

    override fun isReified() = owner.isReified

    override fun getVariance() = owner.variance

    override fun getUpperBounds() = owner.superTypes.map { it.toIrBasedKotlinType() }

    private val _typeConstructor: TypeConstructor by lazy {
        object : AbstractTypeConstructor(LockBasedStorageManager.NO_LOCKS) {
            override fun computeSupertypes() = upperBounds

            override val supertypeLoopChecker = SupertypeLoopChecker.EMPTY

            override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

            override fun isFinal() = false

            override fun isDenotable() = true

            override fun getDeclarationDescriptor() = this@IrBasedTypeParameterDescriptor

            override fun getBuiltIns() = module.builtIns

            override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean = declarationDescriptor === classifier
        }
    }

    override fun getTypeConstructor() = _typeConstructor

    override fun getOriginal() = this

    override fun getSource() = SourceElement.NO_SOURCE

    override fun getIndex() = owner.index

    override fun isCapturedFromOuterDeclaration() = false

    private val _defaultType: SimpleType by lazy {
        KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            Annotations.EMPTY, typeConstructor, emptyList(), false,
            LazyScopeAdapter {
                TypeIntersectionScope.create(
                    "Scope for type parameter " + name.asString(),
                    upperBounds
                )
            }
        )
    }

    override fun getDefaultType() = _defaultType

    override fun getStorageManager() = LockBasedStorageManager.NO_LOCKS

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        visitor!!.visitTypeParameterDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitTypeParameterDescriptor(this, null)
    }

    override fun toString(): String = super.toString() + "\nParent: $containingDeclaration"
}

fun IrTypeParameter.toIrBasedDescriptor() = IrBasedTypeParameterDescriptor(this)

open class IrBasedVariableDescriptor(owner: IrVariable) : VariableDescriptor, IrBasedCallableDescriptor<IrVariable>(owner) {

    override fun getContainingDeclaration() = (owner.parent as IrDeclaration).toIrBasedDescriptor()
    override fun getType() = owner.type.toIrBasedKotlinType()
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

fun IrVariable.toIrBasedDescriptor() = IrBasedVariableDescriptor(this)

open class IrBasedVariableDescriptorWithAccessor(owner: IrLocalDelegatedProperty) : VariableDescriptorWithAccessors,
    IrBasedCallableDescriptor<IrLocalDelegatedProperty>(owner) {
    override fun getName(): Name = owner.name

    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isVar() = owner.isVar

    override fun getCompileTimeInitializer(): ConstantValue<*>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(): KotlinType = owner.type.toIrBasedKotlinType()

    override fun isConst(): Boolean = false

    override fun getContainingDeclaration() = (owner.parent as IrDeclaration).toIrBasedDescriptor()

    override fun isLateInit(): Boolean = false

    override val getter: VariableAccessorDescriptor?
        get() = TODO("not implemented")
    override val setter: VariableAccessorDescriptor?
        get() = TODO("not implemented")
    override val isDelegated: Boolean = true
}

fun IrLocalDelegatedProperty.toIrBasedDescriptor() = IrBasedVariableDescriptorWithAccessor(this)

// We make all IR-based function descriptors instances of DescriptorWithContainerSource, and use .parentClassId to
// check whether declaration is deserialized. See IrInlineCodegen.descriptorIsDeserialized
open class IrBasedSimpleFunctionDescriptor(owner: IrSimpleFunction) : SimpleFunctionDescriptor, DescriptorWithContainerSource,
    IrBasedCallableDescriptor<IrSimpleFunction>(owner) {

    override fun getOverriddenDescriptors(): List<FunctionDescriptor> = owner.overriddenSymbols.map { it.owner.toIrBasedDescriptor() }

    override fun getModality() = owner.modality
    override fun getName() = owner.name
    override fun getVisibility() = owner.visibility
    override fun getReturnType() = owner.returnType.toIrBasedKotlinType()

    override fun getDispatchReceiverParameter() = owner.dispatchReceiverParameter?.run {
        (containingDeclaration as ClassDescriptor).thisAsReceiverParameter
    }

    override fun getExtensionReceiverParameter() = owner.extensionReceiverParameter?.toIrBasedDescriptor() as? ReceiverParameterDescriptor
    override fun getTypeParameters() = owner.typeParameters.map { it.toIrBasedDescriptor() }
    override fun getValueParameters() = owner.valueParameters
        .asSequence()
        .mapNotNull { it.toIrBasedDescriptor() as? ValueParameterDescriptor }
        .toMutableList()

    override fun isExternal() = owner.isExternal
    override fun isSuspend() = owner.isSuspend
    override fun isTailrec() = owner.isTailrec
    override fun isInline() = owner.isInline

    override fun isExpect() = false
    override fun isActual() = false
    override fun isInfix() = false
    override fun isOperator() = false

    override val containerSource: DeserializedContainerSource?
        get() = owner.containerSource

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

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): Nothing {
        TODO("not implemented")
    }

    override fun isHiddenToOvercomeSignatureClash(): Boolean = false
    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean = false

    override fun getInitialSignatureDescriptor(): FunctionDescriptor? = null

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

fun IrSimpleFunction.toIrBasedDescriptor() =
    when {
        isGetter -> IrBasedPropertyGetterDescriptor(this)
        isSetter -> IrBasedPropertySetterDescriptor(this)
        else -> IrBasedSimpleFunctionDescriptor(this)
    }

open class IrBasedClassConstructorDescriptor(owner: IrConstructor) : ClassConstructorDescriptor,
    IrBasedCallableDescriptor<IrConstructor>(owner) {
    override fun getContainingDeclaration() = (owner.parent as IrClass).toIrBasedDescriptor()

    override fun getDispatchReceiverParameter() = owner.dispatchReceiverParameter?.run {
        (containingDeclaration.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
    }

    override fun getTypeParameters() =
        (owner.constructedClass.typeParameters + owner.typeParameters).map { it.toIrBasedDescriptor() }

    override fun getValueParameters() = owner.valueParameters.asSequence()
        .mapNotNull { it.toIrBasedDescriptor() as? ValueParameterDescriptor }
        .toMutableList()

    override fun getOriginal() = this

    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor {
        TODO("not implemented")
    }

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
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

    override fun getConstructedClass() = (owner.parent as IrClass).toIrBasedDescriptor()

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

    override fun getReturnType() = owner.returnType.toIrBasedKotlinType()

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

fun IrConstructor.toIrBasedDescriptor() = IrBasedClassConstructorDescriptor(this)

fun IrFunction.toIrBasedDescriptor(): FunctionDescriptor =
    when (this) {
        is IrSimpleFunction -> toIrBasedDescriptor()
        is IrConstructor -> toIrBasedDescriptor()
        else -> error("Unknown function kind")
    }

open class IrBasedClassDescriptor(owner: IrClass) : ClassDescriptor, IrBasedDeclarationDescriptor<IrClass>(owner) {
    override fun getName() = owner.name

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>) = MemberScope.Empty

    override fun getMemberScope(typeSubstitution: TypeSubstitution) = MemberScope.Empty

    override fun getUnsubstitutedMemberScope() = MemberScope.Empty

    override fun getUnsubstitutedInnerClassesScope() = MemberScope.Empty

    override fun getStaticScope() = MemberScope.Empty

    override fun getSource() = owner.source

    override fun getConstructors() =
        owner.declarations.filterIsInstance<IrConstructor>().filter { !it.origin.isSynthetic }.map { it.toIrBasedDescriptor() }.toList()

    private val _defaultType: SimpleType by lazy {
        TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope, KotlinTypeFactory.EMPTY_REFINED_TYPE_FACTORY)
    }

    override fun getDefaultType(): SimpleType = _defaultType

    override fun getKind() = owner.kind

    override fun getModality() = owner.modality

    override fun getCompanionObjectDescriptor() =
        owner.declarations.filterIsInstance<IrClass>().firstOrNull { it.isCompanion }?.toIrBasedDescriptor()

    override fun getVisibility() = owner.visibility

    override fun isCompanionObject() = owner.isCompanion

    override fun isData() = owner.isData

    override fun isInline() = owner.isInline

    override fun isFun() = owner.isFun

    // In IR, inline and value are synonyms
    override fun isValue() = owner.isInline

    override fun getThisAsReceiverParameter() = owner.thisReceiver?.toIrBasedDescriptor() as ReceiverParameterDescriptor

    override fun getContextReceivers(): List<ReceiverParameterDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getUnsubstitutedPrimaryConstructor() =
        owner.declarations.filterIsInstance<IrConstructor>().singleOrNull { it.isPrimary }?.toIrBasedDescriptor()

    override fun getDeclaredTypeParameters() = owner.typeParameters.map { it.toIrBasedDescriptor() }

    override fun getSealedSubclasses(): Collection<ClassDescriptor> {
        TODO("not implemented")
    }

    override fun getInlineClassRepresentation(): InlineClassRepresentation<SimpleType>? =
        owner.inlineClassRepresentation?.mapUnderlyingType { it.toIrBasedKotlinType() as SimpleType }

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters =
        throw UnsupportedOperationException("IrBased descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    private val _typeConstructor: TypeConstructor by lazy {
        LazyTypeConstructor(
            this,
            ::collectTypeParameters,
            { owner.superTypes.map { it.toIrBasedKotlinType() } },
            LockBasedStorageManager.NO_LOCKS
        )
    }

    private fun collectTypeParameters(): List<TypeParameterDescriptor> =
        owner.typeConstructorParameters
            .map { it.toIrBasedDescriptor() }
            .toList()

    override fun getTypeConstructor(): TypeConstructor = _typeConstructor

    override fun isInner() = owner.isInner

    override fun isExternal() = owner.isExternal

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        visitor!!.visitClassDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitClassDescriptor(this, null)
    }

    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? {
        TODO("not implemented")
    }

    override fun isDefinitelyNotSamInterface(): Boolean {
        TODO("not implemented")
    }
}

fun IrClass.toIrBasedDescriptor() = IrBasedClassDescriptor(this)

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

open class IrBasedEnumEntryDescriptor(owner: IrEnumEntry) : ClassDescriptor, IrBasedDeclarationDescriptor<IrEnumEntry>(owner) {
    override fun getName() = owner.name

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>) = MemberScope.Empty

    override fun getMemberScope(typeSubstitution: TypeSubstitution) = MemberScope.Empty

    override fun getUnsubstitutedMemberScope() = MemberScope.Empty

    override fun getUnsubstitutedInnerClassesScope() = MemberScope.Empty

    override fun getStaticScope() = MemberScope.Empty

    override fun getSource() = SourceElement.NO_SOURCE

    override fun getConstructors() =
        getCorrespondingClass().declarations.asSequence().filterIsInstance<IrConstructor>().map { it.toIrBasedDescriptor() }.toList()

    private fun getCorrespondingClass() = owner.correspondingClass ?: (owner.parent as IrClass)

    private val _defaultType: SimpleType by lazy {
        TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope, KotlinTypeFactory.EMPTY_REFINED_TYPE_FACTORY)
    }

    override fun getDefaultType(): SimpleType = _defaultType

    override fun getKind() = ClassKind.ENUM_ENTRY

    override fun getModality() = Modality.FINAL

    override fun getCompanionObjectDescriptor() = null

    override fun getVisibility() = DescriptorVisibilities.DEFAULT_VISIBILITY

    override fun isCompanionObject() = false

    override fun isData() = false

    override fun isInline() = false

    override fun isFun() = false

    override fun isValue() = false

    override fun getThisAsReceiverParameter() = (owner.parent as IrClass).toIrBasedDescriptor().thisAsReceiverParameter

    override fun getContextReceivers(): List<ReceiverParameterDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        TODO("not implemented")
    }

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSealedSubclasses(): Collection<ClassDescriptor> {
        TODO("not implemented")
    }

    override fun getInlineClassRepresentation(): InlineClassRepresentation<SimpleType>? = TODO("not implemented")

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters =
        throw UnsupportedOperationException("IrBased descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    private val _typeConstructor: TypeConstructor by lazy {
        ClassTypeConstructorImpl(
            this,
            emptyList(),
            getCorrespondingClass().superTypes.map { it.toIrBasedKotlinType() },
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

    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? = null

    override fun isDefinitelyNotSamInterface() = true
}

fun IrEnumEntry.toIrBasedDescriptor() = IrBasedEnumEntryDescriptor(this)

open class IrBasedPropertyDescriptor(owner: IrProperty) :
    PropertyDescriptor, DescriptorWithContainerSource, IrBasedDeclarationDescriptor<IrProperty>(owner) {
    override fun getModality() = owner.modality

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented")
    }

    override fun getKind() = CallableMemberDescriptor.Kind.SYNTHESIZED

    override fun getName() = owner.name

    override fun getSource() = SourceElement.NO_SOURCE

    override fun hasSynthesizedParameterNames(): Boolean {
        TODO("not implemented")
    }

    override fun getOverriddenDescriptors(): MutableCollection<out PropertyDescriptor> = mutableListOf()

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
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

    override val setter: PropertySetterDescriptor? get() = owner.setter?.toIrBasedDescriptor() as? PropertySetterDescriptor

    override val containerSource: DeserializedContainerSource? = owner.containerSource

    override fun getOriginal() = this

    override fun isExpect() = false

    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor =
        throw UnsupportedOperationException("IrBased descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    override fun getReturnType() = (owner.getter?.returnType ?: owner.backingField?.type!!).toIrBasedKotlinType()

    override fun hasStableParameterNames() = false

    override fun getType(): KotlinType = returnType

    override fun isVar() = owner.isVar

    override fun getDispatchReceiverParameter() =
        owner.getter?.dispatchReceiverParameter?.toIrBasedDescriptor() as? ReceiverParameterDescriptor

    override fun isConst() = owner.isConst

    override fun isLateInit() = owner.isLateinit

    override fun getExtensionReceiverParameter() =
        owner.getter?.extensionReceiverParameter?.toIrBasedDescriptor() as? ReceiverParameterDescriptor

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> {
        TODO("Not yet implemented")
    }

    override fun isExternal() = owner.isExternal

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) =
        visitor!!.visitPropertyDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor!!.visitPropertyDescriptor(this, null)
    }

    override val getter: PropertyGetterDescriptor? get() = owner.getter?.toIrBasedDescriptor() as? PropertyGetterDescriptor

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

    override fun getInType(): KotlinType? = setter?.valueParameters?.get(0)?.type

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}

fun IrProperty.toIrBasedDescriptor() = IrBasedPropertyDescriptor(this)

abstract class IrBasedPropertyAccessorDescriptor(owner: IrSimpleFunction) : IrBasedSimpleFunctionDescriptor(owner),
    PropertyAccessorDescriptor {
    override fun isDefault(): Boolean = false

    override fun getOriginal(): IrBasedPropertyAccessorDescriptor = this

    override fun getOverriddenDescriptors() = super.getOverriddenDescriptors().map { it as PropertyAccessorDescriptor }

    override fun getCorrespondingProperty(): PropertyDescriptor = owner.correspondingPropertySymbol!!.owner.toIrBasedDescriptor()

    override val correspondingVariable: VariableDescriptorWithAccessors get() = correspondingProperty
}

open class IrBasedPropertyGetterDescriptor(owner: IrSimpleFunction) : IrBasedPropertyAccessorDescriptor(owner), PropertyGetterDescriptor {
    override fun getOverriddenDescriptors() = super.getOverriddenDescriptors().map { it as PropertyGetterDescriptor }

    override fun getOriginal(): IrBasedPropertyGetterDescriptor = this
}

open class IrBasedPropertySetterDescriptor(owner: IrSimpleFunction) : IrBasedPropertyAccessorDescriptor(owner), PropertySetterDescriptor {
    override fun getOverriddenDescriptors() = super.getOverriddenDescriptors().map { it as PropertySetterDescriptor }

    override fun getOriginal(): IrBasedPropertySetterDescriptor = this
}

open class IrBasedTypeAliasDescriptor(owner: IrTypeAlias) : IrBasedDeclarationDescriptor<IrTypeAlias>(owner), TypeAliasDescriptor {

    override val underlyingType: SimpleType
        get() = throw UnsupportedOperationException("Unexpected use of IrBasedTypeAliasDescriptor $this")

    override val constructors: Collection<TypeAliasConstructorDescriptor>
        get() = throw UnsupportedOperationException("Unexpected use of IrBasedTypeAliasDescriptor $this")

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters =
        throw UnsupportedOperationException("IrBased descriptors should not be substituted")

    override fun getDefaultType(): SimpleType =
        throw UnsupportedOperationException("Unexpected use of IrBasedTypeAliasDescriptor $this")

    override fun getTypeConstructor(): TypeConstructor =
        throw UnsupportedOperationException("Unexpected use of IrBasedTypeAliasDescriptor $this")

    override val expandedType: SimpleType
        get() = owner.expandedType.toIrBasedKotlinType() as SimpleType

    override val classDescriptor: ClassDescriptor?
        get() = TODO("catch type alias class descriptor") //expandedType.constructor.declarationDescriptor as ClassDescriptor?

    override fun getOriginal(): TypeAliasDescriptor = this

    override fun isInner(): Boolean = false

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = owner.typeParameters.map { it.toIrBasedDescriptor() }

    override fun getName(): Name = owner.name

    override fun getModality(): Modality = Modality.FINAL

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE

    override fun getVisibility(): DescriptorVisibility = owner.visibility

    override fun isExpect(): Boolean = false

    override fun isActual(): Boolean = owner.isActual

    override fun isExternal(): Boolean = false

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
        visitor.visitTypeAliasDescriptor(this, data)

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitTypeAliasDescriptor(this, null)
    }
}

fun IrTypeAlias.toIrBasedDescriptor() = IrBasedTypeAliasDescriptor(this)

open class IrBasedFieldDescriptor(owner: IrField) : PropertyDescriptor, IrBasedDeclarationDescriptor<IrField>(owner) {
    override fun getModality() = if (owner.isFinal) Modality.FINAL else Modality.OPEN

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("not implemented")
    }

    override fun getKind() = CallableMemberDescriptor.Kind.SYNTHESIZED

    override fun getName() = owner.name

    override fun getSource() = SourceElement.NO_SOURCE

    override fun hasSynthesizedParameterNames() = false

    override fun getOverriddenDescriptors(): MutableCollection<out PropertyDescriptor> = mutableListOf()

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
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
        throw UnsupportedOperationException("IrBased descriptors SHOULD NOT be substituted")

    override fun isActual() = false

    override fun getReturnType() = owner.type.toIrBasedKotlinType()

    override fun hasStableParameterNames(): Boolean {
        TODO("not implemented")
    }

    override fun getType(): KotlinType = owner.type.toIrBasedKotlinType()

    override fun isVar() = !owner.isFinal

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? =
        if (owner.isStatic) null else (owner.parentAsClass.thisReceiver?.toIrBasedDescriptor() as ReceiverParameterDescriptor)

    override fun isConst() = false

    override fun isLateInit() = owner.correspondingPropertySymbol?.owner?.isLateinit ?: false

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? =
        owner.correspondingPropertySymbol?.owner?.toIrBasedDescriptor()?.extensionReceiverParameter

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> {
        TODO("Not yet implemented")
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

    // Following functions are used in error reporting when rendering annotations on properties
    override fun getBackingField(): FieldDescriptor? = null // TODO
    override fun getDelegateField(): FieldDescriptor? = null // TODO

    override fun getInType(): KotlinType? = setter?.valueParameters?.get(0)?.type

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}

fun IrField.toIrBasedDescriptor() = IrBasedFieldDescriptor(this)

class IrBasedErrorDescriptor(owner: IrErrorDeclaration) : IrBasedDeclarationDescriptor<IrErrorDeclaration>(owner) {
    override fun getName(): Name = error("IrBasedErrorDescriptor.getName: Should not be reached")

    override fun getOriginal(): DeclarationDescriptorWithSource =
        error("IrBasedErrorDescriptor.getOriginal: Should not be reached")

    override fun getContainingDeclaration(): DeclarationDescriptor =
        error("IrBasedErrorDescriptor.getContainingDeclaration: Should not be reached")

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
        error("IrBasedErrorDescriptor.accept: Should not be reached")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
    }
}

fun IrErrorDeclaration.toIrBasedDescriptor() = IrBasedErrorDescriptor(this)

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun getContainingDeclaration(declaration: IrDeclaration): DeclarationDescriptor {
    val parent = declaration.parent
    val parentDescriptor = (parent as IrSymbolOwner).let {
        if (it is IrDeclaration) it.toIrBasedDescriptor() else it.symbol.descriptor
    }
    return if (parent is IrClass && parent.isFileClass) {
        // JVM IR adds facade classes for IR of functions/properties loaded both from sources and dependencies. However, these shouldn't
        // exist in the descriptor hierarchy, since this is what the old backend (dealing with descriptors) expects.
        parentDescriptor.containingDeclaration!!
    } else {
        parentDescriptor
    }
}

fun IrType.toIrBasedKotlinType(): KotlinType = when (this) {
    is IrSimpleType -> makeKotlinType(classifier, arguments, hasQuestionMark)
    else -> TODO(toString())
}

private fun makeKotlinType(
    classifier: IrClassifierSymbol,
    arguments: List<IrTypeArgument>,
    hasQuestionMark: Boolean
): SimpleType =
    when (classifier) {
        is IrTypeParameterSymbol -> classifier.toIrBasedDescriptorIfPossible().defaultType
        is IrClassSymbol -> {
            val classDescriptor = classifier.toIrBasedDescriptorIfPossible()
            val kotlinTypeArguments = arguments.mapIndexed { index, it ->
                when (it) {
                    is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toIrBasedKotlinType())
                    is IrStarProjection -> StarProjectionImpl(classDescriptor.typeConstructor.parameters[index])
                    else -> error(it)
                }
            }

            try {
                classDescriptor.defaultType.replace(newArguments = kotlinTypeArguments).makeNullableAsSpecified(hasQuestionMark)
            } catch (e: Throwable) {
                throw RuntimeException(
                    "Classifier: ${classifier.owner.render()}\n" +
                            "Type parameters:\n" +
                            classDescriptor.defaultType.constructor.parameters.withIndex()
                                .joinToString(separator = "\n") {
                                    val irTypeParameter = (it.value as IrBasedTypeParameterDescriptor).owner
                                    "${it.index}: ${irTypeParameter.render()} " +
                                            "of ${irTypeParameter.parent.render()}"
                                } +
                            "\nType arguments:\n" +
                            arguments.withIndex()
                                .joinToString(separator = "\n") {
                                    "${it.index}: ${it.value.render()}"
                                },
                    e
                )
            }
        }
        else -> error("unknown classifier kind $classifier")
    }

/* When IR-based descriptors are used from Psi2Ir, symbols may be unbound, thus we may need to resort to real descriptors. */
@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrClassSymbol.toIrBasedDescriptorIfPossible(): ClassDescriptor =
    if (isBound) owner.toIrBasedDescriptor() else descriptor

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrTypeParameterSymbol.toIrBasedDescriptorIfPossible(): TypeParameterDescriptor =
    if (isBound) owner.toIrBasedDescriptor() else descriptor

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrSimpleFunctionSymbol.toIrBasedDescriptorIfPossible(): FunctionDescriptor =
    if (isBound) owner.toIrBasedDescriptor() else descriptor

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrPropertySymbol.toIrBasedDescriptorIfPossible(): PropertyDescriptor =
    if (isBound) owner.toIrBasedDescriptor() else descriptor
