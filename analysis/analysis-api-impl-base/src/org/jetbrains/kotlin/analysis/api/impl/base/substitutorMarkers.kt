/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType

/**
 * A [KtSubstitutor] which substitution logic can be represented as a [Map] from a [KtTypeParameterSymbol] to corresponding [KtType]
 * This is an implementation details and Analysis API clients should not depend on the fact if some [KtSubstitutor] is [KtMapBackedSubstitutor] or not.
 */
@KtAnalysisApiInternals
interface KtMapBackedSubstitutor : KtSubstitutor {
    /**
     * Substitution rules in a form of a `Map<KtTypeParameterSymbol, KtType>`
     */
    fun getAsMap(): Map<KtTypeParameterSymbol, KtType>
}


/**
 * A [KtSubstitutor] which substitution logic can be represented as subsequent invocation of two substitutors [first] and [second]
 * This is an implementation detail,
 * and Analysis API clients should not depend on the fact if some [KtSubstitutor] is [KtChainedSubstitutor] or not.
 */
@KtAnalysisApiInternals
interface KtChainedSubstitutor : KtSubstitutor {
    val first: KtSubstitutor
    val second: KtSubstitutor
}
