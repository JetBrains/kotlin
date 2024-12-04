/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * A type substitutor which performs a substitution of type parameters inside a type to another type. The substitution can usually be
 * represented as a map from type parameters to their corresponding substitution types.
 *
 * A substitutor can be built using [buildSubstitutor][org.jetbrains.kotlin.analysis.api.components.buildSubstitutor] or retrieved via a
 * [KaCall][org.jetbrains.kotlin.analysis.api.resolution.KaCall].
 *
 * #### Example
 *
 * ```
 * substitutor = { T -> Int, S -> Long }
 * substitute(Map<T, S>, substitutor) = Map<Int, Long>
 * ```
 */
@KaExperimentalApi
public interface KaSubstitutor : KaLifetimeOwner {
    /**
     * Substitutes the type parameters in [type] using the substitutor's type mapping.
     *
     * @return The substituted type if there was at least one substitution, or [type] itself otherwise.
     */
    @KaExperimentalApi
    public fun substitute(type: KaType): KaType = withValidityAssertion { substituteOrNull(type) ?: type }

    /**
     * Substitutes the type parameters in [type] using the substitutor's type mapping.
     *
     * @return The substituted type if there was at least one substitution, or `null` otherwise.
     */
    @KaExperimentalApi
    public fun substituteOrNull(type: KaType): KaType?

    /**
     * A [KaSubstitutor] with an empty type mapping. It does not perform any substitution.
     */
    @KaExperimentalApi
    public class Empty(override val token: KaLifetimeToken) : KaSubstitutor {
        override fun substitute(type: KaType): KaType = withValidityAssertion { type }
        override fun substituteOrNull(type: KaType): KaType? = withValidityAssertion { null }
    }
}
