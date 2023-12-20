/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.BuiltInAnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.AbstractTypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.SubstitutingScope
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.kotlin.resolve.unsubstitutedUnderlyingType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.types.typeUtil.isSignedOrUnsignedNumberType as classicIsSignedOrUnsignedNumberType
import org.jetbrains.kotlin.types.typeUtil.isStubType as isSimpleTypeStubType
import org.jetbrains.kotlin.types.typeUtil.isStubTypeForBuilderInference as isSimpleTypeStubTypeForBuilderInference
import org.jetbrains.kotlin.types.typeUtil.isStubTypeForVariableInSubtyping as isSimpleTypeStubTypeForVariableInSubtyping

interface ClassicTypeSystemContext : TypeSystemInferenceExtensionContext, TypeSystemCommonBackendContext {
    override fun TypeConstructorMarker.isDenotable(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this.isDenotable
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this is IntegerLiteralTypeConstructor
    }

    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean {
        return isIntegerLiteralTypeConstructor()
    }

    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean {
        return false
    }

    override fun TypeConstructorMarker.isLocalType(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor?.classId?.isLocal == true
    }

    override fun TypeConstructorMarker.isAnonymous(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor?.classId?.shortClassName == SpecialNames.ANONYMOUS
    }

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() {
            require(this is NewTypeVariableConstructor, this::errorMessage)
            return this.originalTypeParameter
        }

