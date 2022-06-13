/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.FlexibleTypeBoundsChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypePreparator
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.builtIns
import java.util.ArrayList

fun createCapturedType(typeProjection: TypeProjection): CapturedType {
    val capturedTypeConstructor = CapturedTypeConstructor(
        typeProjection,
        if (typeProjection.isStarProjection) listOf() else listOf(typeProjection.type.unwrap()),
        original = null
    )
    return CapturedType(
        CaptureStatus.FROM_EXPRESSION,
        capturedTypeConstructor,
        lowerType = null
    )
}

fun KotlinType.isCaptured(): Boolean = constructor is CapturedTypeConstructor

fun TypeSubstitution.wrapWithCapturingSubstitution(needApproximation: Boolean = true): TypeSubstitution =
    if (this is IndexedParametersSubstitution)
        IndexedParametersSubstitution(
            this.parameters,
            this.arguments.zip(this.parameters).map {
                it.first.createCapturedIfNeeded(it.second)
            }.toTypedArray(),
            approximateContravariantCapturedTypes = needApproximation
        )
    else
        object : DelegatedTypeSubstitution(this@wrapWithCapturingSubstitution) {
            override fun approximateContravariantCapturedTypes() = needApproximation
            override fun get(key: KotlinType) =
                super.get(key)?.createCapturedIfNeeded(key.constructor.declarationDescriptor as? TypeParameterDescriptor)
        }

private fun TypeProjection.createCapturedIfNeeded(typeParameterDescriptor: TypeParameterDescriptor?): TypeProjection {
    if (typeParameterDescriptor == null || projectionKind == Variance.INVARIANT) return this

    // Treat consistent projections as invariant
    if (typeParameterDescriptor.variance == projectionKind) {
        // TODO: Make star projection type lazy
        return if (isStarProjection)
            TypeProjectionImpl(LazyWrappedType(LockBasedStorageManager.NO_LOCKS) {
                this@createCapturedIfNeeded.type
            })
        else
            TypeProjectionImpl(this@createCapturedIfNeeded.type)
    }

    return TypeProjectionImpl(createCapturedType(this))
}

fun captureFromExpression(type: UnwrappedType): UnwrappedType? {
    val typeConstructor = type.constructor

    if (typeConstructor !is IntersectionTypeConstructor) {
        return captureFromArguments(type, CaptureStatus.FROM_EXPRESSION)
    }

    /*
     * We capture arguments in the intersection types in specific way:
     *  1) Firstly, we create captured arguments for all type arguments grouped by a type constructor* and a type argument's type.
     *      It means, that we create only one captured argument for two types `Foo<*>` and `Foo<*>?` within a flexible type, for instance.
     *      * In addition to grouping by type constructors, we look at possibility locating of two types in different bounds of the same flexible type.
     *        This is necessary in order to create the same captured arguments,
     *        for example, for `MutableList` in the lower bound of the flexible type and for `List` in the upper one.
     *        Example: MutableList<*>..List<*>? -> MutableList<Captured1(*)>..List<Captured2(*)>?, Captured1(*) and Captured2(*) are the same.
     *  2) Secondly, we replace type arguments with captured arguments by given a type constructor and type arguments.
     */
    val capturedArgumentsByComponents = captureArgumentsForIntersectionType(type) ?: return null

    // We reuse `TypeToCapture` for some types, suitability to reuse defines by `isSuitableForType`
    fun findCorrespondingCapturedArgumentsForType(type: KotlinType) =
        capturedArgumentsByComponents.find { typeToCapture -> typeToCapture.isSuitableForType(type) }?.capturedArguments

    fun replaceArgumentsWithCapturedArgumentsByIntersectionComponents(typeToReplace: UnwrappedType): List<SimpleType> {
        return if (typeToReplace.constructor is IntersectionTypeConstructor) {
            typeToReplace.constructor.supertypes.map { componentType ->
                val capturedArguments = findCorrespondingCapturedArgumentsForType(componentType)
                    ?: return@map componentType.asSimpleType()
                componentType.unwrap().replaceArguments(capturedArguments)
            }
        } else {
            val capturedArguments = findCorrespondingCapturedArgumentsForType(typeToReplace)
                ?: return listOf(typeToReplace.asSimpleType())
            listOf(typeToReplace.unwrap().replaceArguments(capturedArguments))
        }
    }

    return if (type is FlexibleType) {
        val lowerIntersectedType = intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.lowerBound))
            .makeNullableAsSpecified(type.lowerBound.isMarkedNullable)
        val upperIntersectedType = intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.upperBound))
            .makeNullableAsSpecified(type.upperBound.isMarkedNullable)

        KotlinTypeFactory.flexibleType(lowerIntersectedType, upperIntersectedType)
    } else {
        intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type)).makeNullableAsSpecified(type.isMarkedNullable)
    }
}

