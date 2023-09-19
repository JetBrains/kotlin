/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtAnalysisApiInternals::class)

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KtFe10FileSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KtFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.KtFe10PsiDefaultPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.KtFe10PsiDefaultPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.KtFe10PsiDefaultSetterParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.types.*
import org.jetbrains.kotlin.analysis.api.impl.base.KtContextReceiverImpl
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaForKotlinOverridePropertyDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitContextReceiver
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils

internal val MemberDescriptor.ktSymbolKind: KtSymbolKind
    get() {
        return when (this) {
            is PropertyAccessorDescriptor -> KtSymbolKind.ACCESSOR
            is SamConstructorDescriptor -> KtSymbolKind.SAM_CONSTRUCTOR
            else -> when (containingDeclaration) {
                is PackageFragmentDescriptor -> KtSymbolKind.TOP_LEVEL
                is ClassDescriptor -> KtSymbolKind.CLASS_MEMBER
                else -> KtSymbolKind.LOCAL
            }
        }
    }

internal val CallableMemberDescriptor.isExplicitOverride: Boolean
    get() {
        return (this !is PropertyAccessorDescriptor
                && kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                && overriddenDescriptors.isNotEmpty())
    }

internal val ClassDescriptor.isInterfaceLike: Boolean
    get() = when (kind) {
        ClassKind.CLASS, ClassKind.ENUM_CLASS, ClassKind.OBJECT, ClassKind.ENUM_ENTRY -> false
        else -> true
    }

internal fun DeclarationDescriptor.toKtSymbol(analysisContext: Fe10AnalysisContext): KtSymbol? {
    if (this is ClassDescriptor && kind == ClassKind.ENUM_ENTRY) {
        return KtFe10DescEnumEntrySymbol(this, analysisContext)
    }

    return when (this) {
        is ClassifierDescriptor -> toKtClassifierSymbol(analysisContext)
        is ReceiverParameterDescriptor -> toKtReceiverParameterSymbol(analysisContext)
        is CallableDescriptor -> toKtCallableSymbol(analysisContext)
        is PackageViewDescriptor -> toKtPackageSymbol(analysisContext)
        else -> null
    }
}

internal fun ClassifierDescriptor.toKtClassifierSymbol(analysisContext: Fe10AnalysisContext): KtClassifierSymbol? {
    return when (this) {
        is TypeAliasDescriptor -> KtFe10DescTypeAliasSymbol(this, analysisContext)
        is TypeParameterDescriptor -> KtFe10DescTypeParameterSymbol(this, analysisContext)
        is ClassDescriptor -> toKtClassSymbol(analysisContext)
        else -> null
    }
}

internal fun ClassDescriptor.toKtClassSymbol(analysisContext: Fe10AnalysisContext): KtClassOrObjectSymbol {
    return if (DescriptorUtils.isAnonymousObject(this)) {
        KtFe10DescAnonymousObjectSymbol(this, analysisContext)
    } else {
        KtFe10DescNamedClassOrObjectSymbol(this, analysisContext)
    }
}

internal fun PackageViewDescriptor.toKtPackageSymbol(analysisContext: Fe10AnalysisContext): KtPackageSymbol {
    return KtFe10PackageSymbol(fqName, analysisContext)
}

internal fun ReceiverParameterDescriptor.toKtReceiverParameterSymbol(analysisContext: Fe10AnalysisContext): KtReceiverParameterSymbol {
    return KtFe10ReceiverParameterSymbol(this, analysisContext)
}

internal fun KtSymbol.getDescriptor(): DeclarationDescriptor? {
    return when (this) {
        is KtFe10PsiSymbol<*, *> -> descriptor
        is KtFe10DescSymbol<*> -> descriptor
        is KtFe10DescSyntheticFieldSymbol -> descriptor
        is KtFe10PsiDefaultPropertyGetterSymbol -> descriptor
        is KtFe10PsiDefaultSetterParameterSymbol -> descriptor
        is KtFe10PsiDefaultPropertySetterSymbol -> null
        is KtFe10DescDefaultPropertySetterSymbol -> null
        is KtFe10FileSymbol -> null
        is KtFe10DescDefaultPropertySetterSymbol.DefaultKtValueParameterSymbol -> descriptor
        is KtFe10PsiDefaultPropertySetterSymbol.DefaultKtValueParameterSymbol -> descriptor
        is KtFe10DescDefaultBackingFieldSymbol, is KtFe10PsiDefaultBackingFieldSymbol -> null
        is KtFe10PsiClassInitializerSymbol -> null
        else -> unexpectedElementError("KtSymbol", this)
    }
}


