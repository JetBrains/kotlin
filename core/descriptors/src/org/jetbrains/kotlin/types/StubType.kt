/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope

// This type is used as a stub for postponed type variables, which are important for coroutine inference
class StubType(
    private val originalTypeVariable: TypeConstructor,
    override val isMarkedNullable: Boolean,
    override val constructor: TypeConstructor =
        ErrorUtils.createErrorTypeConstructor("Constructor for non fixed type: $originalTypeVariable"),
    override val memberScope: MemberScope =
        ErrorUtils.createErrorScope("Scope for non fixed type: $originalTypeVariable")
) : SimpleType() {

    override val arguments: List<TypeProjection>
        get() = emptyList()

    override val annotations: Annotations
        get() = Annotations.EMPTY

    override fun replaceAnnotations(newAnnotations: Annotations): SimpleType {
        error("Shouldn't be called on non-fixed type")
    }

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        return if (newNullability == isMarkedNullable)
            this
        else
            StubType(originalTypeVariable, newNullability, constructor, memberScope)
    }

    override fun toString(): String {
        return "NonFixed: $originalTypeVariable"
    }
}