private fun captureArgumentsForIntersectionType(type: KotlinType): List<CapturedArguments>? {
    // It's possible to have one of the bounds as non-intersection type
    fun getTypesToCapture(type: KotlinType) =
        if (type.constructor is IntersectionTypeConstructor) type.constructor.supertypes else listOf(type)

    val filteredTypesToCapture =
        if (type is FlexibleType) {
            val typesToCapture = getTypesToCapture(type.lowerBound) + getTypesToCapture(type.upperBound)
            typesToCapture.distinctBy { (FlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(it) ?: it.constructor) to it.arguments }
        } else type.constructor.supertypes

    var changed = false

    val capturedArgumentsByTypes = filteredTypesToCapture.mapNotNull { typeToCapture ->
        val capturedArguments = captureArguments(typeToCapture.unwrap(), CaptureStatus.FROM_EXPRESSION)
            ?: return@mapNotNull null
        changed = true
        CapturedArguments(capturedArguments, originalType = typeToCapture)
    }

    if (!changed) return null

    return capturedArgumentsByTypes
}

// this function suppose that input type is simple classifier type
internal fun captureFromArguments(type: SimpleType, status: CaptureStatus) =
    captureArguments(type, status)?.let { type.replaceArguments(it) }

private fun captureFromArguments(type: UnwrappedType, status: CaptureStatus): UnwrappedType? {
    val capturedArguments = captureArguments(type, status) ?: return null

    return if (type is FlexibleType) {
        KotlinTypeFactory.flexibleType(
            type.lowerBound.replaceArguments(capturedArguments),
            type.upperBound.replaceArguments(capturedArguments)
        )
    } else {
        type.replaceArguments(capturedArguments)
    }
}

private fun UnwrappedType.replaceArguments(arguments: List<TypeProjection>) =
    KotlinTypeFactory.simpleType(attributes, constructor, arguments, isMarkedNullable)

private fun captureArguments(type: UnwrappedType, status: CaptureStatus): List<TypeProjection>? {
    if (type.arguments.size != type.constructor.parameters.size) return null

    val arguments = type.arguments
    if (arguments.all { it.projectionKind == Variance.INVARIANT }) return null

    val capturedArguments = arguments.zip(type.constructor.parameters).map { (projection, parameter) ->
        if (projection.projectionKind == Variance.INVARIANT) return@map projection

        val lowerType =
            if (!projection.isStarProjection && projection.projectionKind == Variance.IN_VARIANCE) {
                projection.type.unwrap()
            } else {
                null
            }

        CapturedType(status, lowerType, projection, parameter).asTypeProjection() // todo optimization: do not create type projection
    }

    val substitutor = TypeConstructorSubstitution.create(type.constructor, capturedArguments).buildSubstitutor()

    for (index in arguments.indices) {
        val oldProjection = arguments[index]
        val newProjection = capturedArguments[index]

        if (oldProjection.projectionKind == Variance.INVARIANT) continue
        val boundSupertypes = type.constructor.parameters[index].upperBounds.mapTo(mutableListOf()) {
            KotlinTypePreparator.Default.prepareType(substitutor.safeSubstitute(it, Variance.INVARIANT).unwrap())
        }

        val projectionSupertype = if (!oldProjection.isStarProjection && oldProjection.projectionKind == Variance.OUT_VARIANCE) {
            KotlinTypePreparator.Default.prepareType(oldProjection.type.unwrap())
        } else null

        val capturedType = newProjection.type as CapturedType
        capturedType.constructor.initializeSupertypes(projectionSupertype, boundSupertypes)
    }

    return capturedArguments
}