internal fun ConstructorDescriptor.toKtConstructorSymbol(analysisContext: Fe10AnalysisContext): KtConstructorSymbol {
    if (this is TypeAliasConstructorDescriptor) {
        return this.underlyingConstructorDescriptor.toKtConstructorSymbol(analysisContext)
    }

    return KtFe10DescConstructorSymbol(this, analysisContext)
}

internal val CallableMemberDescriptor.ktHasStableParameterNames: Boolean
    get() = when {
        this is ConstructorDescriptor && isPrimary && constructedClass.kind == ClassKind.ANNOTATION_CLASS -> true
        isExpect -> false
        else -> when (this) {
            is JavaCallableMemberDescriptor -> false
            else -> hasStableParameterNames()
        }
    }

internal fun CallableDescriptor.toKtCallableSymbol(analysisContext: Fe10AnalysisContext): KtCallableSymbol? {
    return when (val unwrapped = unwrapFakeOverrideIfNeeded()) {
        is PropertyGetterDescriptor -> KtFe10DescPropertyGetterSymbol(unwrapped, analysisContext)
        is PropertySetterDescriptor -> KtFe10DescPropertySetterSymbol(unwrapped, analysisContext)
        is SamConstructorDescriptor -> KtFe10DescSamConstructorSymbol(unwrapped, analysisContext)
        is ConstructorDescriptor -> unwrapped.toKtConstructorSymbol(analysisContext)
        is FunctionDescriptor -> {
            if (DescriptorUtils.isAnonymousFunction(unwrapped)) {
                KtFe10DescAnonymousFunctionSymbol(unwrapped, analysisContext)
            } else {
                KtFe10DescFunctionSymbol.build(unwrapped, analysisContext)
            }
        }
        is SyntheticFieldDescriptor -> KtFe10DescSyntheticFieldSymbol(unwrapped, analysisContext)
        is LocalVariableDescriptor -> KtFe10DescLocalVariableSymbol(unwrapped, analysisContext)
        is ValueParameterDescriptor -> KtFe10DescValueParameterSymbol(unwrapped, analysisContext)
        is SyntheticJavaPropertyDescriptor -> KtFe10DescSyntheticJavaPropertySymbol(unwrapped, analysisContext)
        is JavaForKotlinOverridePropertyDescriptor -> KtFe10DescSyntheticJavaPropertySymbolForOverride(unwrapped, analysisContext)
        is JavaPropertyDescriptor -> KtFe10DescJavaFieldSymbol(unwrapped, analysisContext)
        is PropertyDescriptorImpl -> KtFe10DescKotlinPropertySymbol(unwrapped, analysisContext)
        else -> null
    }
}

/**
 * This logic should be equivalent to
 * [org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder.unwrapSubstitutionOverrideIfNeeded]. But this method unwrap all fake
 * overrides that do not change the signature.
 */
internal fun CallableDescriptor.unwrapFakeOverrideIfNeeded(): CallableDescriptor {
    val useSiteUnwrapped = unwrapUseSiteSubstitutionOverride()
    if (useSiteUnwrapped !is CallableMemberDescriptor) return useSiteUnwrapped
    if (useSiteUnwrapped.kind.isReal) return useSiteUnwrapped
    val overriddenDescriptor = useSiteUnwrapped.overriddenDescriptors.singleOrNull()?.unwrapUseSiteSubstitutionOverride()
        ?: return useSiteUnwrapped
    if (hasTypeReferenceAffectingSignature(useSiteUnwrapped, overriddenDescriptor)) {
        return useSiteUnwrapped
    }
    return overriddenDescriptor.unwrapFakeOverrideIfNeeded()
}

