/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.old

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtPureElement
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


/**
 * List of known problems/not-implemented:
 * - getOrigin return itself all the time. Formally it should return "unsubstituted" version of itself, but it isn't clear,
 *      if we really needed that nor how to implemented it. Btw, it is relevant only to member functions/properties (unlikely, but may be to
 *      inner classes) and not for "resulting descriptor". I.e. only class/interface type parameters could be substituted in FIR,
 *      there is no "resulting descriptor" and type arguments are passed together with the target function.
 * - equals on descriptors and type constructors. It isn't clear what type of equality we should implement,
 *      will figure that later o real cases.
 * See the functions below
 */

internal fun KtSymbolBasedContext.containerDeclarationImplementationPostponed(): Nothing =
    implementationPostponed("It isn't clear what we really need and how to implement it")

internal fun KtSymbolBasedContext.typeAliasImplementationPlanned(): Nothing =
    implementationPlanned("It is easy to implement, but it isn't first priority")

interface KtSymbolBasedNamed : Named {
    val ktSymbol: KtNamedSymbol
    override fun getName(): Name = ktSymbol.name
}

private fun KtSymbolVisibility.toDescriptorVisibility(): DescriptorVisibility =
    when (this) {
        KtSymbolVisibility.PUBLIC -> DescriptorVisibilities.PUBLIC
        KtSymbolVisibility.PRIVATE -> DescriptorVisibilities.PRIVATE
        KtSymbolVisibility.PRIVATE_TO_THIS -> DescriptorVisibilities.PRIVATE_TO_THIS
        KtSymbolVisibility.PROTECTED -> DescriptorVisibilities.PROTECTED
        KtSymbolVisibility.INTERNAL -> DescriptorVisibilities.INTERNAL
        KtSymbolVisibility.LOCAL -> DescriptorVisibilities.LOCAL
        KtSymbolVisibility.UNKNOWN -> DescriptorVisibilities.UNKNOWN
    }

private fun KtSymbolModality.toDescriptorModality(): Modality =
    when (this) {
        KtSymbolModality.SEALED -> Modality.SEALED
        KtCommonSymbolModality.FINAL -> Modality.FINAL
        KtCommonSymbolModality.ABSTRACT -> Modality.ABSTRACT
        KtCommonSymbolModality.OPEN -> Modality.OPEN
    }

private fun KtClassKind.toDescriptorKlassKind(): ClassKind =
    when (this) {
        KtClassKind.CLASS -> ClassKind.CLASS
        KtClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
        KtClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
        KtClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
        KtClassKind.OBJECT, KtClassKind.COMPANION_OBJECT -> ClassKind.OBJECT
        KtClassKind.INTERFACE -> ClassKind.INTERFACE
    }


abstract class KtSymbolBasedDeclarationDescriptor(val context: KtSymbolBasedContext) : DeclarationDescriptorWithSource {
    abstract val ktSymbol: KtSymbol
    override val annotations: Annotations
        get() {
            val ktAnnotations = (ktSymbol as? KtAnnotatedSymbol)?.annotations ?: return Annotations.EMPTY
            return Annotations.create(ktAnnotations.map { KtSymbolBasedAnnotationDescriptor(it, context) })
        }

    override fun getSource(): SourceElement = ktSymbol.psi.safeAs<KtPureElement>().toSourceElement()

    private fun KtSymbol.toSignature(): IdSignature = (this as KtFirSymbol<*>).firRef.withFir { it.createSignature() }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is KtSymbolBasedDeclarationDescriptor) return false

        ktSymbol.psi?.let {
            return other.ktSymbol.psi == it
        }

        return ktSymbol.toSignature() == other.ktSymbol.toSignature()
    }

    override fun hashCode(): Int {
        ktSymbol.psi?.let { return it.hashCode() }
        return ktSymbol.toSignature().hashCode()
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R = noImplementation()
    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?): Unit = noImplementation()

    // stub for automatic Substitutable<T> implementation for all relevant subclasses
    fun substitute(substitutor: TypeSubstitutor): Nothing = noImplementation()

    protected fun noImplementation(): Nothing = context.noImplementation("ktSymbol = $ktSymbol")
    protected fun implementationPostponed(): Nothing = context.implementationPostponed("ktSymbol = $ktSymbol")
    protected fun implementationPlanned(): Nothing = context.implementationPlanned("ktSymbol = $ktSymbol")
}