fun approximateCapturedTypesIfNecessary(typeProjection: TypeProjection?, approximateContravariant: Boolean): TypeProjection? {
    if (typeProjection == null) return null
    if (typeProjection.isStarProjection) return typeProjection

    val type = typeProjection.type
    if (!TypeUtils.contains(type, { it.isCaptured() })) {
        return typeProjection
    }
    val howThisTypeIsUsed = typeProjection.projectionKind
    if (howThisTypeIsUsed == Variance.OUT_VARIANCE) {
        // only 'return' type containing captured types should be over-approximated
        val approximation = approximateCapturedTypes(type)
        return TypeProjectionImpl(howThisTypeIsUsed, approximation.upper)
    }

    if (approximateContravariant) {
        // TODO: assert that howThisTypeIsUsed is always IN
        val approximation = approximateCapturedTypes(type).lower
        return TypeProjectionImpl(howThisTypeIsUsed, approximation)
    }

    return substituteCapturedTypesWithProjections(typeProjection)
}

private fun substituteCapturedTypesWithProjections(typeProjection: TypeProjection): TypeProjection? {
    val typeSubstitutor = TypeSubstitutor.create(object : TypeConstructorSubstitution() {
        override fun get(key: TypeConstructor): TypeProjection? {
            val capturedTypeConstructor = key as? CapturedTypeConstructor ?: return null
            if (capturedTypeConstructor.projection.isStarProjection) {
                return TypeProjectionImpl(Variance.OUT_VARIANCE, capturedTypeConstructor.projection.type)
            }
            return capturedTypeConstructor.projection
        }
    })
    return typeSubstitutor.substituteWithoutApproximation(typeProjection)
}

// todo: dynamic & raw type?
fun approximateCapturedTypes(type: KotlinType): ApproximationBounds<KotlinType> {
    if (type.isFlexible()) {
        val boundsForFlexibleLower = approximateCapturedTypes(type.lowerIfFlexible())
        val boundsForFlexibleUpper = approximateCapturedTypes(type.upperIfFlexible())

        return ApproximationBounds(
            KotlinTypeFactory.flexibleType(
                boundsForFlexibleLower.lower.lowerIfFlexible(),
                boundsForFlexibleUpper.lower.upperIfFlexible()
            ).inheritEnhancement(type),
            KotlinTypeFactory.flexibleType(
                boundsForFlexibleLower.upper.lowerIfFlexible(),
                boundsForFlexibleUpper.upper.upperIfFlexible()
            ).inheritEnhancement(type)
        )
    }

    val typeConstructor = type.constructor
    if (type.isCaptured()) {
        val typeProjection = (typeConstructor as CapturedTypeConstructor).projection
        fun KotlinType.makeNullableIfNeeded() = TypeUtils.makeNullableIfNeeded(this, type.isMarkedNullable)
        val bound = typeProjection.type.makeNullableIfNeeded()

        return when (typeProjection.projectionKind) {
            Variance.IN_VARIANCE -> ApproximationBounds(bound, type.builtIns.nullableAnyType)
            Variance.OUT_VARIANCE -> ApproximationBounds(type.builtIns.nothingType.makeNullableIfNeeded(), bound)
            else -> throw AssertionError("Only nontrivial projections should have been captured, not: $typeProjection")
        }
    }
    if (type.arguments.isEmpty() || type.arguments.size != typeConstructor.parameters.size) {
        return ApproximationBounds(type, type)
    }
    val lowerBoundArguments = ArrayList<TypeArgument>()
    val upperBoundArguments = ArrayList<TypeArgument>()
    for ((typeProjection, typeParameter) in type.arguments.zip(typeConstructor.parameters)) {
        val typeArgument = typeProjection.toTypeArgument(typeParameter)

        // Protection from infinite recursion caused by star projection
        if (typeProjection.isStarProjection) {
            lowerBoundArguments.add(typeArgument)
            upperBoundArguments.add(typeArgument)
        } else {
            val (lower, upper) = approximateProjection(typeArgument)
            lowerBoundArguments.add(lower)
            upperBoundArguments.add(upper)
        }
    }
    val lowerBoundIsTrivial = lowerBoundArguments.any { !it.isConsistent }
    return ApproximationBounds(
        if (lowerBoundIsTrivial) type.builtIns.nothingType else type.replaceTypeArguments(lowerBoundArguments),
        type.replaceTypeArguments(upperBoundArguments)
    )
}

