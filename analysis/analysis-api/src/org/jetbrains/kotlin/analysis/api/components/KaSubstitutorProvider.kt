/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KaExperimentalApi
public interface KaSubstitutorProvider {
    /**
     * Creates a [KaSubstitutor] based on the inheritance relationship between [subClass] and [superClass].
     *
     * The semantic of resulted [KaSubstitutor] is the substitutor that should be applied to a member of [superClass],
     * so it can be called on an instance of [subClass].
     *
     * Basically, it's a composition of inheritance-based substitutions for all the inheritance chain.
     *
     * On the following code:
     * ```
     * class A : B<String>
     * class B<T> : C<T, Int>
     * class C<X, Y>
     * ```
     *
     * * `createInheritanceTypeSubstitutor(A, B)` returns `KtSubstitutor {T -> String}`
     * * `createInheritanceTypeSubstitutor(B, C)` returns `KtSubstitutor {X -> T, Y -> Int}`
     * * `createInheritanceTypeSubstitutor(A, C)` returns `KtSubstitutor {X -> T, Y -> Int} andThen KtSubstitutor {T -> String}`
     *
     * @param subClass the subClass or object symbol.
     * @param superClass the super class symbol.
     * @return [KaSubstitutor] if [subClass] inherits [superClass] and there are no error types in the inheritance path. Returns `null` otherwise.
     */
    @KaExperimentalApi
    public fun createInheritanceTypeSubstitutor(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): KaSubstitutor?

    @KaExperimentalApi
    public fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor
}

/**
 * Creates new [KaSubstitutor] using substitutions specified inside [build] lambda
 */
@KaExperimentalApi
@OptIn(ExperimentalContracts::class, KaAnalysisApiInternals::class)
public inline fun KaSession.buildSubstitutor(
    build: KaSubstitutorBuilder.() -> Unit,
): KaSubstitutor {
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }
    return createSubstitutor(KaSubstitutorBuilder(token).apply(build).mappings)
}

@KaExperimentalApi
public class KaSubstitutorBuilder
@KaAnalysisApiInternals constructor(override val token: KaLifetimeToken) : KaLifetimeOwner {
    private val backingMapping = mutableMapOf<KaTypeParameterSymbol, KaType>()

    public val mappings: Map<KaTypeParameterSymbol, KaType> get() = withValidityAssertion { backingMapping }

    /**
     * Adds a new [typeParameter] -> [type] substitution to the substitutor which is being built.
     * If there already was a substitution with a [typeParameter], replaces corresponding substitution with a new one.
     */
    public fun substitution(typeParameter: KaTypeParameterSymbol, type: KaType): Unit = withValidityAssertion {
        backingMapping[typeParameter] = type
    }

    /**
     * Adds a new substitutions to the substitutor which is being built.
     * If there already was a substitution with a [KaTypeParameterSymbol] which is present in a [substitutions],
     * replaces corresponding substitution with a new one.
     */
    public fun substitutions(substitutions: Map<KaTypeParameterSymbol, KaType>): Unit = withValidityAssertion {
        backingMapping += substitutions
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaSubstitutorBuilder' instead.", replaceWith = ReplaceWith("KaSubstitutorBuilder"))
public typealias KtSubstitutorBuilder = KaSubstitutorBuilder