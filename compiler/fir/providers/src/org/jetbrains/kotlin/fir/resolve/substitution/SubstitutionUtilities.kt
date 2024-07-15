/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.types.*

inline fun ConeCapturedType.substitute(f: (ConeKotlinType) -> ConeKotlinType?): ConeCapturedType? {
    val innerType = this.lowerType ?: this.constructor.projection.type
    // TODO(KT-64024): This early return looks suspicious.
    //  In fact, if the inner type wasn't substituted we will ignore potential substitution in
    //  super types
    val substitutedInnerType = innerType?.let(f) ?: return null
    if (substitutedInnerType is ConeCapturedType) return substitutedInnerType
    val substitutedSuperTypes =
        this.constructor.supertypes?.map { f(it) ?: it }

    // TODO(KT-64027): Creation of new captured types creates unexpected behavior by breaking substitution consistency.
    //  E.g:
    //  ```
    //   substitution = { A => B }
    //   substituteOrSelf(C<CapturedType(out A)_0>) -> C<CapturedType(out B)_1>
    //   substituteOrSelf(C<CapturedType(out A)_0>) -> C<CapturedType(out B)_2>
    //   C<CapturedType(out B)_1> <!:> C<CapturedType(out B)_2>
    //  ```

    return copy(
        constructor = ConeCapturedTypeConstructor(
            wrapProjection(constructor.projection, substitutedInnerType),
            substitutedSuperTypes,
            typeParameterMarker = constructor.typeParameterMarker
        ),
        lowerType = if (lowerType != null) substitutedInnerType else null,
    )
}

fun wrapProjection(old: ConeTypeProjection, newType: ConeKotlinType): ConeTypeProjection {
    return when (old) {
        is ConeStarProjection -> old
        is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
        is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
        is ConeKotlinTypeConflictingProjection -> ConeKotlinTypeConflictingProjection(newType)
        is ConeKotlinType -> newType
        else -> old
    }
}
