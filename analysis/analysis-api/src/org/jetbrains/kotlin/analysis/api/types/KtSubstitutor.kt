/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * Type substitutor. Performs substitution of a type parameters inside a type to some fixed type.
 * Usually can be represented as a map from type parameter to it corresponding substitution.
 *
 * Example of a substitution:
 * ```
 * substitutor = { T -> Int, S -> Long }
 * substitute(Map<T, S>, substitutor) = Map<Int, Long>
 * ```
 *
 * Can be built by [org.jetbrains.kotlin.analysis.api.components.buildSubstitutor] or retrieved via a [org.jetbrains.kotlin.analysis.api.calls.KaCall]
 */
public interface KaSubstitutor : KaLifetimeOwner {
    /**
     * substitutes type parameters in a given type corresponding to internal mapping rules.
     *
     * @return substituted type if there was at least one substitution, [type] itself if there was no type parameter to substitute
     */
    public fun substitute(type: KaType): KaType = withValidityAssertion { substituteOrNull(type) ?: type }

    /**
     * substitutes type parameters in a given type corresponding to internal mapping rules.
     *
     * @return substituted type if there was at least one substitution, `null` if there was no type parameter to substitute
     */
    public fun substituteOrNull(type: KaType): KaType?

    /**
     * [KaSubstitutor] which does nothing on a type and always returns the type intact
     */
    public class Empty(override val token: KaLifetimeToken) : KaSubstitutor {
        override fun substituteOrNull(type: KaType): KaType? = withValidityAssertion { null }

        override fun substitute(type: KaType): KaType = withValidityAssertion { type }
    }
}