data class ApproximationBounds<out T>(
    val lower: T,
    val upper: T
)

private class TypeArgument(
    val typeParameter: TypeParameterDescriptor,
    val inProjection: KotlinType,
    val outProjection: KotlinType
) {
    val isConsistent: Boolean
        get() = KotlinTypeChecker.DEFAULT.isSubtypeOf(inProjection, outProjection)
}

private fun TypeArgument.toTypeProjection(): TypeProjection {
    assert(isConsistent) {
        val descriptorRenderer = DescriptorRenderer.withOptions {
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
        }
        "Only consistent enhanced type projection can be converted to type projection, but " +
                "[${descriptorRenderer.render(typeParameter)}: <${descriptorRenderer.renderType(inProjection)}" +
                ", ${descriptorRenderer.renderType(outProjection)}>]" +
                " was found"
    }
    fun removeProjectionIfRedundant(variance: Variance) = if (variance == typeParameter.variance) Variance.INVARIANT else variance
    return when {
        inProjection == outProjection || typeParameter.variance == Variance.IN_VARIANCE -> TypeProjectionImpl(inProjection)
        KotlinBuiltIns.isNothing(inProjection) && typeParameter.variance != Variance.IN_VARIANCE ->
            TypeProjectionImpl(removeProjectionIfRedundant(Variance.OUT_VARIANCE), outProjection)
        KotlinBuiltIns.isNullableAny(outProjection) -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.IN_VARIANCE), inProjection)
        else -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.OUT_VARIANCE), outProjection)
    }
}

private fun TypeProjection.toTypeArgument(typeParameter: TypeParameterDescriptor) =
    when (TypeSubstitutor.combine(typeParameter.variance, this)) {
        Variance.INVARIANT -> TypeArgument(typeParameter, type, type)
        Variance.IN_VARIANCE -> TypeArgument(typeParameter, type, typeParameter.builtIns.nullableAnyType)
        Variance.OUT_VARIANCE -> TypeArgument(typeParameter, typeParameter.builtIns.nothingType, type)
    }

private fun KotlinType.replaceTypeArguments(newTypeArguments: List<TypeArgument>): KotlinType {
    assert(arguments.size == newTypeArguments.size) { "Incorrect type arguments $newTypeArguments" }
    return replace(newTypeArguments.map { it.toTypeProjection() })
}

private fun approximateProjection(typeArgument: TypeArgument): ApproximationBounds<TypeArgument> {
    val (inLower, inUpper) = approximateCapturedTypes(typeArgument.inProjection)
    val (outLower, outUpper) = approximateCapturedTypes(typeArgument.outProjection)
    return ApproximationBounds(
        lower = TypeArgument(typeArgument.typeParameter, inUpper, outLower),
        upper = TypeArgument(typeArgument.typeParameter, inLower, outUpper)
    )
}

// null means that type should be leaved as is
fun prepareArgumentTypeRegardingCaptureTypes(argumentType: UnwrappedType): UnwrappedType? {
    return if (argumentType is CapturedType) null else captureFromExpression(argumentType)
}