private fun hasTypeReferenceAffectingSignature(
    descriptor: CallableMemberDescriptor,
    overriddenDescriptor: CallableMemberDescriptor
): Boolean {
    val containingClass = (descriptor.containingDeclaration as? ClassifierDescriptorWithTypeParameters)
    val typeParametersFromOuterClass = buildList { containingClass?.let { collectTypeParameters(it) } }
    val allowedTypeParameters = (overriddenDescriptor.typeParameters + typeParametersFromOuterClass).toSet()
    return overriddenDescriptor.returnType?.hasReferenceOtherThan(allowedTypeParameters) == true ||
            overriddenDescriptor.extensionReceiverParameter?.type?.hasReferenceOtherThan(allowedTypeParameters) == true ||
            overriddenDescriptor.valueParameters.any { it.type.hasReferenceOtherThan(allowedTypeParameters) }
}

private fun MutableList<TypeParameterDescriptor>.collectTypeParameters(innerClass: ClassifierDescriptorWithTypeParameters) {
    if (!innerClass.isInner) return
    val outerClass = innerClass.containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: return
    addAll(outerClass.declaredTypeParameters)
    collectTypeParameters(outerClass)
}

private fun KotlinType.hasReferenceOtherThan(allowedTypeParameterDescriptors: Set<TypeParameterDescriptor>): Boolean {
    return when (this) {
        is SimpleType -> {
            val declarationDescriptor = constructor.declarationDescriptor
            if (declarationDescriptor !is AbstractTypeParameterDescriptor) return false
            declarationDescriptor !in allowedTypeParameterDescriptors ||
                    declarationDescriptor.upperBounds.any { it.hasReferenceOtherThan(allowedTypeParameterDescriptors) }
        }
        else -> arguments.any { typeProjection ->
            // A star projection type (lazily) built by type parameter will be yet another type with a star projection,
            // resulting in stack overflow if we keep checking allowed type parameter descriptors
            !typeProjection.isStarProjection &&
                    typeProjection.type.hasReferenceOtherThan(allowedTypeParameterDescriptors)
        }
    }
}

/**
 * Use-site substitution override are tracked through [CallableDescriptor.getOriginal]. Note that overridden symbols are accessed through
 * [CallableDescriptor.getOverriddenDescriptors] instead, which is separate from [CallableDescriptor.getOriginal].
 */
@Suppress("UNCHECKED_CAST")
private fun <T : CallableDescriptor> T.unwrapUseSiteSubstitutionOverride(): T {
    var current: CallableDescriptor = this
    while (original != current) {
        current = current.original
    }
    return current as T
}

internal fun KotlinType.toKtType(analysisContext: Fe10AnalysisContext): KtType {
    return when (val unwrappedType = unwrap()) {
        is DynamicType -> KtFe10DynamicType(unwrappedType, analysisContext)
        is FlexibleType -> KtFe10FlexibleType(unwrappedType, analysisContext)
        is DefinitelyNotNullType -> KtFe10DefinitelyNotNullType(unwrappedType, analysisContext)
        is ErrorType -> {
            if (unwrappedType.kind.isUnresolved)
                KtFe10ClassErrorType(unwrappedType, analysisContext)
            else
                KtFe10TypeErrorType(unwrappedType, analysisContext)
        }
        is CapturedType -> KtFe10CapturedType(unwrappedType, analysisContext)
        is NewCapturedType -> KtFe10NewCapturedType(unwrappedType, analysisContext)
        is SimpleType -> {
            val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(unwrappedType)
            if (typeParameterDescriptor != null) {
                return KtFe10TypeParameterType(unwrappedType, typeParameterDescriptor, analysisContext)
            }

            val typeConstructor = unwrappedType.constructor

            if (typeConstructor is NewTypeVariableConstructor) {
                val newTypeParameterDescriptor = typeConstructor.originalTypeParameter
                return if (newTypeParameterDescriptor != null) {
                    KtFe10TypeParameterType(unwrappedType, newTypeParameterDescriptor, analysisContext)
                } else {
                    KtFe10ClassErrorType(ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_PARAMETER_TYPE), analysisContext)
                }
            }

            if (typeConstructor is IntersectionTypeConstructor) {
                return KtFe10IntersectionType(unwrappedType, typeConstructor.supertypes, analysisContext)
            }

            return when (val typeDeclaration = typeConstructor.declarationDescriptor) {
                is FunctionClassDescriptor -> KtFe10FunctionalType(unwrappedType, typeDeclaration, analysisContext)
                is ClassDescriptor -> KtFe10UsualClassType(unwrappedType, typeDeclaration, analysisContext)
                else -> {
                    val errorType =
                        ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, typeConstructor, typeDeclaration.toString())
                    KtFe10ClassErrorType(errorType, analysisContext)
                }
            }

        }
        else -> error("Unexpected type $this")
    }
}

