/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSubstitutorProvider : KaSessionComponent {
    /**
     * Creates a [KaSubstitutor] based on the given [mappings].
     *
     * Usually, [buildSubstitutor] should be preferred to build a new substitutor from scratch.
     *
     * @see KaSubstitutor
     */
    @KaExperimentalApi
    public fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor

    /**
     * Creates a [KaSubstitutor] based on the inheritance relationship between [subClass] and [superClass]. [subClass] must inherit from
     * [superClass] and there may not be any error types in the inheritance path. Otherwise, `null` is returned.
     *
     * The semantics of the resulting [KaSubstitutor] are as follows: When applied to a member of [superClass], such as a function, its type
     * parameters are substituted in such a way that the resulting member can be used with an instance of [subClass].
     *
     * In other words, the substitutor is a composition of inheritance-based substitutions incorporating the whole inheritance chain.
     *
     * #### Example
     *
     * ```
     * class A : B<String>
     * class B<T> : C<T, Int>
     * class C<X, Y>
     * ```
     *
     * - `createInheritanceTypeSubstitutor(A, B)` returns `KtSubstitutor { T -> String }`
     * - `createInheritanceTypeSubstitutor(B, C)` returns `KtSubstitutor { X -> T, Y -> Int }`
     * - `createInheritanceTypeSubstitutor(A, C)` returns `KtSubstitutor { X -> T, Y -> Int } andThen KtSubstitutor { T -> String }`
     */
    @KaExperimentalApi
    public fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor?

    /**
     * Creates a [KaSubstitutor] that, when applied to the type parameters of [subClass], produces a type that is a subtype of [superType].
     *
     * This is useful when you have a concrete supertype and want to determine how to instantiate a subclass to match that supertype.
     *
     * Returns `null` if:
     * - [subClass] does not inherit from the class of [superType]
     * - There are error types in the inheritance path
     * - The type argument mapping is ambiguous (e.g., the same type parameter maps to different types)
     *
     * **Note:** If [subClass] has type parameters that do not appear in the inheritance path to [superType], those type parameters
     * will not be included in the resulting substitutor (they remain unsubstituted).
     *
     * #### Example
     *
     * ```
     * interface A<T> : B<T>
     * interface B<T> : C<Int, T>
     * interface C<X, Y>
     * ```
     *
     * - `createSubtypingSubstitutor(A, C<Int, String>)` returns `KaSubstitutor { T -> String }`
     * - `createSubtypingSubstitutor(B, C<Int, String>)` returns `KaSubstitutor { T -> String }`
     *
     * #### Free type parameters
     *
     * ```
     * interface A<T, U> : B<T>
     * interface B<X>
     * ```
     *
     * - `createSubtypingSubstitutor(A, B<Int>)` returns `KaSubstitutor { T -> Int }` (U remains unsubstituted)
     *
     * @param subClass The subclass whose type parameters should be substituted.
     * @param superType The target supertype that the substituted subclass type should match.
     * @return A substitutor mapping [subClass]'s type parameters to concrete types, or `null` if the operation fails.
     */
    @KaExperimentalApi
    @KaK1Unsupported
    public fun createSubtypingSubstitutor(subClass: KaClassSymbol, superType: KaClassType): KaSubstitutor?
}

/**
 * Builds a new [KaSubstitutor] from substitutions specified inside [build].
 */
@KaExperimentalApi
@OptIn(ExperimentalContracts::class, KaImplementationDetail::class)
@JvmName("buildSubstitutorExtension")
public inline fun KaSession.buildSubstitutor(
    build: KaSubstitutorBuilder.() -> Unit,
): KaSubstitutor {
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }
    return createSubstitutor(KaSubstitutorBuilder(token).apply(build).mappings)
}

/**
 * Builds a new [KaSubstitutor] from substitutions specified inside [build].
 */
@KaCustomContextParameterBridge
@KaExperimentalApi
context(session: KaSession)
public inline fun buildSubstitutor(
    build: KaSubstitutorBuilder.() -> Unit,
): KaSubstitutor {
    @OptIn(ExperimentalContracts::class)
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }

    return session.buildSubstitutor(build)
}

