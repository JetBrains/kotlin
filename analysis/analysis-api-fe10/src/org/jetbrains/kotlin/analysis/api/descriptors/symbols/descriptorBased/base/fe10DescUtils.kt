/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base

import com.intellij.psi.impl.compiled.ClsElementImpl
import org.jetbrains.kotlin.analysis.api.KaConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.KaNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KaFe10FileSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KaFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.types.*
import org.jetbrains.kotlin.analysis.api.impl.base.*
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.*
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.asKaSymbolModality
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.symbol
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
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
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

internal val DeclarationDescriptor.kaSymbolLocation: KaSymbolLocation
    get() = when {
        isDynamic() -> KaSymbolLocation.CLASS
        this is PropertyAccessorDescriptor -> KaSymbolLocation.PROPERTY
        this is SamConstructorDescriptor -> KaSymbolLocation.TOP_LEVEL
        else -> when (containingDeclaration) {
            is PackageFragmentDescriptor -> KaSymbolLocation.TOP_LEVEL
            is ClassDescriptor -> KaSymbolLocation.CLASS
            else -> KaSymbolLocation.LOCAL
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

internal fun DeclarationDescriptor.toKtSymbol(analysisContext: Fe10AnalysisContext): KaSymbol? {
    if (this is ClassDescriptor && kind == ClassKind.ENUM_ENTRY) {
        return KaFe10DescEnumEntrySymbol(this, analysisContext)
    }

    return when (this) {
        is ClassifierDescriptor -> toKtClassifierSymbol(analysisContext)
        is ReceiverParameterDescriptor -> toKtReceiverParameterSymbol(analysisContext)
        is CallableDescriptor -> toKtCallableSymbol(analysisContext)
        is PackageViewDescriptor -> toKtPackageSymbol(analysisContext)
        else -> null
    }
}

internal fun ClassifierDescriptor.toKtClassifierSymbol(analysisContext: Fe10AnalysisContext): KaClassifierSymbol? {
    return when (this) {
        is TypeAliasDescriptor -> KaFe10DescTypeAliasSymbol(this, analysisContext)
        is TypeParameterDescriptor -> KaFe10DescTypeParameterSymbol(this, analysisContext)
        is ClassDescriptor -> toKaClassSymbol(analysisContext)
        else -> null
    }
}

internal fun ClassDescriptor.toKaClassSymbol(analysisContext: Fe10AnalysisContext): KaClassSymbol {
    return if (DescriptorUtils.isAnonymousObject(this)) {
        KaFe10DescAnonymousObjectSymbol(this, analysisContext)
    } else {
        KaFe10DescNamedClassSymbol(this, analysisContext)
    }
}

internal fun PackageViewDescriptor.toKtPackageSymbol(analysisContext: Fe10AnalysisContext): KaPackageSymbol {
    return KaFe10PackageSymbol(fqName, analysisContext)
}

internal fun ReceiverParameterDescriptor.toKtReceiverParameterSymbol(analysisContext: Fe10AnalysisContext): KaReceiverParameterSymbol {
    return KaFe10ReceiverParameterSymbol(this, analysisContext)
}

internal fun KaSymbol.getDescriptor(): DeclarationDescriptor? {
    return when (this) {
        is KaFe10PsiSymbol<*, *> -> descriptor
        is KaFe10DescSymbol<*> -> descriptor
        is KaFe10DescSyntheticFieldSymbol -> descriptor
        is KaFe10PsiDefaultPropertyGetterSymbol -> descriptor
        is KaFe10PsiDefaultPropertySetterSymbol -> descriptor
        is KaFe10PsiDefaultSetterParameterSymbol -> descriptor
        is KaFe10DescDefaultPropertySetterSymbol -> null
        is KaFe10DynamicFunctionDescValueParameterSymbol -> null
        is KaFe10FileSymbol -> null
        is KaFe10DescDefaultPropertySetterSymbol.KaDefaultValueParameterSymbol -> descriptor
        is KaFe10PsiDefaultPropertySetterSymbol.KaDefaultValueParameterSymbol -> descriptor
        is KaFe10DescDefaultBackingFieldSymbol, is KaFe10PsiDefaultBackingFieldSymbol -> null
        is KaFe10PsiClassInitializerSymbol -> null
        else -> unexpectedElementError("KtSymbol", this)
    }
}


internal fun ConstructorDescriptor.toKtConstructorSymbol(analysisContext: Fe10AnalysisContext): KaConstructorSymbol {
    if (this is TypeAliasConstructorDescriptor) {
        return this.underlyingConstructorDescriptor.toKtConstructorSymbol(analysisContext)
    }

    return KaFe10DescConstructorSymbol(this, analysisContext)
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

internal fun CallableDescriptor.toKtCallableSymbol(analysisContext: Fe10AnalysisContext): KaCallableSymbol? {
    return when (val unwrapped = unwrapFakeOverrideIfNeeded()) {
        is ImportedFromObjectCallableDescriptor<*> -> unwrapped.callableFromObject.toKtCallableSymbol(analysisContext)
        is PropertyGetterDescriptor -> KaFe10DescPropertyGetterSymbol(unwrapped, analysisContext)
        is PropertySetterDescriptor -> KaFe10DescPropertySetterSymbol(unwrapped, analysisContext)
        is SamConstructorDescriptor -> KaFe10DescSamConstructorSymbol(unwrapped, analysisContext)
        is ConstructorDescriptor -> unwrapped.toKtConstructorSymbol(analysisContext)
        is FunctionDescriptor -> {
            if (DescriptorUtils.isAnonymousFunction(unwrapped)) {
                KaFe10DescAnonymousFunctionSymbol(unwrapped, analysisContext)
            } else {
                KaFe10DescNamedFunctionSymbol.build(unwrapped, analysisContext)
            }
        }
        is SyntheticFieldDescriptor -> KaFe10DescSyntheticFieldSymbol(unwrapped, analysisContext)
        is LocalVariableDescriptor -> KaFe10DescLocalVariableSymbol(unwrapped, analysisContext)
        is ValueParameterDescriptor -> KaFe10DescValueParameterSymbol(unwrapped, analysisContext)
        is SyntheticJavaPropertyDescriptor -> KaFe10DescSyntheticJavaPropertySymbol(unwrapped, analysisContext)
        is JavaForKotlinOverridePropertyDescriptor -> KaFe10DescSyntheticJavaPropertySymbolForOverride(unwrapped, analysisContext)
        is JavaPropertyDescriptor -> KaFe10DescJavaFieldSymbol(unwrapped, analysisContext)
        is PropertyDescriptorImpl -> KaFe10DescKotlinPropertySymbol(unwrapped, analysisContext)
        else -> null
    }
}

/**
 * This logic should be equivalent to
 * [org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder.unwrapSubstitutionOverrideIfNeeded]. But this method unwrap all fake
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

internal fun KotlinType.toKtType(analysisContext: Fe10AnalysisContext): KaType {
    return when (val unwrappedType = unwrap()) {
        is DynamicType -> KaFe10DynamicType(unwrappedType, analysisContext)
        is FlexibleType -> KaFe10FlexibleType(unwrappedType, analysisContext)
        is DefinitelyNotNullType -> KaFe10DefinitelyNotNullType(unwrappedType, analysisContext)
        is ErrorType -> {
            if (unwrappedType.kind.isUnresolved)
                KaFe10ClassErrorType(unwrappedType, analysisContext)
            else
                KaFe10ErrorType(unwrappedType, analysisContext)
        }
        is CapturedType -> KaFe10CapturedType(unwrappedType, analysisContext)
        is NewCapturedType -> KaFe10NewCapturedType(unwrappedType, analysisContext)
        is SimpleType -> {
            val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(unwrappedType)
            if (typeParameterDescriptor != null) {
                return KaFe10TypeParameterType(unwrappedType, typeParameterDescriptor, analysisContext)
            }

            val typeConstructor = unwrappedType.constructor

            if (typeConstructor is NewTypeVariableConstructor) {
                val newTypeParameterDescriptor = typeConstructor.originalTypeParameter
                return if (newTypeParameterDescriptor != null) {
                    KaFe10TypeParameterType(unwrappedType, newTypeParameterDescriptor, analysisContext)
                } else {
                    KaFe10ClassErrorType(ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_PARAMETER_TYPE), analysisContext)
                }
            }

            if (typeConstructor is IntersectionTypeConstructor) {
                return KaFe10IntersectionType(unwrappedType, typeConstructor.supertypes, analysisContext)
            }

            return when (val typeDeclaration = typeConstructor.declarationDescriptor) {
                is FunctionClassDescriptor -> KaFe10FunctionType(unwrappedType, typeDeclaration, analysisContext)
                is ClassifierDescriptorWithTypeParameters -> KaFe10UsualClassType(unwrappedType, typeDeclaration, analysisContext)
                else -> {
                    val errorType =
                        ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, typeConstructor, typeDeclaration.toString())
                    KaFe10ClassErrorType(errorType, analysisContext)
                }
            }

        }
    }
}

internal fun TypeProjection.toKtTypeProjection(analysisContext: Fe10AnalysisContext): KaTypeProjection {
    return if (isStarProjection) {
        KaBaseStarTypeProjection(analysisContext.token)
    } else {
        KaBaseTypeArgumentWithVariance(type.toKtType(analysisContext), this.projectionKind, analysisContext.token)
    }
}

internal fun TypeParameterDescriptor.toKtTypeParameter(analysisContext: Fe10AnalysisContext): KaTypeParameterSymbol {
    return KaFe10DescTypeParameterSymbol(this, analysisContext)
}

internal fun DeclarationDescriptor.getSymbolOrigin(analysisContext: Fe10AnalysisContext): KaSymbolOrigin {
    when (this) {
        is SyntheticJavaPropertyDescriptor -> return KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY
        is SyntheticFieldDescriptor -> return KaSymbolOrigin.PROPERTY_BACKING_FIELD
        is SamConstructorDescriptor -> return KaSymbolOrigin.SAM_CONSTRUCTOR
        is JavaClassDescriptor, is JavaCallableMemberDescriptor -> return javaOrigin()
        is DeserializedDescriptor -> return KaSymbolOrigin.LIBRARY
        is EnumEntrySyntheticClassDescriptor -> return containingDeclaration.getSymbolOrigin(analysisContext)
        is CallableMemberDescriptor -> when (kind) {
            CallableMemberDescriptor.Kind.DELEGATION -> return KaSymbolOrigin.DELEGATED
            CallableMemberDescriptor.Kind.SYNTHESIZED -> return KaSymbolOrigin.SOURCE_MEMBER_GENERATED
            else -> {
                if (isDynamic()) {
                    return KaSymbolOrigin.JS_DYNAMIC
                }
            }
        }
    }

    val sourceElement = this.toSourceElement
    if (sourceElement is JavaSourceElement) {
        return javaOrigin()
    }

    val psi = sourceElement.getPsi()
    if (psi != null) {
        if (psi.language != KotlinLanguage.INSTANCE) {
            return javaOrigin()
        }

        val virtualFile = psi.containingFile.virtualFile
        return analysisContext.getOrigin(virtualFile)
    } else { // psi == null
        // Implicit lambda parameter
        if (this is ValueParameterDescriptor && this.name == StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME) {
            return KaSymbolOrigin.SOURCE_MEMBER_GENERATED
        }
    }

    return KaSymbolOrigin.SOURCE
}

private fun DeclarationDescriptor.javaOrigin(): KaSymbolOrigin {
    val psi = toSourceElement.getPsi()
    return if (psi == null || psi is ClsElementImpl) KaSymbolOrigin.JAVA_LIBRARY else KaSymbolOrigin.JAVA_SOURCE
}

@Suppress("DEPRECATION")
internal val KotlinType.ktNullability: org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
    get() = when {
        this.isNullabilityFlexible() -> org.jetbrains.kotlin.analysis.api.types.KaTypeNullability.UNKNOWN
        this.isMarkedNullable -> org.jetbrains.kotlin.analysis.api.types.KaTypeNullability.NULLABLE
        else -> org.jetbrains.kotlin.analysis.api.types.KaTypeNullability.NON_NULLABLE
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

internal val DeclarationDescriptor.kaSymbolModality: KaSymbolModality
    get() {
        if (this !is MemberDescriptor) return KaSymbolModality.FINAL

        val selfModality = this.modality

        if (selfModality == Modality.OPEN) {
            val containingDeclaration = this.containingDeclaration
            if (containingDeclaration is ClassDescriptor && containingDeclaration.modality == Modality.FINAL) {
                if (this !is CallableMemberDescriptor || dispatchReceiverParameter != null) {
                    // Non-static open callables in final class are counted as final (to match FIR)
                    return KaSymbolModality.FINAL
                }
            }
        }

        return this.modality.asKaSymbolModality
    }

internal fun ConstantValue<*>.toKtConstantValue(): KaConstantValue {
    return toKtConstantValueOrNull() ?: error("Unexpected constant value $value")
}

internal fun ConstantValue<*>.toKtConstantValueOrNull(): KaConstantValue? {
    return when (this) {
        is ErrorValue.ErrorValueWithMessage -> KaErrorConstantValueImpl(message, sourcePsi = null)
        is BooleanValue -> KaBooleanConstantValueImpl(value, sourcePsi = null)
        is DoubleValue -> KaDoubleConstantValueImpl(value, sourcePsi = null)
        is FloatValue -> KaFloatConstantValueImpl(value, sourcePsi = null)
        is NullValue -> KaNullConstantValueImpl(sourcePsi = null)
        is StringValue -> KaStringConstantValueImpl(value, sourcePsi = null)
        is ByteValue -> KaByteConstantValueImpl(value, sourcePsi = null)
        is CharValue -> KaCharConstantValueImpl(value, sourcePsi = null)
        is IntValue -> KaIntConstantValueImpl(value, sourcePsi = null)
        is LongValue -> KaLongConstantValueImpl(value, sourcePsi = null)
        is ShortValue -> KaShortConstantValueImpl(value, sourcePsi = null)
        is UByteValue -> KaUnsignedByteConstantValueImpl(value.toUByte(), sourcePsi = null)
        is UIntValue -> KaUnsignedIntConstantValueImpl(value.toUInt(), sourcePsi = null)
        is ULongValue -> KaUnsignedLongConstantValueImpl(value.toULong(), sourcePsi = null)
        is UShortValue -> KaUnsignedShortConstantValueImpl(value.toUShort(), sourcePsi = null)
        else -> null
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
): List<KaAnnotationValue> = flatMap { constantValue: ConstantValue<*> ->
    val constantType = constantValue.getType(analysisContext.resolveSession.moduleDescriptor)
    if (analysisContext.builtIns.areSameArrayTypeIgnoringProjections(containingArrayType, constantType)) {
        // If an element in the array has the same type as the containing array, it's a spread component that needs
        // to be expanded here. (It should have the array element type instead.)
        (constantValue as ArrayValue).value.expandArrayAnnotationValue(containingArrayType, analysisContext)
    } else {
        listOf(constantValue.toKaAnnotationValue(analysisContext))
    }
}

internal fun ConstantValue<*>.toKaAnnotationValue(analysisContext: Fe10AnalysisContext): KaAnnotationValue {
    val token = analysisContext.token

    return when (this) {
        is ArrayValue -> {
            val arrayType = getType(analysisContext.resolveSession.moduleDescriptor)
            KaArrayAnnotationValueImpl(value.expandArrayAnnotationValue(arrayType, analysisContext), sourcePsi = null, token)
        }
        is EnumValue -> KaEnumEntryAnnotationValueImpl(CallableId(enumClassId, enumEntryName), sourcePsi = null, token)
        is KClassValue -> when (val value = value) {
            is KClassValue.Value.LocalClass -> {
                val type = value.type.toKtType(analysisContext)
                val classId = value.type.unwrap().constructor.declarationDescriptor?.maybeLocalClassId
                KaClassLiteralAnnotationValueImpl(type, classId, sourcePsi = null, token)
            }
            is KClassValue.Value.NormalClass -> {
                val classLiteralInfo = resolveClassLiteral(value, analysisContext)

                if (classLiteralInfo != null) {
                    KaClassLiteralAnnotationValueImpl(classLiteralInfo.type, classLiteralInfo.classId, sourcePsi = null, token)
                } else {
                    val classId = if (value.arrayDimensions == 0) value.classId else StandardClassIds.Array

                    val type = ErrorUtils
                        .createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, classId.asFqNameString())
                        .toKtType(analysisContext)

                    KaClassLiteralAnnotationValueImpl(type, classId, sourcePsi = null, token)
                }
            }
        }

        is AnnotationValue -> {
            KaNestedAnnotationAnnotationValueImpl(
                KaAnnotationImpl(
                    value.annotationClass?.classId,
                    psi = null,
                    useSiteTarget = null,
                    lazyArguments = lazy { value.getKtNamedAnnotationArguments(analysisContext) },
                    constructorSymbol = null,
                    token = token
                ),
                token
            )
        }
        else -> {
            KaConstantAnnotationValueImpl(toKtConstantValue(), token)
        }
    }
}

private class ClassLiteralResolutionResult(val type: KaType, val classId: ClassId)

private fun resolveClassLiteral(value: KClassValue.Value.NormalClass, analysisContext: Fe10AnalysisContext): ClassLiteralResolutionResult? {
    var descriptor = analysisContext.resolveSession.moduleDescriptor.findClassifierAcrossModuleDependencies(value.classId)

    if (descriptor is TypeAliasDescriptor) {
        descriptor = descriptor.classDescriptor
    }

    if (descriptor !is ClassDescriptor) {
        return null
    }

    // Generic non-array class literals are not supported in K1
    val typeArguments = descriptor.typeConstructor.parameters.map { StarProjectionImpl(it) }

    var type: KotlinType = TypeUtils.substituteProjectionsForParameters(descriptor, typeArguments)
    var classId = value.classId

    if (value.arrayDimensions > 0) {
        val arrayDescriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(StandardClassIds.Array)
            ?: return null

        repeat(value.arrayDimensions) {
            type = TypeUtils.substituteParameters(arrayDescriptor, listOf(type))
        }

        classId = StandardClassIds.Array
    }

    return ClassLiteralResolutionResult(type.toKtType(analysisContext), classId)
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

internal fun getSymbolDescriptor(symbol: KaSymbol): DeclarationDescriptor? {
    return when (symbol) {
        is KaFe10DescSymbol<*> -> symbol.descriptor
        is KaFe10PsiSymbol<*, *> -> symbol.descriptor
        is KaFe10DescSyntheticFieldSymbol -> symbol.descriptor
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

internal fun ClassDescriptor.computeSymbolSupertypes(): Collection<KotlinType> {
    val classId = this.classId
    if (classId == StandardClassIds.Any || classId == StandardClassIds.Nothing) {
        return emptyList()
    }

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
): KaInitializerValue? {
    if (initializer == null && propertyDescriptor?.compileTimeInitializer == null) {
        return null
    }

    val compileTimeInitializer = propertyDescriptor?.compileTimeInitializer
    if (compileTimeInitializer != null) {
        return KaConstantInitializerValue(compileTimeInitializer.toKtConstantValue(), initializer)
    }
    if (initializer != null) {
        val bindingContext = analysisContext.analyze(initializer)
        val constantValue = ConstantExpressionEvaluator.getConstant(initializer, bindingContext)
        if (constantValue != null) {
            val evaluated = constantValue.toConstantValue(propertyDescriptor?.type ?: TypeUtils.NO_EXPECTED_TYPE).toKtConstantValue()
            return KaConstantInitializerValue(evaluated, initializer)
        }
    }

    return KaNonConstantInitializerValue(initializer)
}

internal fun AnnotationDescriptor.toKaAnnotation(analysisContext: Fe10AnalysisContext): KaAnnotation {
    return KaAnnotationImpl(
        classId = classIdForAnnotation,
        psi = psi,
        useSiteTarget = useSiteTarget,
        lazyArguments = lazy { getKtNamedAnnotationArguments(analysisContext) },
        constructorSymbol = null,
        token = analysisContext.token
    )
}

private val AnnotationDescriptor.psi: KtCallElement? get() = (source as? PsiSourceElement)?.psi as? KtCallElement
internal val AnnotationDescriptor.classIdForAnnotation: ClassId? get() = annotationClass?.maybeLocalClassId
internal val AnnotationDescriptor.useSiteTarget: AnnotationUseSiteTarget?
    get() = (this as? LazyAnnotationDescriptor)?.annotationEntry?.useSiteTarget?.getAnnotationUseSiteTarget()

internal fun AnnotationDescriptor.getKtNamedAnnotationArguments(analysisContext: Fe10AnalysisContext): List<KaNamedAnnotationValue> =
    allValueArguments.map { (name, value) ->
        KaBaseNamedAnnotationValue(name, value.toKaAnnotationValue(analysisContext))
    }

internal fun CallableDescriptor.createContextReceivers(
    analysisContext: Fe10AnalysisContext
): List<KaContextReceiver> {
    return contextReceiverParameters.map { createContextReceiver(it, analysisContext) }
}

internal fun CallableDescriptor.createContextParameters(analysisContext: Fe10AnalysisContext): List<KaContextParameterSymbol> {
    return contextReceiverParameters.map {
        KaFe10DescContextReceiverBasedContextParameterSymbol(it, analysisContext)
    }
}

internal fun ClassDescriptor.createContextReceivers(
    analysisContext: Fe10AnalysisContext
): List<KaContextReceiver> {
    return contextReceivers.map { createContextReceiver(it, analysisContext) }
}

private fun createContextReceiver(
    contextReceiver: ReceiverParameterDescriptor,
    analysisContext: Fe10AnalysisContext
): KaBaseContextReceiver {
    val type = contextReceiver.value.type.toKtType(analysisContext)
    return KaBaseContextReceiver(
        type,
        (contextReceiver.value as ImplicitContextReceiver).customLabelName ?: type.symbol?.name,
        analysisContext.token
    )
}