    override fun SimpleTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker> {
        val typeConstructor = typeConstructor()
        require(typeConstructor is IntegerLiteralTypeConstructor, this::errorMessage)
        return typeConstructor.possibleTypes
    }

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return this.makeNullableAsSpecified(nullable)
    }

    override fun KotlinTypeMarker.isError(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return this.isError
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        require(this is TypeConstructor && ErrorUtils.isError(declarationDescriptor), this::errorMessage)
        return ErrorUtils.createErrorType(ErrorTypeKind.RESOLUTION_ERROR_TYPE, "from type constructor $this")
    }

    override fun KotlinTypeMarker.isUninferredParameter(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return ErrorUtils.isUninferredTypeVariable(this)
    }

    override fun SimpleTypeMarker.isStubType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubType()
    }

    override fun SimpleTypeMarker.isStubTypeForVariableInSubtyping(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubTypeForVariableInSubtyping()
    }

    override fun SimpleTypeMarker.isStubTypeForBuilderInference(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubTypeForBuilderInference()
    }

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker {
        return this
    }

    override fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker {
        require(this is AbstractStubType, this::errorMessage)
        return this.originalTypeVariable as TypeVariableTypeConstructorMarker
    }

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? {
        // TODO: https://youtrack.jetbrains.com/issue/KT-54196 (old captured type here)
        require(this is NewCapturedType, this::errorMessage)
        return this.lowerType
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this is IntersectionTypeConstructor
    }

    override fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        require(a is SimpleType, a::errorMessage)
        require(b is SimpleType, b::errorMessage)
        return a.arguments === b.arguments
    }

    override fun KotlinTypeMarker.asSimpleType(): SimpleTypeMarker? {
        require(this is KotlinType, this::errorMessage)
        return this.unwrap() as? SimpleType
    }

    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? {
        require(this is KotlinType, this::errorMessage)
        return this.unwrap() as? FlexibleType
    }

    override fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker? {
        require(this is FlexibleType, this::errorMessage)
        return this as? DynamicType
    }

    override fun KotlinTypeMarker.isRawType(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return this is RawType
    }

    override fun KotlinTypeMarker.convertToNonRaw(): KotlinTypeMarker {
        error("Is not expected to be called in K1")
    }

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        require(this is FlexibleType, this::errorMessage)
        return this.upperBound
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        require(this is FlexibleType, this::errorMessage)
        return this.lowerBound
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? {
        require(this is SimpleType, this::errorMessage)
        return if (this is SimpleTypeWithEnhancement) origin.asCapturedType() else this as? NewCapturedType
    }

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? {
        require(this is SimpleType, this::errorMessage)
        return this as? DefinitelyNotNullType
    }

    @OptIn(ObsoleteTypeKind::class)
    override fun KotlinTypeMarker.isNotNullTypeParameter(): Boolean = this is NotNullTypeParameter

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isMarkedNullable
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker {
        require(this is SimpleType, this::errorMessage)
        return this.constructor
    }

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker {
        require(this is NewCapturedType, this::errorMessage)
        return this.constructor
    }

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker {
        require(this is NewCapturedTypeConstructor, this::errorMessage)
        return this.projection
    }

    override fun KotlinTypeMarker.argumentsCount(): Int {
        require(this is KotlinType, this::errorMessage)
        return this.arguments.size
    }

    override fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        require(this is KotlinType, this::errorMessage)
        return this.arguments[index]
    }

    override fun KotlinTypeMarker.getArguments(): List<TypeArgumentMarker> {
        require(this is KotlinType, this::errorMessage)
        return this.arguments
    }

    override fun TypeArgumentMarker.isStarProjection(): Boolean {
        require(this is TypeProjection, this::errorMessage)
        return this.isStarProjection
    }

    override fun TypeArgumentMarker.getVariance(): TypeVariance {
        require(this is TypeProjection, this::errorMessage)
        return this.projectionKind.convertVariance()
    }

    override fun TypeArgumentMarker.replaceType(newType: KotlinTypeMarker): TypeArgumentMarker {
        require(this is TypeProjection, this::errorMessage)
        require(newType is KotlinType, this::errorMessage)
        return this.replaceType(newType)
    }

    override fun TypeArgumentMarker.getType(): KotlinTypeMarker {
        require(this is TypeProjection, this::errorMessage)
        return this.type.unwrap()
    }


    override fun TypeConstructorMarker.parametersCount(): Int {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters.size
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters[index]
    }

    override fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters
    }

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        require(this is TypeConstructor, this::errorMessage)
        return this.supertypes
    }

    override fun TypeParameterMarker.getVariance(): TypeVariance {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.variance.convertVariance()
    }

    override fun TypeParameterMarker.upperBoundCount(): Int {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds.size
    }

    override fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds[index]
    }

    override fun TypeParameterMarker.getUpperBounds(): List<KotlinTypeMarker> {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.typeConstructor
    }

    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        require(this is TypeParameterDescriptor, this::errorMessage)
        require(selfConstructor is TypeConstructor?, this::errorMessage)

        return hasTypeParameterRecursiveBounds(this, selfConstructor)
    }

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        require(c1 is TypeConstructor, c1::errorMessage)
        require(c2 is TypeConstructor, c2::errorMessage)
        return c1 == c2
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor is ClassDescriptor
    }

    override fun TypeConstructorMarker.isInterface(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return DescriptorUtils.isInterface(declarationDescriptor)
    }

    override fun TypeConstructorMarker.isFinalClassConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        val classDescriptor = declarationDescriptor as? ClassDescriptor ?: return false
        return classDescriptor.isFinalClass
    }

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        val classDescriptor = declarationDescriptor as? ClassDescriptor ?: return false
        return classDescriptor.isFinalClass &&
                classDescriptor.kind != ClassKind.ENUM_ENTRY &&
                classDescriptor.kind != ClassKind.ANNOTATION_CLASS
    }

    override fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        val classDescriptor = declarationDescriptor
        return classDescriptor is ClassDescriptor && classDescriptor.isFinalClass
    }

    override fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker {
        require(this is SimpleType, this::errorMessage)
        return this
    }

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        require(type is SimpleType, type::errorMessage)
        return org.jetbrains.kotlin.types.checker.captureFromArguments(type, status)
    }

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.isTypeConstructorForGivenClass(this, FqNames.any)
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.isTypeConstructorForGivenClass(this, FqNames.nothing)
    }

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.isTypeConstructorForGivenClass(this, FqNames.array)
    }

    override fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is KotlinType, this::errorMessage)
        return this.asTypeProjection()
    }

    override fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.isTypeConstructorForGivenClass(this, FqNames.unit)
    }

    /**
     *
     * SingleClassifierType is one of the following types:
     *  - classType
     *  - type for type parameter
     *  - captured type
     *
     * Such types can contain error types in our arguments, but type constructor isn't errorTypeConstructor
     */
    override fun SimpleTypeMarker.isSingleClassifierType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return !isError &&
                constructor.declarationDescriptor !is TypeAliasDescriptor &&
                (constructor.declarationDescriptor != null || this is CapturedType || this is NewCapturedType || this is DefinitelyNotNullType || constructor is IntegerLiteralTypeConstructor || isSingleClassifierTypeWithEnhancement())
    }

    private fun SimpleTypeMarker.isSingleClassifierTypeWithEnhancement() =
        this is SimpleTypeWithEnhancement && origin.isSingleClassifierType()

    override fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean): Boolean {
        require(this is KotlinType, this::errorMessage)
        return containsInternal(this, predicate)
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        require(this is SimpleType, this::errorMessage)
        if (this is TypeUtils.SpecialType) return 0

        val maxInArguments = arguments.maxOfOrNull {
            if (it.isStarProjection) 1 else it.type.unwrap().typeDepth()
        } ?: 0

        return maxInArguments + 1
    }

    override fun intersectTypes(types: List<KotlinTypeMarker>): KotlinTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return org.jetbrains.kotlin.types.checker.intersectTypes(types as List<UnwrappedType>)
    }

    override fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return org.jetbrains.kotlin.types.checker.intersectTypes(types as List<SimpleType>)
    }

    override fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker? {
        @Suppress("UNCHECKED_CAST")
        return singleBestRepresentative(this as Collection<KotlinType>)
    }

    override fun KotlinTypeMarker.isUnit(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return KotlinBuiltIns.isUnit(this)
    }

    override fun KotlinTypeMarker.isBuiltinFunctionTypeOrSubtype(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return isBuiltinFunctionalTypeOrSubtype
    }

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound is SimpleType, this::errorMessage)
        require(upperBound is SimpleType, this::errorMessage)
        return KotlinTypeFactory.flexibleType(lowerBound, upperBound)
    }

    override fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker {
        return when (this) {
            is SimpleTypeMarker -> this.withNullability(nullable)
            is FlexibleTypeMarker -> createFlexibleType(lowerBound().withNullability(nullable), upperBound().withNullability(nullable))
            else -> error("sealed")
        }
    }


    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState {
        return createClassicTypeCheckerState(errorTypesEqualToAnything, stubTypesEqualToAnything, typeSystemContext = this)
    }

    override fun nullableNothingType(): SimpleTypeMarker {
        return builtIns.nullableNothingType
    }

    override fun nullableAnyType(): SimpleTypeMarker {
        return builtIns.nullableAnyType
    }

    override fun nothingType(): SimpleTypeMarker {
        return builtIns.nothingType
    }

    override fun anyType(): SimpleTypeMarker {
        return builtIns.anyType
    }

    val builtIns: KotlinBuiltIns
        get() = throw UnsupportedOperationException("Not supported")

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker {
        require(this is UnwrappedType, this::errorMessage)
        return makeDefinitelyNotNullOrNotNullInternal(this)
    }


    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return makeSimpleTypeDefinitelyNotNullOrNotNullInternal(this)
    }

    override fun KotlinTypeMarker.removeAnnotations(): KotlinTypeMarker {
        require(this is UnwrappedType, this::errorMessage)
        return this.replaceAnnotations(Annotations.EMPTY)
    }

    override fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker {
        require(this is UnwrappedType, this::errorMessage)
        val annotationsWithoutExact = this.annotations.filterNot(AnnotationDescriptor::isExactAnnotation)
        return this.replaceAnnotations(Annotations.create(annotationsWithoutExact))
    }

    override fun KotlinTypeMarker.hasExactAnnotation(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return hasExactInternal(this)
    }

    override fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return hasNoInferInternal(this)
    }

    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker {
        return when (this) {
            is NewCapturedType -> this.constructor.projection
            is CapturedType -> this.typeProjection
            else -> error("Unsupported captured type")
        }
    }

    override fun CapturedTypeMarker.withNotNullProjection(): KotlinTypeMarker {
        require(this is NewCapturedType, this::errorMessage)

        return NewCapturedType(captureStatus, constructor, lowerType, attributes, isMarkedNullable, isProjectionNotNull = true)
    }

    override fun CapturedTypeMarker.isProjectionNotNull(): Boolean {
        require(this is NewCapturedType, this::errorMessage)
        return this.isProjectionNotNull
    }

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        require(this is NewCapturedType, this::errorMessage)
        return this.constructor.typeParameter
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is NewCapturedType, this::errorMessage)
        return this.captureStatus
    }

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean = this is CapturedType

    override fun CapturedTypeMarker.hasRawSuperType(): Boolean {
        error("Is not expected to be called in K1")
    }

    override fun KotlinTypeMarker.isNullableType(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return TypeUtils.isNullableType(this)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean,
        attributes: List<AnnotationMarker>?
    ): SimpleTypeMarker {
        require(constructor is TypeConstructor, constructor::errorMessage)

        val ourAnnotations = attributes?.firstIsInstanceOrNull<AnnotationsTypeAttribute>()?.annotations?.toList()

        fun createExtensionFunctionAnnotation() = BuiltInAnnotationDescriptor(builtIns, FqNames.extensionFunctionType, emptyMap())

        val resultingAnnotations = when {
            ourAnnotations.isNullOrEmpty() && isExtensionFunction -> Annotations.create(listOf(createExtensionFunctionAnnotation()))
            !ourAnnotations.isNullOrEmpty() && !isExtensionFunction -> Annotations.create(ourAnnotations.filter { it.fqName != FqNames.extensionFunctionType })
            !ourAnnotations.isNullOrEmpty() && isExtensionFunction -> Annotations.create(ourAnnotations + createExtensionFunctionAnnotation())
            else -> Annotations.EMPTY
        }

        @Suppress("UNCHECKED_CAST")
        return KotlinTypeFactory.simpleType(
            DefaultTypeAttributeTranslator.toAttributes(resultingAnnotations),
            constructor,
            arguments as List<TypeProjection>,
            nullable
        )
    }

    override fun createTypeArgument(type: KotlinTypeMarker, variance: TypeVariance): TypeArgumentMarker {
        require(type is KotlinType, type::errorMessage)
        return TypeProjectionImpl(variance.convertVariance(), type)
    }

    override fun createStarProjection(typeParameter: TypeParameterMarker): TypeArgumentMarker {
        require(typeParameter is TypeParameterDescriptor, typeParameter::errorMessage)
        return StarProjectionImpl(typeParameter)
    }

    override fun KotlinTypeMarker.canHaveUndefinedNullability(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return constructor is NewTypeVariableConstructor ||
                constructor.declarationDescriptor is TypeParameterDescriptor ||
                this is NewCapturedType
    }

    override fun SimpleTypeMarker.isExtensionFunction(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.hasAnnotation(FqNames.extensionFunctionType)
    }

    override fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        @Suppress("UNCHECKED_CAST")
        return this.replace(newArguments as List<TypeProjection>)
    }

    override fun SimpleTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return this.replaceArgumentsByExistingArgumentsWith(replacement)
    }

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker {
        require(this is DefinitelyNotNullType, this::errorMessage)
        return this.original
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<KotlinTypeMarker>,
        lowerType: KotlinTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        errorSupportedOnlyInTypeInference()
    }

    @K2Only
    override fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker {
        error("Only for K2")
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        require(type is UnwrappedType, type::errorMessage)
        require(this is TypeSubstitutor, this::errorMessage)
        return safeSubstitute(type, Variance.INVARIANT)
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun KotlinTypeMarker.isSpecial(): Boolean {
        require(this is KotlinType)
        return this is TypeUtils.SpecialType
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        errorSupportedOnlyInTypeInference()
    }

    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
        errorSupportedOnlyInTypeInference()
    }

    override fun KotlinTypeMarker.isSignedOrUnsignedNumberType(): Boolean {
        require(this is KotlinType)
        return classicIsSignedOrUnsignedNumberType() || constructor is IntegerLiteralTypeConstructor
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
        @Suppress("UNCHECKED_CAST")
        explicitSupertypes as List<SimpleType>
        return IntegerLiteralTypeConstructor.findCommonSuperType(explicitSupertypes)
    }

    override fun unionTypeAttributes(types: List<KotlinTypeMarker>): List<AnnotationMarker> {
        @Suppress("UNCHECKED_CAST")
        return (types as List<KotlinType>).map { it.unwrap().attributes }.reduce { x, y -> x.union(y) }.toList()
    }

    override fun KotlinTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): KotlinTypeMarker {
        require(this is KotlinType)
        @Suppress("UNCHECKED_CAST")
        val attributes = (newAttributes as List<TypeAttribute<*>>).filterNot { it is AnnotationsTypeAttribute }.toMutableList()
        attributes.addIfNotNull(this.attributes.annotationsAttribute)
        return this.unwrap().replaceAttributes(
            TypeAttributes.create(attributes)
        )
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return ErrorUtils.isError(declarationDescriptor)
    }

    override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker {
        require(this is IntegerLiteralTypeConstructor, this::errorMessage)
        return this.getApproximatedType().unwrap()
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return KotlinBuiltIns.isPrimitiveType(this)
    }

    override fun KotlinTypeMarker.getAttributes(): List<AnnotationMarker> {
        require(this is KotlinType, this::errorMessage)
        return this.attributes.toList()
    }

    override fun KotlinTypeMarker.hasCustomAttributes(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return !this.attributes.isEmpty() && this.getCustomAttributes().size > 0
    }

    override fun KotlinTypeMarker.getCustomAttributes(): List<AnnotationMarker> {
        require(this is KotlinType, this::errorMessage)
        return this.attributes.filterNot { it is AnnotationsTypeAttribute }
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        return captureFromExpressionInternal(type as UnwrappedType)
    }

    override fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): SimpleTypeMarker {
        return ErrorUtils.createErrorType(ErrorTypeKind.RESOLUTION_ERROR_TYPE, debugName)
    }

    override fun createUninferredType(constructor: TypeConstructorMarker): KotlinTypeMarker {
        return ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_TYPE_VARIABLE, constructor as TypeConstructor, constructor.toString())
    }

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean {
        return this is NewCapturedTypeConstructor
    }

    override fun KotlinTypeMarker.eraseContainingTypeParameters(): KotlinTypeMarker {
        val eraser = TypeParameterUpperBoundEraser(
            ErasureProjectionComputer(),
            TypeParameterErasureOptions(leaveNonTypeParameterTypes = true, intersectUpperBounds = true)
        )
        val typeParameters = this.extractTypeParameters()
            .map { it as TypeParameterDescriptor }
            .associateWith {
                TypeProjectionImpl(Variance.OUT_VARIANCE, eraser.getErasedUpperBound(it, ErasureTypeAttributes(TypeUsage.COMMON)))
            }
        return TypeConstructorSubstitution.createByParametersMap(typeParameters).buildSubstitutor().safeSubstitute(this)
    }

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean {
        return this is ClassifierBasedTypeConstructor && this.declarationDescriptor is AbstractTypeParameterDescriptor
    }

    override fun arrayType(componentType: KotlinTypeMarker): SimpleTypeMarker {
        require(componentType is KotlinType, this::errorMessage)
        return builtIns.getArrayType(Variance.INVARIANT, componentType)
    }

    override fun KotlinTypeMarker.isArrayOrNullableArray(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return KotlinBuiltIns.isArray(this)
    }

    override fun KotlinTypeMarker.hasAnnotation(fqName: FqName): Boolean {
        require(this is KotlinType, this::errorMessage)
        return annotations.hasAnnotation(fqName)
    }

    override fun KotlinTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? {
        require(this is KotlinType, this::errorMessage)
        return annotations.findAnnotation(fqName)?.allValueArguments?.values?.firstOrNull()?.value
    }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor as? TypeParameterDescriptor
    }

    override fun TypeConstructorMarker.isInlineClass(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return (declarationDescriptor as? ClassDescriptor)?.valueClassRepresentation is InlineClassRepresentation
    }

    override fun TypeConstructorMarker.isMultiFieldValueClass(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return (declarationDescriptor as? ClassDescriptor)?.valueClassRepresentation is MultiFieldValueClassRepresentation
    }

    override fun TypeConstructorMarker.getValueClassProperties(): List<Pair<Name, SimpleTypeMarker>>? {
        require(this is TypeConstructor, this::errorMessage)
        return (declarationDescriptor as? ClassDescriptor)?.valueClassRepresentation?.underlyingPropertyNamesToTypes
    }

    override fun TypeConstructorMarker.isInnerClass(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return (declarationDescriptor as? ClassDescriptor)?.isInner == true
    }

    override fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return representativeUpperBound
    }

    override fun KotlinTypeMarker.getUnsubstitutedUnderlyingType(): KotlinTypeMarker? {
        require(this is KotlinType, this::errorMessage)
        return unsubstitutedUnderlyingType()
    }

    override fun KotlinTypeMarker.getSubstitutedUnderlyingType(): KotlinTypeMarker? {
        require(this is KotlinType, this::errorMessage)
        return substitutedUnderlyingType()
    }

    override fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType? {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.getPrimitiveType(declarationDescriptor as ClassDescriptor)
    }

    override fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType? {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.getPrimitiveArrayType(declarationDescriptor as ClassDescriptor)
    }

    override fun TypeConstructorMarker.isUnderKotlinPackage(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor?.let(KotlinBuiltIns::isUnderKotlinPackage) == true
    }

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe {
        require(this is TypeConstructor, this::errorMessage)
        return (declarationDescriptor as ClassDescriptor).fqNameUnsafe
    }

    override fun TypeParameterMarker.getName(): Name {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return name
    }

    override fun TypeParameterMarker.isReified(): Boolean {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return isReified
    }

    override fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        require(this is KotlinType, this::errorMessage)
        val descriptor = constructor.declarationDescriptor
        return descriptor is ClassDescriptor && (descriptor.kind == ClassKind.INTERFACE || descriptor.kind == ClassKind.ANNOTATION_CLASS)
    }

    override fun createTypeWithAlternativeForIntersectionResult(
        firstCandidate: KotlinTypeMarker,
        secondCandidate: KotlinTypeMarker,
    ): KotlinTypeMarker {
        require(firstCandidate is KotlinType, this::errorMessage)
        require(secondCandidate is KotlinType, this::errorMessage)

        (firstCandidate.constructor as? IntersectionTypeConstructor)?.let { intersectionConstructor ->
            val intersectionTypeWithAlternative = intersectionConstructor.setAlternative(secondCandidate).createType()
            return if (firstCandidate.isMarkedNullable) intersectionTypeWithAlternative.makeNullableAsSpecified(true)
            else intersectionTypeWithAlternative

        } ?: error("Expected intersection type, found $firstCandidate")
    }

    override fun KotlinTypeMarker.isFunctionOrKFunctionWithAnySuspendability(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return this.isFunctionOrKFunctionTypeWithAnySuspendability
    }

    override fun KotlinTypeMarker.isExtensionFunctionType(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return this.isBuiltinExtensionFunctionalType
    }

    override fun KotlinTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<KotlinTypeMarker> {
        require(this is KotlinType, this::errorMessage)
        return this.getPureArgumentsForFunctionalTypeOrSubtype()
    }

    override fun KotlinTypeMarker.getFunctionTypeFromSupertypes(): KotlinTypeMarker {
        require(this is KotlinType)
        return this.extractFunctionalTypeFromSupertypes()
    }

    override fun KotlinTypeMarker.functionTypeKind(): FunctionTypeKind? {
        require(this is KotlinType)
        return this.functionTypeKind
    }

    override fun getNonReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker {
        return getFunctionDescriptor(
            builtIns,
            parametersNumber,
            isSuspendFunction = kind.nonReflectKind() == FunctionTypeKind.SuspendFunction
        ).typeConstructor
    }

    override fun getReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker {
        return getKFunctionDescriptor(
            builtIns,
            parametersNumber,
            isSuspendFunction = kind.reflectKind() == FunctionTypeKind.KSuspendFunction
        ).typeConstructor
    }

    override fun createSubstitutorForSuperTypes(baseType: KotlinTypeMarker): TypeSubstitutorMarker? {
        require(baseType is KotlinType, baseType::errorMessage)
        return (baseType.memberScope as? SubstitutingScope)?.substitutor
    }

    override fun useRefinedBoundsForTypeVariableInFlexiblePosition(): Boolean = false

    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): TypeCheckerState.SupertypesPolicy {
        require(type is SimpleType, type::errorMessage)
        val substitutor = TypeConstructorSubstitution.create(type).buildSubstitutor()

        return object : TypeCheckerState.SupertypesPolicy.DoCustomTransform() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker): SimpleTypeMarker {
                return substitutor.safeSubstitute(
                    type.lowerBoundIfFlexible() as KotlinType,
                    Variance.INVARIANT
                ).asSimpleType()!!
            }
        }
    }

    override fun KotlinTypeMarker.isTypeVariableType(): Boolean {
        return this is UnwrappedType && constructor is NewTypeVariableConstructor
    }

    override val isK2: Boolean
        get() = false
}