internal fun TypeProjection.toKtTypeProjection(analysisContext: Fe10AnalysisContext): KtTypeProjection {
    return if (isStarProjection) {
        KtStarTypeProjection(analysisContext.token)
    } else {
        KtTypeArgumentWithVariance(type.toKtType(analysisContext), this.projectionKind, analysisContext.token)
    }
}

internal fun TypeParameterDescriptor.toKtTypeParameter(analysisContext: Fe10AnalysisContext): KtTypeParameterSymbol {
    return KtFe10DescTypeParameterSymbol(this, analysisContext)
}

internal fun DeclarationDescriptor.getSymbolOrigin(analysisContext: Fe10AnalysisContext): KtSymbolOrigin {
    when (this) {
        is SyntheticJavaPropertyDescriptor -> return KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY
        is SyntheticFieldDescriptor -> return KtSymbolOrigin.PROPERTY_BACKING_FIELD
        is SamConstructorDescriptor -> return KtSymbolOrigin.SAM_CONSTRUCTOR
        is JavaClassDescriptor, is JavaCallableMemberDescriptor -> return KtSymbolOrigin.JAVA
        is DeserializedDescriptor -> return KtSymbolOrigin.LIBRARY
        is EnumEntrySyntheticClassDescriptor -> return containingDeclaration.getSymbolOrigin(analysisContext)
        is CallableMemberDescriptor -> when (kind) {
            CallableMemberDescriptor.Kind.DELEGATION -> return KtSymbolOrigin.DELEGATED
            CallableMemberDescriptor.Kind.SYNTHESIZED -> return KtSymbolOrigin.SOURCE_MEMBER_GENERATED
            else -> {}
        }
    }

    val sourceElement = this.toSourceElement
    if (sourceElement is JavaSourceElement) {
        return KtSymbolOrigin.JAVA
    }

    val psi = sourceElement.getPsi()
    if (psi != null) {
        if (psi.language != KotlinLanguage.INSTANCE) {
            return KtSymbolOrigin.JAVA
        }

        val virtualFile = psi.containingFile.virtualFile
        return analysisContext.getOrigin(virtualFile)
    } else { // psi == null
        // Implicit lambda parameter
        if (this is ValueParameterDescriptor && this.name == StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME) {
            return KtSymbolOrigin.SOURCE_MEMBER_GENERATED
        }
    }

    return KtSymbolOrigin.SOURCE
}

internal val KotlinType.ktNullability: KtTypeNullability
    get() = when {
        this.isNullabilityFlexible() -> KtTypeNullability.UNKNOWN
        this.isMarkedNullable -> KtTypeNullability.NULLABLE
        else -> KtTypeNullability.NON_NULLABLE
    }

internal val DeclarationDescriptorWithVisibility.ktVisibility: Visibility
    get() = when (visibility) {
        DescriptorVisibilities.PUBLIC -> Visibilities.Public
        DescriptorVisibilities.PROTECTED -> Visibilities.Protected
        DescriptorVisibilities.INTERNAL -> Visibilities.Internal
        DescriptorVisibilities.PRIVATE -> Visibilities.Private
        DescriptorVisibilities.PRIVATE_TO_THIS -> Visibilities.PrivateToThis
        DescriptorVisibilities.LOCAL -> Visibilities.Local
        DescriptorVisibilities.INVISIBLE_FAKE -> Visibilities.InvisibleFake
        DescriptorVisibilities.INHERITED -> Visibilities.Inherited
        else -> Visibilities.Unknown
    }

