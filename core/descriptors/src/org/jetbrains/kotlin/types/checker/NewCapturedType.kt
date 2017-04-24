/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.DO_NOTHING_2

fun captureFromArguments(
        type: SimpleType,
        status: CaptureStatus,
        acceptNewCapturedType: ((argumentIndex: Int, NewCapturedType) -> Unit) = DO_NOTHING_2
): SimpleType {
    val arguments = type.arguments
    if (arguments.all { it.projectionKind == Variance.INVARIANT }) return type

    val newArguments = arguments.map {
        projection ->
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
            NewKotlinTypeChecker.transformToNewType(substitutor.safeSubstitute(it, Variance.INVARIANT).unwrap())
        }
        if (!oldProjection.isStarProjection && oldProjection.projectionKind == Variance.OUT_VARIANCE) {
            upperBounds += NewKotlinTypeChecker.transformToNewType(oldProjection.type.unwrap())
        }

        val capturedType = newProjection.type as NewCapturedType
        capturedType.constructor.initializeSupertypes(upperBounds)
        acceptNewCapturedType(index, capturedType)
    }

    return KotlinTypeFactory.simpleType(type.annotations, type.constructor, newArguments, type.isMarkedNullable)
}

enum class CaptureStatus {
    FOR_SUBTYPING,
    FROM_EXPRESSION
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
        val lowerType: UnwrappedType?,
        override val annotations: Annotations = Annotations.EMPTY,
        override val isMarkedNullable: Boolean = false
): SimpleType() {
    constructor(captureStatus: CaptureStatus, lowerType: UnwrappedType?, projection: TypeProjection) :
            this(captureStatus, NewCapturedTypeConstructor(projection), lowerType)

    override val arguments: List<TypeProjection> get() = listOf()

    override val memberScope: MemberScope // todo what about foo().bar() where foo() return captured type?
        get() = ErrorUtils.createErrorScope("No member resolution should be done on captured type!", true)

    override fun replaceAnnotations(newAnnotations: Annotations) =
            NewCapturedType(captureStatus, constructor, lowerType, newAnnotations, isMarkedNullable)

    override fun makeNullableAsSpecified(newNullability: Boolean) =
            NewCapturedType(captureStatus, constructor, lowerType, annotations, newNullability)
}

class NewCapturedTypeConstructor(val projection: TypeProjection, private var supertypes: List<UnwrappedType>? = null) : TypeConstructor {
    fun initializeSupertypes(supertypes: List<UnwrappedType>) {
        assert(this.supertypes == null) {
            "Already initialized! oldValue = ${this.supertypes}, newValue = $supertypes"
        }
        this.supertypes = supertypes
    }

    override fun getSupertypes() = supertypes ?: emptyList()
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun isFinal() = false
    override fun isDenotable() = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null
    override fun getBuiltIns(): KotlinBuiltIns = projection.type.builtIns

    override fun toString() = "CapturedType($projection)"
}
