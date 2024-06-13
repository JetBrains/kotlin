/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * A [KaSubstitutor] which substitution logic can be represented as a [Map] from a [KaTypeParameterSymbol] to corresponding [KaType]
 * This is an implementation details and Analysis API clients should not depend on the fact if some [KaSubstitutor] is [KaMapBackedSubstitutor] or not.
 */
@KaImplementationDetail
interface KaMapBackedSubstitutor : KaSubstitutor {
    /**
     * Substitution rules in a form of a `Map<KaTypeParameterSymbol, KaType>`
     */
    fun getAsMap(): Map<KaTypeParameterSymbol, KaType>
}


/**
 * A [KaSubstitutor] which substitution logic can be represented as subsequent invocation of two substitutors [first] and [second]
 * This is an implementation detail,
 * and Analysis API clients should not depend on the fact if some [KaSubstitutor] is [KaChainedSubstitutor] or not.
 */
@KaImplementationDetail
interface KaChainedSubstitutor : KaSubstitutor {
    val first: KaSubstitutor
    val second: KaSubstitutor
}