class KtSymbolBasedAnnotationDescriptor(
    private val ktAnnotationCall: KtAnnotationCall,
    val context: KtSymbolBasedContext
) : AnnotationDescriptor {
    override val type: KotlinType
        get() = context.implementationPlanned("ktAnnotationCall = $ktAnnotationCall")

    override val fqName: FqName?
        get() = ktAnnotationCall.classId?.asSingleFqName()

    override val allValueArguments: Map<Name, ConstantValue<*>> =
        ktAnnotationCall.arguments.associate { Name.identifier(it.name) to it.expression.toConstantValue() }

    override val source: SourceElement
        get() = ktAnnotationCall.psi.toSourceElement()

    private fun KtConstantValue.toConstantValue(): ConstantValue<*> =
        when (this) {
            KtUnsupportedConstantValue -> ErrorValue.create("Error value for KtUnsupportedConstantValue")
            is KtSimpleConstantValue<*> -> when (constantValueKind) {
                ConstantValueKind.Null -> NullValue()
                ConstantValueKind.Boolean -> BooleanValue(value as Boolean)
                ConstantValueKind.Char -> CharValue(value as Char)
                ConstantValueKind.Byte -> ByteValue(value as Byte)
                ConstantValueKind.Short -> ShortValue(value as Short)
                ConstantValueKind.Int -> IntValue(value as Int)
                ConstantValueKind.Long -> LongValue(value as Long)
                ConstantValueKind.String -> StringValue(value as String)
                ConstantValueKind.Float -> FloatValue(value as Float)
                ConstantValueKind.Double -> DoubleValue(value as Double)
                ConstantValueKind.UnsignedByte -> UByteValue(value as Byte)
                ConstantValueKind.UnsignedShort -> UShortValue(value as Short)
                ConstantValueKind.UnsignedInt -> UIntValue(value as Int)
                ConstantValueKind.UnsignedLong -> ULongValue(value as Long)
                else -> error("Unexpected constant KtSimpleConstantValue: $value (class: ${value?.javaClass}")
            }
        }
}

