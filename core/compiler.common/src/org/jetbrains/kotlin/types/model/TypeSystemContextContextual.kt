/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

context(c: TypeSystemContext)
fun KotlinTypeMarker.asRigidType(): RigidTypeMarker? = with(c) { asRigidType() }

private const val USELESS_CALL_MESSAGE = "This call does effectively nothing, please drop it"

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun RigidTypeMarker.asRigidType(): RigidTypeMarker = this

context(c: TypeSystemContext)
fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? = with(c) { asFlexibleType() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun FlexibleTypeMarker.asFlexibleType(): FlexibleTypeMarker = this

context(c: TypeSystemContext)
fun KotlinTypeMarker.isError(): Boolean = with(c) { isError() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isError(): Boolean = with(c) { isError() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isUninferredParameter(): Boolean = with(c) { isUninferredParameter() }

context(c: TypeSystemContext)
fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker? = with(c) { asDynamicType() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun DynamicTypeMarker.asDynamicType(): DynamicTypeMarker = this

context(c: TypeSystemContext)
fun KotlinTypeMarker.isRawType(): Boolean = with(c) { isRawType() }

context(c: TypeSystemContext)
fun FlexibleTypeMarker.upperBound(): RigidTypeMarker = with(c) { upperBound() }

context(c: TypeSystemContext)
fun FlexibleTypeMarker.lowerBound(): RigidTypeMarker = with(c) { lowerBound() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? = with(c) { asCapturedType() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun CapturedTypeMarker.asCapturedType(): CapturedTypeMarker = this

context(c: TypeSystemContext)
fun RigidTypeMarker.asCapturedTypeUnwrappingDnn(): CapturedTypeMarker? = with(c) { asCapturedTypeUnwrappingDnn() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun CapturedTypeMarker.asCapturedTypeUnwrappingDnn(): CapturedTypeMarker = this

context(c: TypeSystemContext)
fun KotlinTypeMarker.isCapturedType() = with(c) { isCapturedType() }

context(c: TypeSystemContext)
fun RigidTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? = with(c) { asDefinitelyNotNullType() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun DefinitelyNotNullTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker = this

context(c: TypeSystemContext)
fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker = with(c) { original() }

context(c: TypeSystemContext)
fun RigidTypeMarker.originalIfDefinitelyNotNullable(): SimpleTypeMarker = with(c) { originalIfDefinitelyNotNullable() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun SimpleTypeMarker.originalIfDefinitelyNotNullable(): SimpleTypeMarker = this

context(c: TypeSystemContext)
fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker = with(c) { makeDefinitelyNotNullOrNotNull() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun DefinitelyNotNullTypeMarker.makeDefinitelyNotNullOrNotNull(): DefinitelyNotNullTypeMarker = this

context(c: TypeSystemContext)
fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(preserveAttributes: Boolean): KotlinTypeMarker =
    with(c) { makeDefinitelyNotNullOrNotNull(preserveAttributes) }

context(c: TypeSystemContext)
fun RigidTypeMarker.makeDefinitelyNotNullOrNotNull(): RigidTypeMarker = with(c) { makeDefinitelyNotNullOrNotNull() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isMarkedNullable(): Boolean = with(c) { isMarkedNullable() }

context(c: TypeSystemContext)
fun RigidTypeMarker.withNullability(nullable: Boolean): RigidTypeMarker = with(c) { withNullability(nullable) }

context(c: TypeSystemContext)
fun RigidTypeMarker.typeConstructor(): TypeConstructorMarker = with(c) { typeConstructor() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker = with(c) { withNullability(nullable) }

context(c: TypeSystemContext)
fun CapturedTypeMarker.isOldCapturedType(): Boolean = with(c) { isOldCapturedType() }

context(c: TypeSystemContext)
fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker = with(c) { typeConstructor() }

context(c: TypeSystemContext)
fun CapturedTypeMarker.captureStatus(): CaptureStatus = with(c) { captureStatus() }

context(c: TypeSystemContext)
fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker = with(c) { projection() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.argumentsCount(): Int = with(c) { argumentsCount() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker = with(c) { getArgument(index) }

context(c: TypeSystemContext)
fun KotlinTypeMarker.getArguments(): List<TypeArgumentMarker> = with(c) { getArguments() }

context(c: TypeSystemContext)
fun RigidTypeMarker.getArgumentOrNull(index: Int): TypeArgumentMarker? = with(c) { getArgumentOrNull(index) }

context(c: TypeSystemContext)
fun RigidTypeMarker.isStubType(): Boolean = with(c) { isStubType() }

context(c: TypeSystemContext)
fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean = with(c) { isStubTypeForVariableInSubtyping() }

context(_: TypeSystemContext)
fun RigidTypeMarker.isStubTypeForVariableInSubtypingOrCaptured(): Boolean =
    isStubTypeForVariableInSubtyping() || isCapturedStubTypeForVariableInSubtyping()

context(_: TypeSystemContext)
private fun RigidTypeMarker.isCapturedStubTypeForVariableInSubtyping() =
    asCapturedTypeUnwrappingDnn()?.typeConstructor()?.projection()?.takeUnless { it.isStarProjection() }
        ?.getType()?.asRigidType()?.isStubTypeForVariableInSubtyping() == true

context(c: TypeSystemContext)
fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean = with(c) { isStubTypeForBuilderInference() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker = with(c) { unwrapStubTypeVariableConstructor() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker = with(c) { asTypeArgument() }

context(c: TypeSystemContext)
fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? = with(c) { lowerType() }

context(c: TypeSystemContext)
fun TypeArgumentMarker.isStarProjection(): Boolean = with(c) { isStarProjection() }

context(c: TypeSystemContext)
fun TypeArgumentMarker.getVariance(): TypeVariance = with(c) { getVariance() }

context(c: TypeSystemContext)
fun TypeArgumentMarker.getType(): KotlinTypeMarker? = with(c) { getType() }

context(c: TypeSystemContext)
fun TypeArgumentMarker.replaceType(newType: KotlinTypeMarker): TypeArgumentMarker = with(c) { replaceType(newType) }

context(c: TypeSystemContext)
fun TypeConstructorMarker.parametersCount(): Int = with(c) { parametersCount() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker = with(c) { getParameter(index) }

context(c: TypeSystemContext)
fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> = with(c) { getParameters() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> = with(c) { supertypes() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntersection(): Boolean = with(c) { isIntersection() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isClassTypeConstructor(): Boolean = with(c) { isClassTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isInterface(): Boolean = with(c) { isInterface() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean = with(c) { isIntegerLiteralTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean = with(c) { isIntegerLiteralConstantTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean = with(c) { isIntegerConstantOperatorTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isLocalType(): Boolean = with(c) { isLocalType() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isAnonymous(): Boolean = with(c) { isAnonymous() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? = with(c) { getTypeParameterClassifier() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean = with(c) { isTypeParameterTypeConstructor() }

context(c: TypeSystemContext)
val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
    get() = with(c) { typeParameter }

context(c: TypeSystemContext)
fun TypeParameterMarker.getVariance(): TypeVariance = with(c) { getVariance() }

context(c: TypeSystemContext)
fun TypeParameterMarker.upperBoundCount(): Int = with(c) { upperBoundCount() }

context(c: TypeSystemContext)
fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker = with(c) { getUpperBound(index) }

context(c: TypeSystemContext)
fun TypeParameterMarker.getUpperBounds(): List<KotlinTypeMarker> = with(c) { getUpperBounds() }

context(c: TypeSystemContext)
fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker = with(c) { getTypeConstructor() }

context(c: TypeSystemContext)
fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker? = null): Boolean = with(c) { hasRecursiveBounds(selfConstructor) }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isDenotable(): Boolean = with(c) { isDenotable() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.lowerBoundIfFlexible(): RigidTypeMarker = with(c) { lowerBoundIfFlexible() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.upperBoundIfFlexible(): RigidTypeMarker = with(c) { upperBoundIfFlexible() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun RigidTypeMarker.lowerBoundIfFlexible(): RigidTypeMarker = this

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun RigidTypeMarker.upperBoundIfFlexible(): RigidTypeMarker = this

context(c: TypeSystemContext)
fun KotlinTypeMarker.isFlexibleWithDifferentTypeConstructors(): Boolean = with(c) { isFlexibleWithDifferentTypeConstructors() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isFlexible(): Boolean = with(c) { isFlexible() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isDynamic(): Boolean = with(c) { isDynamic() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isCapturedDynamic(): Boolean = with(c) { isCapturedDynamic() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isDefinitelyNotNullType(): Boolean = with(c) { isDefinitelyNotNullType() }

context(c: TypeSystemContext)
fun RigidTypeMarker.isDefinitelyNotNullType(): Boolean = with(c) { isDefinitelyNotNullType() }

@ObsoleteTypeKind
context(c: TypeSystemContext)
fun KotlinTypeMarker.isNotNullTypeParameter(): Boolean = with(c) { isNotNullTypeParameter() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.hasFlexibleNullability() = with(c) { hasFlexibleNullability() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.typeConstructor(): TypeConstructorMarker = with(c) { typeConstructor() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isNullableType(considerTypeVariableBounds: Boolean = true): Boolean = with(c) { isNullableType(considerTypeVariableBounds) }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isNullableAny() = with(c) { isNullableAny() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isNothing() = with(c) { isNothing() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isFlexibleNothing(): Boolean = with(c) { isFlexibleNothing() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isNullableNothing() = with(c) { isNullableNothing() }

context(c: TypeSystemContext)
fun RigidTypeMarker.isClassType(): Boolean = with(c) { isClassType() }

context(c: TypeSystemContext)
fun RigidTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? =
    with(c) { fastCorrespondingSupertypes(constructor) }

context(c: TypeSystemContext)
fun RigidTypeMarker.isIntegerLiteralType(): Boolean = with(c) { isIntegerLiteralType() }

context(c: TypeSystemContext)
fun RigidTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker> = with(c) { possibleIntegerTypes() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean = with(c) { isCommonFinalClassConstructor() }

context(c: TypeSystemContext)
fun RigidTypeMarker.asArgumentList(): TypeArgumentListMarker = with(c) { asArgumentList() }

context(c: TypeSystemContext)
operator fun TypeArgumentListMarker.get(index: Int) = with(c) { get(index) }

context(c: TypeSystemContext)
fun TypeArgumentListMarker.size(): Int = with(c) { size() }

context(c: TypeSystemContext)
operator fun TypeArgumentListMarker.iterator() = with(c) { iterator() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isAnyConstructor(): Boolean = with(c) { isAnyConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isNothingConstructor(): Boolean = with(c) { isNothingConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isArrayConstructor(): Boolean = with(c) { isArrayConstructor() }

context(c: TypeSystemContext)
fun RigidTypeMarker.isSingleClassifierType(): Boolean = with(c) { isSingleClassifierType() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isRigidType(): Boolean = with(c) { isRigidType() }

context(c: TypeSystemContext)
fun RigidTypeMarker.isPrimitiveType(): Boolean = with(c) { isPrimitiveType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isPrimitiveType(): Boolean = with(c) { isPrimitiveType() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.getAttributes(): List<AnnotationMarker> = with(c) { getAttributes() }

context(c: TypeSystemContext)
fun KotlinTypeMarker.isTypeVariableType(): Boolean = with(c) { isTypeVariableType() }

context(c: TypeSystemContext)
fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker = with(c) { safeSubstitute(type) }
