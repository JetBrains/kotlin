/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol

/**
 * Represents a resolved `for` loop, which desugars into three operator calls:
 * [iterator()][iteratorCall], [hasNext()][hasNextCall], and [next()][nextCall].
 *
 * #### Example
 *
 * ```kotlin
 * for (item in list) {
 *     println(item)
 * }
 * ```
 *
 * A `for` loop over `list` desugars to:
 * ```kotlin
 * val iterator = list.iterator()
 * while (iterator.hasNext()) {
 *     val item = iterator.next()
 *     println(item)
 * }
 * ```
 *
 * @see KaMultiCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaForLoopCall : KaMultiCall, KaCall {
    /** The `iterator()` call on the loop range expression. */
    public val iteratorCall: KaFunctionCall<KaNamedFunctionSymbol>

    /** The `hasNext()` call on the iterator. */
    public val hasNextCall: KaFunctionCall<KaNamedFunctionSymbol>

    /** The `next()` call on the iterator. */
    public val nextCall: KaFunctionCall<KaNamedFunctionSymbol>
}
