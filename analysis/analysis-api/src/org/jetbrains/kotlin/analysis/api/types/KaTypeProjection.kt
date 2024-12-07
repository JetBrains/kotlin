/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.types.Variance

/**
 * [KaTypeProjection] represents a type argument used in the context of a class or function type. It provides information about the type and
 * its variance.
 */
public sealed interface KaTypeProjection : KaLifetimeOwner {
    /**
     * The projected type, or `null` for the star projection.
     */
    public val type: KaType?
}

/**
 * Represents a [star projection](https://kotlinlang.org/docs/generics.html#star-projections) (`*`) used in type arguments. It indicates
 * that the specific type argument is not important or unknown.
 */
public interface KaStarTypeProjection : KaTypeProjection {
    override val type: KaType? get() = withValidityAssertion { null }
}

/**
 * Represents a type argument with an explicit type and [variance](https://kotlinlang.org/docs/generics.html#use-site-variance-type-projections).
 */
public interface KaTypeArgumentWithVariance : KaTypeProjection {
    /**
     * The projected type.
     */
    override val type: KaType

    /**
     * The [Variance] of the type argument. It can be:
     *
     * - [OUT_VARIANCE][Variance.OUT_VARIANCE]: The type argument is used as a covariant, `out` type parameter.
     * - [IN_VARIANCE][Variance.IN_VARIANCE]: The type argument is used as a contravariant, `in` type parameter.
     * - [INVARIANT][Variance.INVARIANT]: The type argument is used as an invariant type parameter.
     */
    public val variance: Variance
}
