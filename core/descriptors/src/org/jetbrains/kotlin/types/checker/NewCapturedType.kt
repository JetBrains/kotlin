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

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.inference.CapturedTypeConstructor
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.CapturedTypeMarker
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.DO_NOTHING_2

// if input type is capturedType, then we approximate it to UpperBound
// null means that type should be leaved as is
fun prepareArgumentTypeRegardingCaptureTypes(argumentType: UnwrappedType): UnwrappedType? {
    val simpleType = NewKotlinTypeChecker.Default.transformToNewType(argumentType.lowerIfFlexible())
    if (simpleType.constructor is IntersectionTypeConstructor) {
        var changed = false
        val preparedSuperTypes = simpleType.constructor.supertypes.map {
            prepareArgumentTypeRegardingCaptureTypes(it.unwrap())?.apply { changed = true } ?: it.unwrap()
        }
        if (!changed) return null
        return intersectTypes(preparedSuperTypes).makeNullableAsSpecified(simpleType.isMarkedNullable)
    }
    if (simpleType is NewCapturedType) {
        // todo may be we should respect flexible/definitelyNotNull captured types also...
        return simpleType.constructor.supertypes.takeIf { it.isNotEmpty() }?.let {
            intersectTypes(it).makeNullableAsSpecified(simpleType.isMarkedNullable)
        } ?: argumentType.builtIns.nullableAnyType
    }
    return captureFromExpression(simpleType)
}

fun captureFromExpression(type: UnwrappedType): UnwrappedType? = when (type) {
    is SimpleType -> captureFromExpression(type)
    // i.e. if there is nothing to capture -- no changes, if there is something -- use lowerBound as base type
    is FlexibleType -> captureFromExpression(type.lowerBound)
}

fun captureFromExpression(type: SimpleType): UnwrappedType? {
    val typeConstructor = type.constructor
    if (typeConstructor is IntersectionTypeConstructor) {
        var changed = false
        val capturedSupertypes = typeConstructor.supertypes.map {
            captureFromExpression(it.unwrap())?.apply { changed = true } ?: it.unwrap()
        }
        if (!changed) return null
        return intersectTypes(capturedSupertypes).makeNullableAsSpecified(type.isMarkedNullable)
    }
    return captureFromArguments(type, CaptureStatus.FROM_EXPRESSION)
}

// this function suppose that input type is simple classifier type
fun captureFromArguments(
    type: SimpleType,
    status: CaptureStatus,
    acceptNewCapturedType: ((argumentIndex: Int, NewCapturedType) -> Unit) = DO_NOTHING_2
): SimpleType? {
    if (type.arguments.size != type.constructor.parameters.size) return null

    val arguments = type.arguments
    if (arguments.all { it.projectionKind == Variance.INVARIANT }) return null

    val newArguments = arguments.map { projection ->
        if (projection.projectionKind == Variance.INVARIANT) return@map projection

        val lowerType = if (!projection.isStarProjection && projection.projectionKind == Variance.IN_VARIANCE) {
            projection.type.unwrap()
        } else null

        NewCapturedType(status, lowerType, projection).asTypeProjection() // todo optimization: do not create type projection
    }

    val substitutor = TypeConstructorSubstitution.create(type.constructor, newArguments).buildSubstitutor()
    for (index in arguments.indices) {
        val oldProjection = arguments[index]
        val newProjection = newArguments[index]

        if (oldProjection.projectionKind == Variance.INVARIANT) continue
        var upperBounds = type.constructor.parameters[index].upperBounds.map {
            NewKotlinTypeChecker.Default.transformToNewType(substitutor.safeSubstitute(it, Variance.INVARIANT).unwrap())
        }
        if (!oldProjection.isStarProjection && oldProjection.projectionKind == Variance.OUT_VARIANCE) {
            upperBounds += NewKotlinTypeChecker.Default.transformToNewType(oldProjection.type.unwrap())
        }

        val capturedType = newProjection.type as NewCapturedType
        capturedType.constructor.initializeSupertypes(upperBounds)
        acceptNewCapturedType(index, capturedType)
    }

    return KotlinTypeFactory.simpleType(type.annotations, type.constructor, newArguments, type.isMarkedNullable)
}

/**
 * Now [lowerType] is not null only for in projections.
 * Example: `Inv<in String>` For `in String` we create CapturedType with [lowerType] = String.
 *
 * TODO: interface D<T, S: List<T>, D<*, List<Number>> -> D<Q, List<Number>>
 *     We should set [lowerType] for Q as Number. For this we should use constraint system.
 *
 */
class NewCapturedType(
    val captureStatus: CaptureStatus,
    override val constructor: NewCapturedTypeConstructor,
    val lowerType: UnwrappedType?, // todo check lower type for nullable captured types
    override val annotations: Annotations = Annotations.EMPTY,
    override val isMarkedNullable: Boolean = false
) : SimpleType(), CapturedTypeMarker {
    internal constructor(captureStatus: CaptureStatus, lowerType: UnwrappedType?, projection: TypeProjection) :
            this(captureStatus, NewCapturedTypeConstructor(projection), lowerType)

    override val arguments: List<TypeProjection> get() = listOf()

    override val memberScope: MemberScope // todo what about foo().bar() where foo() return captured type?
        get() = ErrorUtils.createErrorScope("No member resolution should be done on captured type!", true)

    override fun replaceAnnotations(newAnnotations: Annotations) =
        NewCapturedType(captureStatus, constructor, lowerType, newAnnotations, isMarkedNullable)

    override fun makeNullableAsSpecified(newNullability: Boolean) =
        NewCapturedType(captureStatus, constructor, lowerType, annotations, newNullability)

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) =
        NewCapturedType(
            captureStatus,
            constructor.refine(kotlinTypeRefiner),
            lowerType?.let { kotlinTypeRefiner.refineType(it).unwrap() },
            annotations,
            isMarkedNullable
        )
}

class NewCapturedTypeConstructor(
    override val projection: TypeProjection,
    private var supertypesComputation: (() -> List<UnwrappedType>)? = null,
    private val original: NewCapturedTypeConstructor? = null
) : CapturedTypeConstructor {

    constructor(
        projection: TypeProjection,
        supertypes: List<UnwrappedType>,
        original: NewCapturedTypeConstructor? = null
    ) : this(projection, { supertypes }, original)

    private val _supertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        supertypesComputation?.invoke()
    }

    fun initializeSupertypes(supertypes: List<UnwrappedType>) {
        assert(this.supertypesComputation == null) {
            "Already initialized! oldValue = ${this.supertypesComputation}, newValue = $supertypes"
        }
        this.supertypesComputation = { supertypes }
    }

    override fun getSupertypes() = _supertypes ?: emptyList()
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun isFinal() = false
    override fun isDenotable() = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null
    override fun getBuiltIns(): KotlinBuiltIns = projection.type.builtIns

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) =
        NewCapturedTypeConstructor(
            projection.refine(kotlinTypeRefiner),
            supertypesComputation?.let {
                {
                    supertypes.map { it.refine(kotlinTypeRefiner) }
                }
            },
            original ?: this
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewCapturedTypeConstructor

        return (original ?: this) === (other.original ?: other)
    }

    override fun hashCode(): Int = original?.hashCode() ?: super.hashCode()
    override fun toString() = "CapturedType($projection)"
}