class KtSymbolBasedClassDescriptor(override val ktSymbol: KtClassOrObjectSymbol, context: KtSymbolBasedContext) :
    KtSymbolBasedDeclarationDescriptor(context), KtSymbolBasedNamed, ClassDescriptor {

    override fun isInner(): Boolean = ktSymbol.isInner
    override fun isCompanionObject(): Boolean = ktSymbol.classKind == KtClassKind.COMPANION_OBJECT
    override fun isData(): Boolean = ktSymbol.isData
    override fun isInline(): Boolean = ktSymbol.isInline // seems like th`is `flag should be removed in favor of isValue
    override fun isValue(): Boolean = ktSymbol.isInline
    override fun isFun(): Boolean = ktSymbol.isFun

    override fun isExpect(): Boolean = implementationPostponed()
    override fun isActual(): Boolean = implementationPostponed()
    override fun isExternal(): Boolean = ktSymbol.isExternal

    override fun getVisibility(): DescriptorVisibility = ktSymbol.visibility.toDescriptorVisibility()
    override fun getModality(): Modality = ktSymbol.modality.toDescriptorModality()

    override fun getKind(): ClassKind = ktSymbol.classKind.toDescriptorKlassKind()

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = ktSymbol.companionObject?.let {
        KtSymbolBasedClassDescriptor(it, context)
    }

    override fun getTypeConstructor(): TypeConstructor = KtSymbolBasedClassTypeConstructor(this)

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> =
        ktSymbol.typeParameters.map { KtSymbolBasedTypeParameterDescriptor(it, context) }

    override fun getDefaultType(): SimpleType {
        val arguments = TypeUtils.getDefaultTypeProjections(typeConstructor.parameters)
        return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            Annotations.EMPTY, typeConstructor, arguments, false,
            MemberScopeForKtSymbolBasedDescriptors { "ktSymbol = $ktSymbol" }
        )
    }

    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor =
        ReceiverParameterDescriptorImpl(this, ImplicitClassReceiver(this), Annotations.EMPTY)

    override fun getOriginal(): ClassDescriptor = this

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = with(context.ktAnalysisSession) {
        ktSymbol.getDeclaredMemberScope().getConstructors().firstOrNull { it.isPrimary }
            ?.let { KtSymbolBasedConstructorDescriptor(it, this@KtSymbolBasedClassDescriptor) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getConstructors(): Collection<ClassConstructorDescriptor> = with(context.ktAnalysisSession) {
        ktSymbol.getDeclaredMemberScope().getConstructors().map {
            KtSymbolBasedConstructorDescriptor(it, this@KtSymbolBasedClassDescriptor)
        }.toList()
    }

    override fun getContainingDeclaration(): DeclarationDescriptor {
        val containerSymbol = with(context.ktAnalysisSession) {
            ktSymbol.getContainingSymbol()
        }
        return when (containerSymbol) {
            null -> {
                val currentClassId = ktSymbol.classIdIfNonLocal ?: error("should be top-level")
                KtSymbolBasedPackageFragmentDescriptor(currentClassId.packageFqName, context)
            }
            is KtClassOrObjectSymbol -> KtSymbolBasedClassDescriptor(containerSymbol, context)
            else -> noImplementation()
        }
    }

    override fun getSealedSubclasses(): Collection<ClassDescriptor> = implementationPostponed()

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope = noImplementation()
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope = noImplementation()
    override fun getUnsubstitutedMemberScope(): MemberScope = noImplementation()
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = noImplementation()
    override fun getStaticScope(): MemberScope = noImplementation()

    override fun isDefinitelyNotSamInterface(): Boolean = noImplementation()
    override fun getDefaultFunctionTypeForSamInterface(): SimpleType = noImplementation()
}

class KtSymbolBasedTypeParameterDescriptor(
    override val ktSymbol: KtTypeParameterSymbol, context: KtSymbolBasedContext
) : KtSymbolBasedDeclarationDescriptor(context), KtSymbolBasedNamed, TypeParameterDescriptor {
    override fun isReified(): Boolean = ktSymbol.isReified
    override fun getVariance(): Variance = ktSymbol.variance

    override fun getTypeConstructor(): TypeConstructor = KtSymbolBasedTypeParameterTypeConstructor(this)

    override fun getDefaultType(): SimpleType =
        KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            Annotations.EMPTY, typeConstructor, emptyList(), false,
            MemberScopeForKtSymbolBasedDescriptors { "ktSymbol = $ktSymbol" }
        )

    override fun getUpperBounds(): List<KotlinType> = ktSymbol.upperBounds.map { it.toKotlinType(context) }
    override fun getOriginal(): TypeParameterDescriptor = this

    override fun getContainingDeclaration(): DeclarationDescriptor = context.containerDeclarationImplementationPostponed()

    // there is no such thing in FIR, and it seems like it isn't really needed for IDE and could be bypassed on client site
    override fun getIndex(): Int = implementationPostponed()

    override fun isCapturedFromOuterDeclaration(): Boolean = noImplementation()
    override fun getStorageManager(): StorageManager = noImplementation()
}

abstract class KtSymbolBasedFunctionLikeDescriptor(context: KtSymbolBasedContext) :
    KtSymbolBasedDeclarationDescriptor(context), FunctionDescriptor {
    abstract override val ktSymbol: KtFunctionLikeSymbol

    override fun getReturnType(): KotlinType = ktSymbol.annotatedType.toKotlinType(context)

    override fun getValueParameters(): List<ValueParameterDescriptor> = ktSymbol.valueParameters.mapIndexed { index, it ->
        it.toValueParameterDescriptor(index)
    }

    private fun KtParameterSymbol.toValueParameterDescriptor(index: Int): ValueParameterDescriptor {
        return ValueParameterDescriptorImpl(
            this@KtSymbolBasedFunctionLikeDescriptor,
            null,
            index,
            Annotations.EMPTY, // TODO
            name,
            annotatedType.toKotlinType(context),
            this.hasDefaultValue,
            context.incorrectImplementation { false },
            context.incorrectImplementation { false },
            context.incorrectImplementation { null },
            psi.safeAs<KtPureElement>().toSourceElement()
        )
    }

    override fun hasStableParameterNames(): Boolean = implementationPostponed()
    override fun hasSynthesizedParameterNames(): Boolean = implementationPostponed()
    override fun getKind(): CallableMemberDescriptor.Kind = implementationPostponed()

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null

    override fun getOriginal(): FunctionDescriptor = this
    override fun getContainingDeclaration(): DeclarationDescriptor = context.containerDeclarationImplementationPostponed()

    override fun getOverriddenDescriptors(): Collection<FunctionDescriptor> = context.incorrectImplementation { emptyList() }
    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean = implementationPostponed()

    override fun getInitialSignatureDescriptor(): FunctionDescriptor? = noImplementation()
    override fun isHiddenToOvercomeSignatureClash(): Boolean = noImplementation()

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) = noImplementation()
    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> = noImplementation()
    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): FunctionDescriptor = noImplementation()
}

