/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol

/**
 * A DSL builder for [KaSubstitutor].
 *
 * @see KaSubstitutor
 * @see buildSubstitutor
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSubstitutorBuilder : KaLifetimeOwner {
    /**
     * A map of the type substitutions that have so far been accumulated by the builder.
     */
    public val mappings: Map<KaTypeParameterSymbol, KaType>

    /**
     * Adds a new [typeParameter] -> [type] substitution to the substitutor.
     *
     * If there already was a substitution with a [typeParameter], the function replaces the corresponding substitution with a new one.
     */
    public fun substitution(typeParameter: KaTypeParameterSymbol, type: KaType)

    /**
     * Adds new [KaTypeParameterSymbol] -> [KaType] substitutions to the substitutor.
     *
     * If there already was a substitution with a [KaTypeParameterSymbol], the function replaces the corresponding substitution with a new
     * one.
     */
    public fun substitutions(substitutions: Map<KaTypeParameterSymbol, KaType>)
}
