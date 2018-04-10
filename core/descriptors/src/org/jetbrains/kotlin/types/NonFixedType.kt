/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class NonFixedType(val originalTypeVariable: TypeConstructor) : SimpleType() {
    override val constructor: TypeConstructor =
        ErrorUtils.createErrorTypeConstructor("Constructor for non fixed type: $originalTypeVariable")

    override val arguments: List<TypeProjection>
        get() = emptyList()

    override val isMarkedNullable: Boolean
        get() = false

    override val memberScope: MemberScope =
        ErrorUtils.createErrorScope("Scope for non fixed type: $originalTypeVariable")

    override val annotations: Annotations
        get() = Annotations.EMPTY

    override fun replaceAnnotations(newAnnotations: Annotations): SimpleType = this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType = this

    override fun toString(): String {
        return "NonFixed: $originalTypeVariable"
    }
}