internal val MemberDescriptor.ktModality: Modality
    get() {
        val selfModality = this.modality

        if (selfModality == Modality.OPEN) {
            val containingDeclaration = this.containingDeclaration
            if (containingDeclaration is ClassDescriptor && containingDeclaration.modality == Modality.FINAL) {
                if (this !is CallableMemberDescriptor || dispatchReceiverParameter != null) {
                    // Non-static open callables in final class are counted as final (to match FIR)
                    return Modality.FINAL
                }
            }
        }

        return this.modality
    }

internal fun ConstantValue<*>.toKtConstantValue(): KtConstantValue {
    return when (this) {
        is ErrorValue.ErrorValueWithMessage -> KtConstantValue.KtErrorConstantValue(message, sourcePsi = null)
        is BooleanValue -> KtConstantValue.KtBooleanConstantValue(value, sourcePsi = null)
        is DoubleValue -> KtConstantValue.KtDoubleConstantValue(value, sourcePsi = null)
        is FloatValue -> KtConstantValue.KtFloatConstantValue(value, sourcePsi = null)
        is NullValue -> KtConstantValue.KtNullConstantValue(sourcePsi = null)
        is StringValue -> KtConstantValue.KtStringConstantValue(value, sourcePsi = null)
        is ByteValue -> KtConstantValue.KtByteConstantValue(value, sourcePsi = null)
        is CharValue -> KtConstantValue.KtCharConstantValue(value, sourcePsi = null)
        is IntValue -> KtConstantValue.KtIntConstantValue(value, sourcePsi = null)
        is LongValue -> KtConstantValue.KtLongConstantValue(value, sourcePsi = null)
        is ShortValue -> KtConstantValue.KtShortConstantValue(value, sourcePsi = null)
        is UByteValue -> KtConstantValue.KtUnsignedByteConstantValue(value.toUByte(), sourcePsi = null)
        is UIntValue -> KtConstantValue.KtUnsignedIntConstantValue(value.toUInt(), sourcePsi = null)
        is ULongValue -> KtConstantValue.KtUnsignedLongConstantValue(value.toULong(), sourcePsi = null)
        is UShortValue -> KtConstantValue.KtUnsignedShortConstantValue(value.toUShort(), sourcePsi = null)
        else -> error("Unexpected constant value $value")
    }
}

internal tailrec fun KotlinBuiltIns.areSameArrayTypeIgnoringProjections(left: KotlinType, right: KotlinType): Boolean {
    val leftIsArray = KotlinBuiltIns.isArrayOrPrimitiveArray(left)
    val rightIsArray = KotlinBuiltIns.isArrayOrPrimitiveArray(right)

    return when {
        leftIsArray && rightIsArray -> areSameArrayTypeIgnoringProjections(getArrayElementType(left), getArrayElementType(right))
        !leftIsArray && !rightIsArray -> left == right
        else -> false
    }
}


internal fun List<ConstantValue<*>>.expandArrayAnnotationValue(
    containingArrayType: KotlinType,
    analysisContext: Fe10AnalysisContext,
): List<KtAnnotationValue> = flatMap { constantValue: ConstantValue<*> ->
    val constantType = constantValue.getType(analysisContext.resolveSession.moduleDescriptor)
    if (analysisContext.builtIns.areSameArrayTypeIgnoringProjections(containingArrayType, constantType)) {
        // If an element in the array has the same type as the containing array, it's a spread component that needs
        // to be expanded here. (It should have the array element type instead.)
        (constantValue as ArrayValue).value.expandArrayAnnotationValue(containingArrayType, analysisContext)
    } else {
        listOf(constantValue.toKtAnnotationValue(analysisContext))
    }
}

