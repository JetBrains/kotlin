/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

/**
 * Represents the deprecation status of a symbol.
 *
 * @see deprecation
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDeprecation {
    /** The deprecation level. */
    public val level: KaDeprecationLevel

    /** Whether this deprecation propagates to overriding members. */
    public val isPropagatedToOverrides: Boolean
}

/**
 * Deprecation severity levels, corresponding to [DeprecationLevel] in the Kotlin standard library.
 *
 * The levels form a progression from least to most restrictive: [WARNING] produces a compiler warning, [ERROR] produces a compiler
 * error, and [HIDDEN] makes the declaration invisible to new code.
 */
@KaExperimentalApi
public class KaDeprecationLevel private constructor(public val name: String) {
    @KaExperimentalApi
    public companion object {
        /**
         * The deprecated symbol can still be used, but a warning is reported at the call site.
         *
         * Corresponds to [DeprecationLevel.WARNING].
         */
        @JvmField
        public val WARNING: KaDeprecationLevel = KaDeprecationLevel("WARNING")

        /**
         * The deprecated symbol can still be referenced, but a compilation error is reported at the call site.
         *
         * Corresponds to [DeprecationLevel.ERROR].
         */
        @JvmField
        public val ERROR: KaDeprecationLevel = KaDeprecationLevel("ERROR")

        /**
         * The deprecated symbol is no longer visible to new code.
         * Existing compiled code that references it will still work, but new source code cannot reference it.
         *
         * Corresponds to [DeprecationLevel.HIDDEN].
         */
        @JvmField
        public val HIDDEN: KaDeprecationLevel = KaDeprecationLevel("HIDDEN")
    }

    override fun toString(): String = name
}
