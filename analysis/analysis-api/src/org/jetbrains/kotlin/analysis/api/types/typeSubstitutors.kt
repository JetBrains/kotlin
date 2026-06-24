/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaUnificationSubstitutorPolicy
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol

/**
 * Creates a [KaSubstitutor] based on the given [mappings].
 *
 * Usually, [buildSubstitutor] should be preferred to build a new substitutor from scratch.
 *
 * @see KaSubstitutor
 */
@KaExperimentalApi
context(session: KaSession)
public fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor {
    @OptIn(KaImplementationDetail::class)
    return internals.substitutorProvider.createSubstitutor(mappings)
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
 * - `createInheritanceTypeSubstitutor(A, B)` returns `KaSubstitutor { T -> String }`
 * - `createInheritanceTypeSubstitutor(B, C)` returns `KaSubstitutor { X -> T, Y -> Int }`
 * - `createInheritanceTypeSubstitutor(A, C)` returns `KaSubstitutor { X -> T, Y -> Int } and then KaSubstitutor { T -> String }`
 */
@KaExperimentalApi
context(session: KaSession)
public fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor? {
    @OptIn(KaImplementationDetail::class)
    return internals.substitutorProvider.createInheritanceTypeSubstitutor(subClass, superClass)
}