@KaExperimentalApi
@OptIn(KaImplementationDetail::class)
public class KaSubstitutorBuilder
@KaImplementationDetail constructor(override val token: KaLifetimeToken) : KaLifetimeOwner {
    private val backingMapping = mutableMapOf<KaTypeParameterSymbol, KaType>()

    /**
     * A map of the type substitutions that have so far been accumulated by the builder.
     */
    public val mappings: Map<KaTypeParameterSymbol, KaType>
        get() = withValidityAssertion { backingMapping }

    /**
     * Adds a new [typeParameter] -> [type] substitution to the substitutor.
     *
     * If there already was a substitution with a [typeParameter], the function replaces the corresponding substitution with a new one.
     */
    public fun substitution(typeParameter: KaTypeParameterSymbol, type: KaType): Unit = withValidityAssertion {
        backingMapping[typeParameter] = type
    }

    /**
     * Adds new [KaTypeParameterSymbol] -> [KaType] substitutions to the substitutor.
     *
     * If there already was a substitution with a [KaTypeParameterSymbol], the function replaces the corresponding substitution with a new
     * one.
     */
    public fun substitutions(substitutions: Map<KaTypeParameterSymbol, KaType>): Unit = withValidityAssertion {
        backingMapping += substitutions
    }
}

/**
 * Creates a [KaSubstitutor] based on the given [mappings].
 *
 * Usually, [buildSubstitutor] should be preferred to build a new substitutor from scratch.
 *
 * @see KaSubstitutor
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor {
    return with(session) {
        createSubstitutor(
            mappings = mappings,
        )
    }
}

/**
 * Creates a [KaSubstitutor] based on the inheritance relationship between [subClass] and [superClass]. [subClass] must inherit from
 * [superClass] and there may not be any error types in the inheritance path. Otherwise, `null` is returned.
 *
 * The semantics of the resulting [KaSubstitutor] are as follows: When applied to a member of [superClass], such as a function, its type
 * parameters are substituted in such a way that the resulting member can be used with an instance of [subClass].
 *
 * In other words, the substitutor is a composition of inheritance-based substitutions incorporating the whole inheritance chain.
 *
 * #### Example
 *
 * ```
 * class A : B<String>
 * class B<T> : C<T, Int>
 * class C<X, Y>
 * ```
 *
 * - `createInheritanceTypeSubstitutor(A, B)` returns `KtSubstitutor { T -> String }`
 * - `createInheritanceTypeSubstitutor(B, C)` returns `KtSubstitutor { X -> T, Y -> Int }`
 * - `createInheritanceTypeSubstitutor(A, C)` returns `KtSubstitutor { X -> T, Y -> Int } andThen KtSubstitutor { T -> String }`
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor? {
    return with(session) {
        createInheritanceTypeSubstitutor(
            subClass = subClass,
            superClass = superClass,
        )
    }
}

/**
 * Creates a [KaSubstitutor] that, when applied to the type parameters of [subClass], produces a type that is a subtype of [superType].
 *
 * This is useful when you have a concrete supertype and want to determine how to instantiate a subclass to match that supertype.
 *
 * Returns `null` if:
 * - [subClass] does not inherit from the class of [superType]
 * - There are error types in the inheritance path
 * - The type argument mapping is ambiguous (e.g., the same type parameter maps to different types)
 *
 * **Note:** If [subClass] has type parameters that do not appear in the inheritance path to [superType], those type parameters
 * will not be included in the resulting substitutor (they remain unsubstituted).
 *
 * #### Example
 *
 * ```
 * interface A<T> : B<T>
 * interface B<T> : C<Int, T>
 * interface C<X, Y>
 * ```
 *
 * - `createSubtypingSubstitutor(A, C<Int, String>)` returns `KaSubstitutor { T -> String }`
 * - `createSubtypingSubstitutor(B, C<Int, String>)` returns `KaSubstitutor { T -> String }`
 *
 * #### Free type parameters
 *
 * ```
 * interface A<T, U> : B<T>
 * interface B<X>
 * ```
 *
 * - `createSubtypingSubstitutor(A, B<Int>)` returns `KaSubstitutor { T -> Int }` (U remains unsubstituted)
 *
 * @param subClass The subclass whose type parameters should be substituted.
 * @param superType The target supertype that the substituted subclass type should match.
 * @return A substitutor mapping [subClass]'s type parameters to concrete types, or `null` if the operation fails.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaK1Unsupported
@KaContextParameterApi
context(session: KaSession)
public fun createSubtypingSubstitutor(subClass: KaClassSymbol, superType: KaClassType): KaSubstitutor? {
    return with(session) {
        createSubtypingSubstitutor(
            subClass = subClass,
            superType = superType,
        )
    }
}