class KtSymbolBasedFunctionDescriptor(override val ktSymbol: KtFunctionSymbol, context: KtSymbolBasedContext) :
    KtSymbolBasedFunctionLikeDescriptor(context),
    SimpleFunctionDescriptor,
    KtSymbolBasedNamed {
    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = getExtensionReceiverParameter(ktSymbol)
    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = getDispatchReceiverParameter(ktSymbol)
    override fun getContainingDeclaration(): DeclarationDescriptor {
        val containerSymbol = with(context.ktAnalysisSession) {
            ktSymbol.getContainingSymbol()
        }
        return when (containerSymbol) {
            null -> { // i.e. function is top-level
                val currentClassId = ktSymbol.callableIdIfNonLocal ?: error("should be top-level")
                KtSymbolBasedPackageFragmentDescriptor(currentClassId.parent(), context)
            }
            is KtClassOrObjectSymbol -> KtSymbolBasedClassDescriptor(containerSymbol, context)
            is KtFunctionSymbol -> KtSymbolBasedFunctionDescriptor(containerSymbol, context)
            else -> noImplementation()
        }
    }

    override fun isSuspend(): Boolean = ktSymbol.isSuspend
    override fun isOperator(): Boolean = ktSymbol.isOperator
    override fun isExternal(): Boolean = ktSymbol.isExternal
    override fun isInline(): Boolean = ktSymbol.isInline
    override fun isInfix(): Boolean = implementationPostponed()
    override fun isTailrec(): Boolean = implementationPostponed()

    override fun isExpect(): Boolean = implementationPostponed()
    override fun isActual(): Boolean = implementationPostponed()

    override fun getVisibility(): DescriptorVisibility = ktSymbol.visibility.toDescriptorVisibility()
    override fun getModality(): Modality = ktSymbol.modality.toDescriptorModality()

    override fun getTypeParameters(): List<TypeParameterDescriptor> =
        ktSymbol.typeParameters.map { KtSymbolBasedTypeParameterDescriptor(it, context) }

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): SimpleFunctionDescriptor = context.noImplementation()

    override fun getOriginal(): SimpleFunctionDescriptor = context.incorrectImplementation { this }

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out SimpleFunctionDescriptor> =
        context.noImplementation()
}

class KtSymbolBasedConstructorDescriptor(
    override val ktSymbol: KtConstructorSymbol,
    private val ktSBClassDescriptor: KtSymbolBasedClassDescriptor
) : KtSymbolBasedFunctionLikeDescriptor(ktSBClassDescriptor.context),
    ClassConstructorDescriptor {
    override fun getName(): Name = Name.special("<init>")

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null
    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = getDispatchReceiverParameter(ktSymbol)

    override fun getConstructedClass(): ClassDescriptor = ktSBClassDescriptor
    override fun isPrimary(): Boolean = ktSymbol.isPrimary

    override fun getReturnType(): KotlinType = ktSBClassDescriptor.defaultType

    override fun isSuspend(): Boolean = false
    override fun isOperator(): Boolean = false
    override fun isExternal(): Boolean = false
    override fun isInline(): Boolean = false
    override fun isInfix(): Boolean = false
    override fun isTailrec(): Boolean = false

    override fun isExpect(): Boolean = implementationPostponed()
    override fun isActual(): Boolean = implementationPostponed()

    override fun getVisibility(): DescriptorVisibility = ktSymbol.visibility.toDescriptorVisibility()
    override fun getModality(): Modality = Modality.FINAL

    override fun getTypeParameters(): List<TypeParameterDescriptor> =
        ktSymbol.typeParameters.map { KtSymbolBasedTypeParameterDescriptor(it, context) }

    override fun getOriginal(): ClassConstructorDescriptor = this
    override fun getContainingDeclaration(): ClassDescriptor = ktSBClassDescriptor

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): ClassConstructorDescriptor = noImplementation()
}