fun TypeVariance.convertVariance(): Variance {
    return when (this) {
        TypeVariance.INV -> Variance.INVARIANT
        TypeVariance.IN -> Variance.IN_VARIANCE
        TypeVariance.OUT -> Variance.OUT_VARIANCE
    }
}

private fun captureFromExpressionInternal(type: UnwrappedType) = captureFromExpression(type)

private fun hasNoInferInternal(type: UnwrappedType): Boolean {
    return type.hasNoInferAnnotation()
}


private fun hasExactInternal(type: UnwrappedType): Boolean {
    return type.hasExactAnnotation()
}


private fun makeDefinitelyNotNullOrNotNullInternal(type: UnwrappedType): UnwrappedType {
    return type.makeDefinitelyNotNullOrNotNull()
}

private fun makeSimpleTypeDefinitelyNotNullOrNotNullInternal(type: SimpleType): SimpleType {
    return type.makeSimpleTypeDefinitelyNotNullOrNotNull()
}

private fun containsInternal(type: KotlinType, predicate: (KotlinTypeMarker) -> Boolean): Boolean = type.contains(predicate)

private fun singleBestRepresentative(collection: Collection<KotlinType>) = collection.singleBestRepresentative()

@Suppress("NOTHING_TO_INLINE")
private inline fun Any.errorMessage(): String {
    return "ClassicTypeSystemContext couldn't handle: $this, ${this::class}"
}

private fun errorSupportedOnlyInTypeInference(): Nothing {
    error("supported only in type inference context")
}
