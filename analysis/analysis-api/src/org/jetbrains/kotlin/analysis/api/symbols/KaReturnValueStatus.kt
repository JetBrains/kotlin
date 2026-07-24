/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi

/**
 * The return value status of the function (should it be used, or can it be ignored).
 * @see returnValueStatus
 */
@KaExperimentalApi
public sealed class KaReturnValueStatus(public val name: String) {
    override fun toString(): String = name

    /**
     * The return value of the function must be checked for usage.
     */
    @KaExperimentalApi
    public data object MustUse : KaReturnValueStatus("MustUse")

    /**
     * The return value of the function is declared as explicitly ignorable and should not be checked for usage.
     */
    @KaExperimentalApi
    public data object ExplicitlyIgnorable : KaReturnValueStatus("ExplicitlyIgnorable")

    /**
     * The return value status of the function is unspecified.
     */
    @KaExperimentalApi
    public data object Unspecified : KaReturnValueStatus("Unspecified")

    /**
     * A dummy private subclass to force 'else' branches in client code
     */
    @Suppress("unused")
    @KaExperimentalApi
    private data object Unknown : KaReturnValueStatus("Unknown")
}