class KtSymbolBasedAnonymousDescriptor(
    override val ktSymbol: KtAnonymousFunctionSymbol,
    context: KtSymbolBasedContext
) : KtSymbolBasedFunctionLikeDescriptor(context) {
    override fun getName(): Name = SpecialNames.ANONYMOUS_FUNCTION

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = getExtensionReceiverParameter(ktSymbol)
    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun isOperator(): Boolean = false
    override fun isExternal(): Boolean = false
    override fun isInline(): Boolean = false
    override fun isInfix(): Boolean = false
    override fun isTailrec(): Boolean = false
    override fun isExpect(): Boolean = false
    override fun isActual(): Boolean = false

    override fun getVisibility(): DescriptorVisibility = DescriptorVisibilities.LOCAL
    override fun getModality(): Modality = Modality.FINAL
    override fun getTypeParameters(): List<TypeParameterDescriptor> = emptyList()

    // it doesn't seems like isSuspend are used in FIR for anonymous functions, but it used in FIR2IR so if we really need that
    // we could implement that later
    override fun isSuspend(): Boolean = implementationPostponed()
}

private fun KtSymbolBasedDeclarationDescriptor.getDispatchReceiverParameter(ktSymbol: KtPossibleMemberSymbol): ReceiverParameterDescriptor? {
    val ktDispatchTypeAndAnnotations = ktSymbol.dispatchType ?: return null
    return KtSymbolStubDispatchReceiverParameterDescriptor(ktDispatchTypeAndAnnotations, context)
}

private fun <T> T.getExtensionReceiverParameter(
    ktSymbol: KtPossibleExtensionSymbol
): ReceiverParameterDescriptor? where T : KtSymbolBasedDeclarationDescriptor, T : CallableDescriptor {
    val receiverTypeAndAnnotation = ktSymbol.receiverType ?: return null
    val receiverValue = ExtensionReceiver(this, receiverTypeAndAnnotation.type.toKotlinType(context), null)
    return ReceiverParameterDescriptorImpl(this, receiverValue, receiverTypeAndAnnotation.getDescriptorsAnnotations(context))
}

private class KtSymbolBasedReceiverValue(val ktType: KtType, val context: KtSymbolBasedContext) : ReceiverValue {
    override fun getType(): KotlinType = ktType.toKotlinType(context)

    override fun replaceType(newType: KotlinType): ReceiverValue = context.noImplementation("Should be called from IDE")
    override fun getOriginal(): ReceiverValue = this
}

// Don't think that annotation is important here, because containingDeclaration used way more then annotation and they are not supported here
private class KtSymbolStubDispatchReceiverParameterDescriptor(
    val receiverType: KtType,
    val context: KtSymbolBasedContext
) : AbstractReceiverParameterDescriptor(Annotations.EMPTY) {
    override fun getContainingDeclaration(): DeclarationDescriptor = context.containerDeclarationImplementationPostponed()

    override fun getValue(): ReceiverValue = KtSymbolBasedReceiverValue(receiverType, context)

    override fun copy(newOwner: DeclarationDescriptor): ReceiverParameterDescriptor =
        context.noImplementation("Copy should be called from IDE code")
}

class KtSymbolBasedPackageFragmentDescriptor(
    override val fqName: FqName,
    val context: KtSymbolBasedContext
) : PackageFragmentDescriptor {

    override fun getName(): Name = fqName.shortName()

    override fun getOriginal(): DeclarationDescriptorWithSource = this

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE

    override val annotations: Annotations
        get() = Annotations.EMPTY

    override fun getContainingDeclaration(): ModuleDescriptor = context.moduleDescriptor

    override fun getMemberScope(): MemberScope = context.noImplementation()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        context.noImplementation("IDE shouldn't use visitor on descriptors")

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) =
        context.noImplementation("IDE shouldn't use visitor on descriptors")
}