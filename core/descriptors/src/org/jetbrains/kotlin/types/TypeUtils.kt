/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types.typeUtil

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.inference.isCaptured
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.checker.intersectTypes
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.types.model.TypeVariableTypeConstructorMarker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

enum class TypeNullability {
    NOT_NULL,
    NULLABLE,
    FLEXIBLE
}

fun KotlinType.nullability(): TypeNullability {
    return when {
        isNullabilityFlexible() -> TypeNullability.FLEXIBLE
        TypeUtils.isNullableType(this) -> TypeNullability.NULLABLE
        else -> TypeNullability.NOT_NULL
    }
}

val KotlinType.builtIns: KotlinBuiltIns
    get() = constructor.builtIns

fun KotlinType.makeNullable() = TypeUtils.makeNullable(this)
fun KotlinType.makeNotNullable() = TypeUtils.makeNotNullable(this)

fun KotlinType.immediateSupertypes(): Collection<KotlinType> = TypeUtils.getImmediateSupertypes(this)
fun KotlinType.supertypes(): Collection<KotlinType> = TypeUtils.getAllSupertypes(this)

fun KotlinType.isNothing(): Boolean = KotlinBuiltIns.isNothing(this)
fun KotlinType.isNullableNothing(): Boolean = KotlinBuiltIns.isNullableNothing(this)
fun KotlinType.isNothingOrNullableNothing(): Boolean = KotlinBuiltIns.isNothingOrNullableNothing(this)
fun KotlinType.isUnit(): Boolean = KotlinBuiltIns.isUnit(this)
fun KotlinType.isAnyOrNullableAny(): Boolean = KotlinBuiltIns.isAnyOrNullableAny(this)
fun KotlinType.isAny(): Boolean = KotlinBuiltIns.isAny(this)
fun KotlinType.isNullableAny(): Boolean = KotlinBuiltIns.isNullableAny(this)
fun KotlinType.isBoolean(): Boolean = KotlinBuiltIns.isBoolean(this)
fun KotlinType.isPrimitiveNumberType(): Boolean = KotlinBuiltIns.isPrimitiveType(this) && !isBoolean()
fun KotlinType.isUnsignedNumberType(): Boolean = UnsignedTypes.isUnsignedType(this)
fun KotlinType.isSignedOrUnsignedNumberType(): Boolean = isPrimitiveNumberType() || isUnsignedNumberType()

fun KotlinType.isBooleanOrNullableBoolean(): Boolean = KotlinBuiltIns.isBooleanOrNullableBoolean(this)
fun KotlinType.isNotNullThrowable(): Boolean = KotlinBuiltIns.isThrowableOrNullableThrowable(this) && !isMarkedNullable
fun KotlinType.isByte() = KotlinBuiltIns.isByte(this)
fun KotlinType.isChar() = KotlinBuiltIns.isChar(this)
fun KotlinType.isShort() = KotlinBuiltIns.isShort(this)
fun KotlinType.isInt() = KotlinBuiltIns.isInt(this)
fun KotlinType.isLong() = KotlinBuiltIns.isLong(this)
fun KotlinType.isFloat() = KotlinBuiltIns.isFloat(this)
fun KotlinType.isDouble() = KotlinBuiltIns.isDouble(this)

fun KotlinType.isPrimitiveNumberOrNullableType(): Boolean =
    KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(this) &&
            !KotlinBuiltIns.isBooleanOrNullableBoolean(this) &&
            !KotlinBuiltIns.isCharOrNullableChar(this)

fun KotlinType.isTypeParameter(): Boolean = TypeUtils.isTypeParameter(this)

fun KotlinType.containsTypeParameter(): Boolean = TypeUtils.contains(this) { t -> TypeUtils.isTypeParameter(t) }

fun KotlinType.upperBoundedByPrimitiveNumberOrNullableType(): Boolean =
    TypeUtils.getTypeParameterDescriptorOrNull(this)?.upperBounds?.any {
        it.isPrimitiveNumberOrNullableType() || it.upperBoundedByPrimitiveNumberOrNullableType()
    } == true

