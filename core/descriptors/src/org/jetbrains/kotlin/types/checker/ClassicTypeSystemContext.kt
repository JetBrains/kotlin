/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalTypeOrSubtype
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.BuiltInAnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.hasExactAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.hasNoInferAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.isExactAnnotation
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.types.typeUtil.isSignedOrUnsignedNumberType as classicIsSignedOrUnsignedNumberType

interface ClassicTypeSystemContext : TypeSystemInferenceExtensionContext, TypeSystemCommonBackendContext {
    override fun TypeConstructorMarker.isDenotable(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this.isDenotable
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this is IntegerLiteralTypeConstructor
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
        return ErrorUtils.createErrorType("from type constructor(${toString()})")
    }

    override fun KotlinTypeMarker.isUninferredParameter(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return ErrorUtils.isUninferredParameter(this)
    }

    override fun SimpleTypeMarker.isStubType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this is AbstractStubType
    }

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? {
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

    override fun FlexibleTypeMarker.asRawType(): RawTypeMarker? {
        require(this is FlexibleType, this::errorMessage)
        return this as? RawType
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
        return this as? NewCapturedType
    }

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? {
        require(this is SimpleType, this::errorMessage)
        return this as? DefinitelyNotNullType
    }

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

    override fun TypeArgumentMarker.isStarProjection(): Boolean {
        require(this is TypeProjection, this::errorMessage)
        return this.isStarProjection
    }

    override fun TypeArgumentMarker.getVariance(): TypeVariance {
        require(this is TypeProjection, this::errorMessage)
        return this.projectionKind.convertVariance()
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

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.typeConstructor
    }

    override fun isEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        require(c1 is TypeConstructor, c1::errorMessage)
        require(c2 is TypeConstructor, c2::errorMessage)
        return c1 == c2
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor is ClassDescriptor
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
     * Such types can contains error types in our arguments, but type constructor isn't errorTypeConstructor
     */
    override fun SimpleTypeMarker.isSingleClassifierType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return !isError &&
                constructor.declarationDescriptor !is TypeAliasDescriptor &&
                (constructor.declarationDescriptor != null || this is CapturedType || this is NewCapturedType || this is DefinitelyNotNullType || constructor is IntegerLiteralTypeConstructor)
    }

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

    override fun KotlinTypeMarker.isBuiltinFunctionalTypeOrSubtype(): Boolean {
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


    override fun newBaseTypeCheckerContext(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): AbstractTypeCheckerContext {
        return ClassicTypeCheckerContext(errorTypesEqualToAnything, stubTypesEqualToAnything)
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

    open val builtIns: KotlinBuiltIns
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
        require(this is NewCapturedType, this::errorMessage)
        return this.constructor.projection
    }

    override fun CapturedTypeMarker.withNotNullProjection(): KotlinTypeMarker {
        require(this is NewCapturedType, this::errorMessage)

        return NewCapturedType(captureStatus, constructor, lowerType, annotations, isMarkedNullable, isProjectionNotNull = true)
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

    override fun KotlinTypeMarker.isNullableType(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return TypeUtils.isNullableType(this)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean
    ): SimpleTypeMarker {
        require(constructor is TypeConstructor, constructor::errorMessage)

        val annotations = if (isExtensionFunction) {
            Annotations.create(listOf(BuiltInAnnotationDescriptor(builtIns, FqNames.extensionFunctionType, emptyMap())))
        } else Annotations.EMPTY

        @Suppress("UNCHECKED_CAST")
        return KotlinTypeFactory.simpleType(annotations, constructor, arguments as List<TypeProjection>, nullable)
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

    override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        require(type is UnwrappedType, type::errorMessage)
        return NewKotlinTypeChecker.Default.transformToNewType(type)
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

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        require(type is UnwrappedType, type::errorMessage)
        require(this is TypeSubstitutor, this::errorMessage)
        return safeSubstitute(type, Variance.INVARIANT)
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createStubType(typeVariable: TypeVariableMarker): StubTypeMarker {
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
        return classicIsSignedOrUnsignedNumberType()
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
        @Suppress("UNCHECKED_CAST")
        explicitSupertypes as List<SimpleType>
        return IntegerLiteralTypeConstructor.findCommonSuperType(explicitSupertypes)
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

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        return captureFromExpressionInternal(type as UnwrappedType)
    }

    override fun createErrorType(debugName: String): SimpleTypeMarker {
        return ErrorUtils.createErrorType(debugName)
    }

    override fun createErrorTypeWithCustomConstructor(debugName: String, constructor: TypeConstructorMarker): KotlinTypeMarker {
        require(constructor is TypeConstructor, constructor::errorMessage)
        return ErrorUtils.createErrorTypeWithCustomConstructor(debugName, constructor)
    }

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean {
        return this is NewCapturedTypeConstructor
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
        return (declarationDescriptor as? ClassDescriptor)?.isInline == true
    }

    override fun TypeConstructorMarker.isInnerClass(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return (declarationDescriptor as? ClassDescriptor)?.isInner == true
    }

    override fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return representativeUpperBound
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

        firstCandidate.constructor.safeAs<IntersectionTypeConstructor>()?.let { intersectionConstructor ->
            val intersectionTypeWithAlternative = intersectionConstructor.setAlternative(secondCandidate).createType()
            return if (firstCandidate.isMarkedNullable) intersectionTypeWithAlternative.makeNullableAsSpecified(true)
            else intersectionTypeWithAlternative

        } ?: error("Expected intersection type, found $firstCandidate")
    }
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

@OptIn(ExperimentalContracts::class)
fun requireOrDescribe(condition: Boolean, value: Any?) {
    contract {
        returns() implies condition
    }
    require(condition) {
        val typeInfo = if (value != null) {
            ", type = '${value::class}'"
        } else ""
        "Unexpected: value = '$value'$typeInfo"
    }
}