internal fun ConstantValue<*>.toKtAnnotationValue(analysisContext: Fe10AnalysisContext): KtAnnotationValue {
    return when (this) {
        is ArrayValue -> {
            val arrayType = getType(analysisContext.resolveSession.moduleDescriptor)
            KtArrayAnnotationValue(value.expandArrayAnnotationValue(arrayType, analysisContext), sourcePsi = null)
        }
        is EnumValue -> KtEnumEntryAnnotationValue(CallableId(enumClassId, enumEntryName), sourcePsi = null)
        is KClassValue -> when (val value = value) {
            is KClassValue.Value.LocalClass -> {
                val descriptor = value.type.constructor.declarationDescriptor as ClassDescriptor
                KtKClassAnnotationValue.KtLocalKClassAnnotationValue(descriptor.source.getPsi() as KtClassOrObject, sourcePsi = null)
            }
            is KClassValue.Value.NormalClass -> KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue(value.classId, sourcePsi = null)
        }

        is AnnotationValue -> {
            KtAnnotationApplicationValue(
                KtAnnotationApplicationWithArgumentsInfo(
                    value.annotationClass?.classId,
                    psi = null,
                    useSiteTarget = null,
                    arguments = value.getKtNamedAnnotationArguments(analysisContext),
                    index = null,
                )
            )
        }
        else -> {
            KtConstantAnnotationValue(toKtConstantValue())
        }
    }
}

internal val CallableMemberDescriptor.callableIdIfNotLocal: CallableId?
    get() = calculateCallableId(allowLocal = false)

internal fun CallableMemberDescriptor.calculateCallableId(allowLocal: Boolean): CallableId? {
    if (this is SyntheticJavaPropertyDescriptor) {
        return getMethod.calculateCallableId(allowLocal)?.copy(callableName = name)
    }
    var current: DeclarationDescriptor = containingDeclaration

    val localName = mutableListOf<String>()
    val className = mutableListOf<String>()

    while (true) {
        when (current) {
            is PackageFragmentDescriptor -> {
                return CallableId(
                    packageName = current.fqName,
                    className = if (className.isNotEmpty()) FqName.fromSegments(className.asReversed()) else null,
                    callableName = name,
                    pathToLocal = if (localName.isNotEmpty()) FqName.fromSegments(localName.asReversed()) else null
                )
            }
            is ModuleDescriptor -> {
                return CallableId(
                    packageName = FqName.ROOT,
                    className = if (className.isNotEmpty()) FqName.fromSegments(className.asReversed()) else null,
                    callableName = name,
                    pathToLocal = if (localName.isNotEmpty()) FqName.fromSegments(localName.asReversed()) else null
                )
            }
            is ClassDescriptor -> {
                if (current.kind == ClassKind.ENUM_ENTRY) {
                    if (!allowLocal) {
                        return null
                    }

                    localName += current.name.asString()
                } else {
                    className += current.name.asString()
                }
            }
            is PropertyAccessorDescriptor -> {} // Filter out property accessors
            is CallableDescriptor -> {
                if (!allowLocal) {
                    return null
                }

                localName += current.name.asString()
            }
        }

        current = current.containingDeclaration ?: return null
    }
}

internal val PropertyDescriptor.getterCallableIdIfNotLocal: CallableId?
    get() {
        if (this is SyntheticPropertyDescriptor) {
            return getMethod.callableIdIfNotLocal
        }

        return null
    }

internal val PropertyDescriptor.setterCallableIdIfNotLocal: CallableId?
    get() {
        if (this is SyntheticPropertyDescriptor) {
            val setMethod = this.setMethod
            if (setMethod != null) {
                return setMethod.callableIdIfNotLocal
            }
        }

        return null
    }

internal fun getSymbolDescriptor(symbol: KtSymbol): DeclarationDescriptor? {
    return when (symbol) {
        is KtFe10DescSymbol<*> -> symbol.descriptor
        is KtFe10PsiSymbol<*, *> -> symbol.descriptor
        is KtFe10DescSyntheticFieldSymbol -> symbol.descriptor
        else -> null
    }
}

internal val ClassifierDescriptor.classId: ClassId?
    get() = when (val owner = containingDeclaration) {
        is PackageFragmentDescriptor -> ClassId(owner.fqName, name)
        is ClassifierDescriptorWithTypeParameters -> owner.classId?.createNestedClassId(name)
        else -> null
    }