fun KotlinType.isInterface(): Boolean = (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.INTERFACE
fun KotlinType.isEnum(): Boolean = (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS

fun KotlinType?.isArrayOfNothing(): Boolean {
    if (this == null || !KotlinBuiltIns.isArray(this)) return false

    val typeArg = arguments.firstOrNull()?.type
    return typeArg != null && KotlinBuiltIns.isNothingOrNullableNothing(typeArg)
}


fun KotlinType.isSubtypeOf(superType: KotlinType): Boolean = KotlinTypeChecker.DEFAULT.isSubtypeOf(this, superType)

fun isNullabilityMismatch(expected: KotlinType, actual: KotlinType) =
    !expected.isMarkedNullable && actual.isMarkedNullable && actual.isSubtypeOf(TypeUtils.makeNullable(expected))

fun KotlinType.cannotBeReified(): Boolean =
    KotlinBuiltIns.isNothingOrNullableNothing(this) || this.isDynamic() || this.isCaptured()

fun TypeProjection.substitute(doSubstitute: (KotlinType) -> KotlinType): TypeProjection {
    return if (isStarProjection)
        this
    else TypeProjectionImpl(projectionKind, doSubstitute(type))
}

fun KotlinType.replaceAnnotations(newAnnotations: Annotations): KotlinType {
    if (annotations.isEmpty() && newAnnotations.isEmpty()) return this
    return unwrap().replaceAnnotations(newAnnotations)
}

fun KotlinTypeChecker.equalTypesOrNulls(type1: KotlinType?, type2: KotlinType?): Boolean {
    if (type1 === type2) return true
    if (type1 == null || type2 == null) return false
    return equalTypes(type1, type2)
}

fun KotlinType.containsError() = ErrorUtils.containsErrorType(this)

fun List<KotlinType>.defaultProjections(): List<TypeProjection> = map(::TypeProjectionImpl)

fun KotlinType.isDefaultBound(): Boolean = KotlinBuiltIns.isDefaultBound(getSupertypeRepresentative())

fun createProjection(type: KotlinType, projectionKind: Variance, typeParameterDescriptor: TypeParameterDescriptor?): TypeProjection =
    TypeProjectionImpl(if (typeParameterDescriptor?.variance == projectionKind) Variance.INVARIANT else projectionKind, type)

fun <T> Collection<T>.closure(preserveOrder: Boolean = false, f: (T) -> Collection<T>): Collection<T> {
    if (size == 0) return this

    val result = if (preserveOrder) LinkedHashSet(this) else HashSet(this)
    var elementsToCheck = result
    var oldSize = 0
    while (result.size > oldSize) {
        oldSize = result.size
        val toAdd = if (preserveOrder) linkedSetOf() else hashSetOf<T>()
        elementsToCheck.forEach { toAdd.addAll(f(it)) }
        result.addAll(toAdd)
        elementsToCheck = toAdd
    }

    return result
}

fun boundClosure(types: Collection<KotlinType>): Collection<KotlinType> =
    types.closure { type -> TypeUtils.getTypeParameterDescriptorOrNull(type)?.upperBounds ?: emptySet() }

fun constituentTypes(types: Collection<KotlinType>): Collection<KotlinType> {
    val result = hashSetOf<KotlinType>()
    constituentTypes(result, types)
    return result
}

fun KotlinType.constituentTypes(): Collection<KotlinType> =
    constituentTypes(listOf(this))

private fun constituentTypes(result: MutableSet<KotlinType>, types: Collection<KotlinType>) {
    result.addAll(types)
    for (type in types) {
        if (type.isFlexible()) {
            with(type.asFlexibleType()) { constituentTypes(result, setOf(lowerBound, upperBound)) }
        } else {
            constituentTypes(result, type.arguments.mapNotNull { if (!it.isStarProjection) it.type else null })
        }
    }
}

fun KotlinType.getImmediateSuperclassNotAny(): KotlinType? {
    val superclasses = constructor.supertypes.filter {
        DescriptorUtils.isClassOrEnumClass(it.constructor.declarationDescriptor) && !KotlinBuiltIns.isAnyOrNullableAny(it)
    }
    return superclasses.singleOrNull()?.let {
        TypeUtils.createSubstitutedSupertype(this, it, TypeSubstitutor.create(this))
    }
}

fun KotlinType.asTypeProjection(): TypeProjection = TypeProjectionImpl(this)
fun KotlinType.contains(predicate: (UnwrappedType) -> Boolean) = TypeUtils.contains(this, predicate)

fun KotlinType.replaceArgumentsWithStarProjections() = replaceArgumentsByParametersWith(::StarProjectionImpl)
fun KotlinType.replaceArgumentsWithNothing() = replaceArgumentsByParametersWith { it.builtIns.nothingType.asTypeProjection() }

fun KotlinType.extractTypeParametersFromUpperBounds(visitedTypeParameters: Set<TypeParameterDescriptor>?): Set<TypeParameterDescriptor> =
    mutableSetOf<TypeParameterDescriptor>().also { extractTypeParametersFromUpperBounds(this, it, visitedTypeParameters) }

private fun KotlinType.extractTypeParametersFromUpperBounds(
    baseType: KotlinType,
    to: MutableSet<TypeParameterDescriptor>,
    visitedTypeParameters: Set<TypeParameterDescriptor>?
) {
    val declarationDescriptor = constructor.declarationDescriptor

    if (declarationDescriptor is TypeParameterDescriptor) {
        if (constructor != baseType.constructor) {
            to += declarationDescriptor
        } else {
            for (upperBound in declarationDescriptor.upperBounds) {
                upperBound.extractTypeParametersFromUpperBounds(baseType, to, visitedTypeParameters)
            }
        }
    } else {
        val typeParameters = (constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
        for ((i, argument) in arguments.withIndex()) {
            val typeParameter = typeParameters?.getOrNull(i) // TODO: support inner classes' type parameters
            val isTypeParameterVisited = typeParameter != null && visitedTypeParameters != null && typeParameter in visitedTypeParameters
            if (isTypeParameterVisited || argument.isStarProjection) continue
            if (argument.type.constructor.declarationDescriptor in to || argument.type.constructor == baseType.constructor) continue
            argument.type.extractTypeParametersFromUpperBounds(baseType, to, visitedTypeParameters)
        }
    }
}

@JvmOverloads
fun hasTypeParameterRecursiveBounds(
    typeParameter: TypeParameterDescriptor,
    selfConstructor: TypeConstructor? = null,
    visitedTypeParameters: Set<TypeParameterDescriptor>? = null
): Boolean =
    typeParameter.upperBounds.any { upperBound ->
        upperBound.containsSelfTypeParameter(typeParameter.defaultType.constructor, visitedTypeParameters)
                && (selfConstructor == null || upperBound.constructor == selfConstructor)
    }

private fun KotlinType.containsSelfTypeParameter(
    baseConstructor: TypeConstructor,
    visitedTypeParameters: Set<TypeParameterDescriptor>?
): Boolean {
    if (this.constructor == baseConstructor) return true

    val typeParameters = (constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
    return arguments.withIndex().any { (i, argument) ->
        val typeParameter = typeParameters?.getOrNull(i)
        val isTypeParameterVisited =
            typeParameter != null && visitedTypeParameters != null && typeParameter in visitedTypeParameters
        if (isTypeParameterVisited || argument.isStarProjection) return@any false
        argument.type.containsSelfTypeParameter(baseConstructor, visitedTypeParameters)
    }
}

fun KotlinType.replaceArgumentsWithStarProjectionOrMapped(
    substitutor: TypeSubstitutor,
    substitutionMap: Map<TypeConstructor, TypeProjection>,
    variance: Variance,
    visitedTypeParameters: Set<TypeParameterDescriptor>?
) =
    replaceArgumentsByParametersWith { typeParameterDescriptor ->
        val argument = arguments.getOrNull(typeParameterDescriptor.index)
        val isTypeParameterVisited = visitedTypeParameters != null && typeParameterDescriptor in visitedTypeParameters
        if (!isTypeParameterVisited && argument != null && argument.type.constructor in substitutionMap) {
            argument
        } else StarProjectionImpl(typeParameterDescriptor)
    }.let { substitutor.safeSubstitute(it, variance) }


inline fun KotlinType.replaceArgumentsByParametersWith(replacement: (TypeParameterDescriptor) -> TypeProjection): KotlinType {
    val unwrapped = unwrap()
    return when (unwrapped) {
        is FlexibleType -> KotlinTypeFactory.flexibleType(
            unwrapped.lowerBound.replaceArgumentsByParametersWith(replacement),
            unwrapped.upperBound.replaceArgumentsByParametersWith(replacement)
        )
        is SimpleType -> unwrapped.replaceArgumentsByParametersWith(replacement)
    }.inheritEnhancement(unwrapped)
}

inline fun SimpleType.replaceArgumentsByParametersWith(replacement: (TypeParameterDescriptor) -> TypeProjection): SimpleType {
    if (constructor.parameters.isEmpty() || constructor.declarationDescriptor == null) return this

    val newArguments = constructor.parameters.map(replacement)

    return replace(newArguments)
}

inline fun SimpleType.replaceArgumentsByExistingArgumentsWith(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleType {
    if (arguments.isEmpty()) return this
    return replace(newArguments = arguments.map { replacement(it) as TypeProjection })
}

fun KotlinType.containsTypeAliasParameters(): Boolean =
    contains {
        it.constructor.declarationDescriptor?.isTypeAliasParameter() ?: false
    }

fun KotlinType.containsTypeAliases(): Boolean =
    contains {
        it.constructor.declarationDescriptor is TypeAliasDescriptor
    }

fun ClassifierDescriptor.isTypeAliasParameter(): Boolean =
    this is TypeParameterDescriptor && containingDeclaration is TypeAliasDescriptor

fun KotlinType.requiresTypeAliasExpansion(): Boolean =
    contains {
        it.constructor.declarationDescriptor?.let {
            it is TypeAliasDescriptor || it is TypeParameterDescriptor
        } ?: false
    }

fun KotlinType.containsTypeProjectionsInTopLevelArguments(): Boolean {
    if (isError) return false
    val possiblyInnerType = buildPossiblyInnerType() ?: return false
    return possiblyInnerType.arguments.any { it.isStarProjection || it.projectionKind != Variance.INVARIANT }
}

val TypeParameterDescriptor.representativeUpperBound: KotlinType
    get() {
        assert(upperBounds.isNotEmpty()) { "Upper bounds should not be empty: $this" }

        return upperBounds.firstOrNull {
            val classDescriptor = it.constructor.declarationDescriptor as? ClassDescriptor ?: return@firstOrNull false
            classDescriptor.kind != ClassKind.INTERFACE && classDescriptor.kind != ClassKind.ANNOTATION_CLASS
        } ?: upperBounds.first()
    }

fun KotlinType.expandIntersectionTypeIfNecessary(): Collection<KotlinType> {
    if (constructor !is IntersectionTypeConstructor) return listOf(this)
    val types = constructor.supertypes
    return if (isMarkedNullable) {
        types.map { it.makeNullable() }
    } else {
        types
    }
}

fun KotlinType.unCapture(): KotlinType = unwrap().unCapture()

fun UnwrappedType.unCapture(): UnwrappedType = when (this) {
    is AbbreviatedType -> unCapture()
    is SimpleType -> unCapture()
    is FlexibleType -> unCapture()
}

fun SimpleType.unCapture(): UnwrappedType {
    if (this is ErrorType) return this
    if (this is NewCapturedType)
        return unCaptureTopLevelType()

    val newArguments = arguments.map(::unCaptureProjection)
    return replace(newArguments).unwrap()
}

fun unCaptureProjection(projection: TypeProjection): TypeProjection {
    val unCapturedProjection = projection.type.constructor.safeAs<NewCapturedTypeConstructor>()?.projection ?: projection
    if (unCapturedProjection.isStarProjection || unCapturedProjection.type is ErrorType) return unCapturedProjection

    val newArguments = unCapturedProjection.type.arguments.map(::unCaptureProjection)
    return TypeProjectionImpl(
        unCapturedProjection.projectionKind,
        unCapturedProjection.type.replace(newArguments)
    )
}

fun AbbreviatedType.unCapture(): SimpleType {
    val newType = expandedType.unCapture()
    return AbbreviatedType(newType as? SimpleType ?: expandedType, abbreviation)
}

fun FlexibleType.unCapture(): FlexibleType {
    val unCapturedLowerBound = when (val unCaptured = lowerBound.unCapture()) {
        is SimpleType -> unCaptured
        is FlexibleType -> unCaptured.lowerBound
    }

    val unCapturedUpperBound = when (val unCaptured = upperBound.unCapture()) {
        is SimpleType -> unCaptured
        is FlexibleType -> unCaptured.upperBound
    }

    return FlexibleTypeImpl(unCapturedLowerBound, unCapturedUpperBound)
}

private fun NewCapturedType.unCaptureTopLevelType(): UnwrappedType {
    if (lowerType != null) return lowerType

    val supertypes = constructor.supertypes
    if (supertypes.isNotEmpty()) return intersectTypes(supertypes)

    return constructor.projection.type.unwrap()
}

fun KotlinType?.shouldBeUpdated() =
    this == null || contains { it is StubTypeForBuilderInference || it.constructor is TypeVariableTypeConstructorMarker || it.isError }
