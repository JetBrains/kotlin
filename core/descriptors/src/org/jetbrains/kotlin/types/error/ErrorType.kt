/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class ErrorType @JvmOverloads internal constructor(
    override val constructor: TypeConstructor,
    override val memberScope: MemberScope,
    val kind: ErrorTypeKind,
    override val arguments: List<TypeProjection> = emptyList(),
    override val isMarkedNullable: Boolean = false,
    private vararg val formatParams: String
) : SimpleType() {
    val debugMessage = String.format(kind.debugMessage, *formatParams)

    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType = this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType =
            ErrorType(constructor, memberScope, kind, arguments, newNullability, *formatParams)

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = this
}
