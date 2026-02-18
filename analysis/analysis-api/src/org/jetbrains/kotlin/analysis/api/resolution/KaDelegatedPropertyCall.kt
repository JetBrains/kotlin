/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol

/**
 * Represents a resolved delegated property, which desugars into up to three operator calls:
 * [getValue()][valueGetterCall], [setValue()][valueSetterCall], and [provideDelegate()][provideDelegateCall].
 *
 * #### Example
 *
 * ```kotlin
 * val name: String by lazy { "John" }
 * ```
 *
 * A delegated `val` property desugars to:
 * ```kotlin
 * val name$delegate = lazy { "John" }
 * val name: String get() = name$delegate.getValue(thisRef, property)
 * ```
 *
 * For a `var` property, a `setValue` call is also generated.
 * If the delegate expression defines a `provideDelegate` operator, it is called first.
 *
 * @see KaMultiCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDelegatedPropertyCall : KaMultiCall, KaCall {
    /** The `getValue()` operator call on the delegate object. */
    public val valueGetterCall: KaFunctionCall<KaNamedFunctionSymbol>

    /** The `setValue()` operator call on the delegate object. `null` for `val` properties. */
    public val valueSetterCall: KaFunctionCall<KaNamedFunctionSymbol>?

    /** The `provideDelegate()` operator call. `null` if not applicable. */
    public val provideDelegateCall: KaFunctionCall<KaNamedFunctionSymbol>?
}
