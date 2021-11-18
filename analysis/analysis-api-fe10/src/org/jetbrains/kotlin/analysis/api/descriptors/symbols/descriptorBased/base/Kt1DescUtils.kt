/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base

import org.jetbrains.kotlin.analysis.api.KtStarProjectionTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedConstantValue
import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.types.*
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KtFe10Renderer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor

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
        is CallableDescriptor -> toKtCallableSymbol(analysisContext)
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
    return when (this) {
        is PropertyGetterDescriptor -> KtFe10DescPropertyGetterSymbol(this, analysisContext)
        is PropertySetterDescriptor -> KtFe10DescPropertySetterSymbol(this, analysisContext)
        is SamConstructorDescriptor -> KtFe10DescSamConstructorSymbol(this, analysisContext)
        is ConstructorDescriptor -> toKtConstructorSymbol(analysisContext)
        is FunctionDescriptor -> {
            if (DescriptorUtils.isAnonymousFunction(this)) {
                KtFe10DescAnonymousFunctionSymbol(this, analysisContext)
            } else {
                KtFe10DescFunctionSymbol(this, analysisContext)
            }
        }
        is SyntheticFieldDescriptor -> KtFe10DescSyntheticFieldSymbol(this, analysisContext)
        is LocalVariableDescriptor -> KtFe10DescLocalVariableSymbol(this, analysisContext)
        is ValueParameterDescriptor -> KtFe10DescValueParameterSymbol(this, analysisContext)
        is SyntheticJavaPropertyDescriptor -> KtFe10DescSyntheticJavaPropertySymbol(this, analysisContext)
        is JavaForKotlinOverridePropertyDescriptor -> KtFe10DescSyntheticJavaPropertySymbolForOverride(this, analysisContext)
        is JavaPropertyDescriptor -> KtFe10DescJavaFieldSymbol(this, analysisContext)
        is PropertyDescriptorImpl -> KtFe10DescKotlinPropertySymbol(this, analysisContext)
        else -> null
    }
}

internal fun KotlinType.toKtType(analysisContext: Fe10AnalysisContext): KtType {
    return when (val unwrappedType = unwrap()) {
        is FlexibleType -> KtFe10FlexibleType(unwrappedType, analysisContext)
        is DefinitelyNotNullType -> KtFe10DefinitelyNotNullType(unwrappedType, analysisContext)
        is ErrorType -> KtFe10ClassErrorType(unwrappedType, analysisContext)
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
                    KtFe10ClassErrorType(ErrorUtils.createErrorType("Unresolved type parameter type") as ErrorType, analysisContext)
                }
            }

            if (typeConstructor is IntersectionTypeConstructor) {
                return KtFe10IntersectionType(unwrappedType, typeConstructor.supertypes, analysisContext)
            }

            return when (val typeDeclaration = typeConstructor.declarationDescriptor) {
                is FunctionClassDescriptor -> KtFe10FunctionalType(unwrappedType, typeDeclaration, analysisContext)
                is ClassDescriptor -> KtFe10UsualClassType(unwrappedType, typeDeclaration, analysisContext)
                else -> {
                    val errorType = ErrorUtils.createErrorTypeWithCustomConstructor("Unresolved class type", typeConstructor)
                    KtFe10ClassErrorType(errorType as ErrorType, analysisContext)
                }
            }

        }
        else -> error("Unexpected type $this")
    }
}

internal fun TypeProjection.toKtTypeArgument(analysisContext: Fe10AnalysisContext): KtTypeArgument {
    return if (isStarProjection) {
        KtStarProjectionTypeArgument(analysisContext.token)
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
                return Modality.FINAL
            }
        }

        return this.modality
    }

internal fun ConstantValue<*>.toKtConstantValue(): KtConstantValue {
    return when (this) {
        is BooleanValue -> KtLiteralConstantValue(ConstantValueKind.Boolean, value, null)
        is CharValue -> KtLiteralConstantValue(ConstantValueKind.Char, value, null)
        is ByteValue -> KtLiteralConstantValue(ConstantValueKind.Byte, value, null)
        is UByteValue -> KtLiteralConstantValue(ConstantValueKind.UnsignedByte, value, null)
        is ShortValue -> KtLiteralConstantValue(ConstantValueKind.Short, value, null)
        is UShortValue -> KtLiteralConstantValue(ConstantValueKind.UnsignedShort, value, null)
        is IntValue -> KtLiteralConstantValue(ConstantValueKind.Int, value, null)
        is UIntValue -> KtLiteralConstantValue(ConstantValueKind.UnsignedInt, value, null)
        is LongValue -> KtLiteralConstantValue(ConstantValueKind.Long, value, null)
        is ULongValue -> KtLiteralConstantValue(ConstantValueKind.UnsignedLong, value, null)
        is FloatValue -> KtLiteralConstantValue(ConstantValueKind.Float, value, null)
        is DoubleValue -> KtLiteralConstantValue(ConstantValueKind.Double, value, null)
        is NullValue -> KtLiteralConstantValue(ConstantValueKind.Null, null, null)
        is StringValue -> KtLiteralConstantValue(ConstantValueKind.String, value, null)
        is ArrayValue -> KtArrayConstantValue(value.map { it.toKtConstantValue() }, null)
        is EnumValue -> KtEnumEntryConstantValue(CallableId(enumClassId, enumEntryName), null)
        is AnnotationValue -> {
            val arguments = value.allValueArguments.map { (name, v) -> KtNamedConstantValue(name.asString(), v.toKtConstantValue()) }
            KtAnnotationConstantValue(value.annotationClass?.classId, arguments, null)
        }
        is ErrorValue -> KtErrorValue(this.toString())
        else -> KtUnsupportedConstantValue
    }
}

internal val CallableMemberDescriptor.callableIdIfNotLocal: CallableId?
    get() = calculateCallableId(allowLocal = false)

internal fun CallableMemberDescriptor.calculateCallableId(allowLocal: Boolean): CallableId? {
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
    get() = classId ?: ClassId(containingPackage() ?: FqName.ROOT, FqName.topLevel(this.name), true)

internal fun ClassDescriptor.getSupertypesWithAny(): Collection<KotlinType> {
    val supertypes = typeConstructor.supertypes
    if (isInterfaceLike) {
        return supertypes
    }

    val hasClassSupertype = supertypes.any { (it.constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.CLASS }
    return if (hasClassSupertype) supertypes else listOf(builtIns.anyType) + supertypes
}

internal fun DeclarationDescriptor.render(analysisContext: Fe10AnalysisContext, options: KtDeclarationRendererOptions): String {
    val renderer = KtFe10Renderer(analysisContext, options)
    val consumer = StringBuilder()
    renderer.render(this, consumer)
    return consumer.toString().trim()
}

internal fun CallableMemberDescriptor.getSymbolPointerSignature(analysisContext: Fe10AnalysisContext): String {
    return render(analysisContext, KtDeclarationRendererOptions.DEFAULT)
}
