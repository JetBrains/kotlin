/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.StubTypeMarker
import org.jetbrains.kotlin.types.refinement.TypeRefinement

// This type is used as a stub for postponed type variables, which are important for coroutine inference
class StubType(
    originalTypeVariable: TypeConstructor,
    isMarkedNullable: Boolean,
    constructor: TypeConstructor = ErrorUtils.createErrorTypeConstructor("Constructor for non fixed type: $originalTypeVariable"),
    memberScope: MemberScope = ErrorUtils.createErrorScope("Scope for non fixed type: $originalTypeVariable")
) : AbstractStubType(originalTypeVariable, isMarkedNullable, constructor, memberScope), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType {
        return StubType(originalTypeVariable, newNullability, constructor, memberScope)
    }
}

class StubTypeForTypeVariablesInSubtyping(
    originalTypeVariable: TypeConstructor,
    isMarkedNullable: Boolean,
    constructor: TypeConstructor = ErrorUtils.createErrorTypeConstructor("Constructor for non fixed type: $originalTypeVariable"),
    memberScope: MemberScope = ErrorUtils.createErrorScope("Scope for non fixed type: $originalTypeVariable")
) : AbstractStubType(originalTypeVariable, isMarkedNullable, constructor, memberScope), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType {
        return StubTypeForTypeVariablesInSubtyping(originalTypeVariable, newNullability, constructor, memberScope)
    }
}

// This type is used as a replacement of type variables for provideDelegate resolve
class StubTypeForProvideDelegateReceiver(
    originalTypeVariable: TypeConstructor,
    isMarkedNullable: Boolean,
    constructor: TypeConstructor = ErrorUtils.createErrorTypeConstructor("Constructor for non fixed type: $originalTypeVariable"),
    memberScope: MemberScope = ErrorUtils.createErrorScope("Scope for non fixed type: $originalTypeVariable")
) : AbstractStubType(originalTypeVariable, isMarkedNullable, constructor, memberScope) {
    override fun materialize(newNullability: Boolean): StubTypeForProvideDelegateReceiver {
        return StubTypeForProvideDelegateReceiver(originalTypeVariable, newNullability, constructor, memberScope)
    }
}

abstract class AbstractStubType(
    val originalTypeVariable: TypeConstructor,
    override val isMarkedNullable: Boolean,
    override val constructor: TypeConstructor,
    override val memberScope: MemberScope
) : SimpleType() {
    override val arguments: List<TypeProjection>
        get() = emptyList()

    override val annotations: Annotations
        get() = Annotations.EMPTY

    override fun replaceAnnotations(newAnnotations: Annotations): SimpleType = this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        return if (newNullability == isMarkedNullable) this else materialize(newNullability)
    }

    override fun toString(): String {
        return "NonFixed: $originalTypeVariable${if (isMarkedNullable) "?" else ""}"
    }

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = this

    abstract fun materialize(newNullability: Boolean): AbstractStubType
}