internal val ClassifierDescriptor.maybeLocalClassId: ClassId
    get() = classId ?: ClassId(containingPackage() ?: FqName.ROOT, FqName.topLevel(this.name), isLocal = true)

internal fun ClassDescriptor.getSupertypesWithAny(): Collection<KotlinType> {
    val supertypes = typeConstructor.supertypes
    if (isInterfaceLike) {
        return supertypes
    }

    val hasClassSupertype = supertypes.any { (it.constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.CLASS }
    return if (hasClassSupertype) supertypes else listOf(builtIns.anyType) + supertypes
}


internal fun CallableMemberDescriptor.getSymbolPointerSignature(): String {
    return DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)
}

internal fun createKtInitializerValue(
    initializer: KtExpression?,
    propertyDescriptor: PropertyDescriptor?,
    analysisContext: Fe10AnalysisContext,
): KtInitializerValue? {
    if (initializer == null && propertyDescriptor?.compileTimeInitializer == null) {
        return null
    }

    val compileTimeInitializer = propertyDescriptor?.compileTimeInitializer
    if (compileTimeInitializer != null) {
        return KtConstantInitializerValue(compileTimeInitializer.toKtConstantValue(), initializer)
    }
    if (initializer != null) {
        val bindingContext = analysisContext.analyze(initializer)
        val constantValue = ConstantExpressionEvaluator.getConstant(initializer, bindingContext)
        if (constantValue != null) {
            val evaluated = constantValue.toConstantValue(propertyDescriptor?.type ?: TypeUtils.NO_EXPECTED_TYPE).toKtConstantValue()
            return KtConstantInitializerValue(evaluated, initializer)
        }
    }

    return KtNonConstantInitializerValue(initializer)
}

internal fun AnnotationDescriptor.toKtAnnotationApplication(
    analysisContext: Fe10AnalysisContext,
    index: Int,
): KtAnnotationApplicationWithArgumentsInfo = KtAnnotationApplicationWithArgumentsInfo(
    classId = classIdForAnnotation,
    psi = psi,
    useSiteTarget = useSiteTarget,
    arguments = getKtNamedAnnotationArguments(analysisContext),
    index = index,
)

internal fun AnnotationDescriptor.toKtAnnotationInfo(index: Int): KtAnnotationApplicationInfo = KtAnnotationApplicationInfo(
    classId = classIdForAnnotation,
    psi = psi,
    useSiteTarget = useSiteTarget,
    isCallWithArguments = allValueArguments.isNotEmpty(),
    index = index,
)

private val AnnotationDescriptor.psi: KtCallElement? get() = (source as? PsiSourceElement)?.psi as? KtCallElement
internal val AnnotationDescriptor.classIdForAnnotation: ClassId? get() = annotationClass?.maybeLocalClassId
internal val AnnotationDescriptor.useSiteTarget: AnnotationUseSiteTarget?
    get() = (this as? LazyAnnotationDescriptor)?.annotationEntry?.useSiteTarget?.getAnnotationUseSiteTarget()

internal fun AnnotationDescriptor.getKtNamedAnnotationArguments(analysisContext: Fe10AnalysisContext): List<KtNamedAnnotationValue> =
    allValueArguments.map { (name, value) ->
        KtNamedAnnotationValue(name, value.toKtAnnotationValue(analysisContext))
    }

internal fun CallableDescriptor.createContextReceivers(
    analysisContext: Fe10AnalysisContext
): List<KtContextReceiver> {
    return contextReceiverParameters.map { createContextReceiver(it, analysisContext) }
}

internal fun ClassDescriptor.createContextReceivers(
    analysisContext: Fe10AnalysisContext
): List<KtContextReceiver> {
    return contextReceivers.map { createContextReceiver(it, analysisContext) }
}

private fun createContextReceiver(
    contextReceiver: ReceiverParameterDescriptor,
    analysisContext: Fe10AnalysisContext
): KtContextReceiverImpl {
    return KtContextReceiverImpl(
        contextReceiver.value.type.toKtType(analysisContext),
        (contextReceiver.value as ImplicitContextReceiver).customLabelName,
        analysisContext.token
    )
}
