/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
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
 * Can be built by [org.jetbrains.kotlin.analysis.api.components.buildSubstitutor] or retrieved via a [org.jetbrains.kotlin.analysis.api.calls.KtCall]
 */
public interface KtSubstitutor : KtLifetimeOwner {
    /**
     * substitutes type parameters in a given type corresponding to internal mapping rules.
     *
     * @return substituted type if there was at least one substitution, [type] itself if there was no type parameter to substitute
     */
    public fun substitute(type: KtType): KtType = withValidityAssertion { substituteOrNull(type) ?: type }

    /**
     * substitutes type parameters in a given type corresponding to internal mapping rules.
     *
     * @return substituted type if there was at least one substitution, `null` if there was no type parameter to substitute
     */
    public fun substituteOrNull(type: KtType): KtType?

    /**
     * [KtSubstitutor] which does nothing on a type and always returns the type intact
     */
    public class Empty(override val token: KtLifetimeToken) : KtSubstitutor {
        override fun substituteOrNull(type: KtType): KtType? = withValidityAssertion { null }

        override fun substitute(type: KtType): KtType = withValidityAssertion { type }
    }
}
