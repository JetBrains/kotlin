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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

open class ErrorType @JvmOverloads internal constructor(
    override val constructor: TypeConstructor,
    override val memberScope: MemberScope,
    override val arguments: List<TypeProjection> = emptyList(),
    override val isMarkedNullable: Boolean = false,
    open val presentableName: String = "???"
) : SimpleType() {
    override val annotations: Annotations
        get() = Annotations.EMPTY

    override fun toString(): String =
            constructor.toString() + if (arguments.isEmpty()) "" else arguments.joinToString(", ", "<", ">", -1, "...", null)

    override fun replaceAnnotations(newAnnotations: Annotations): SimpleType = this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType =
            ErrorType(constructor, memberScope, arguments, newNullability)

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = this
}

class UnresolvedType(
    override val presentableName: String,
    constructor: TypeConstructor,
    memberScope: MemberScope,
    arguments: List<TypeProjection>,
    isMarkedNullable: Boolean
) : ErrorType(constructor, memberScope, arguments, isMarkedNullable) {
    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType =
        UnresolvedType(presentableName, constructor, memberScope, arguments, newNullability)

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = this
}
