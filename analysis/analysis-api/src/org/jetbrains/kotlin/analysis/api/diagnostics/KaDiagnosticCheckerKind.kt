/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.diagnostics

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi

/**
 * A kind of compiler checkers which report [diagnostics][KaDiagnostic].
 *
 * Checker kinds control which checkers are run during diagnostic collection, so requesting more kinds means more work. Use
 * [KaDiagnostics.withCheckers] to select the kinds to run.
 *
 * All instances are provided by the [Companion], so instances can be compared by identity.
 */
@KaExperimentalApi
public class KaDiagnosticCheckerKind private constructor(public val name: String) {
    override fun toString(): String = name

    @KaExperimentalApi
    public companion object {
        /**
         * The compiler's common checkers. Their diagnostics are exactly the ones reported during compilation.
         */
        public val COMMON: KaDiagnosticCheckerKind = KaDiagnosticCheckerKind("COMMON")

        /**
         * Extended checkers, which typically run only in the IDE. They report additional diagnostics which are not a part of the
         * compilation, such as reports about redundant code.
         */
        public val EXTENDED: KaDiagnosticCheckerKind = KaDiagnosticCheckerKind("EXTENDED")

        /**
         * Experimental checkers. Their role is the same as of [EXTENDED] checkers, with the following differences:
         *
         * - They might have false positives.
         * - They might be slow.
         */
        public val EXPERIMENTAL: KaDiagnosticCheckerKind = KaDiagnosticCheckerKind("EXPERIMENTAL")

        /**
         * All checker kinds supported by the current version of the Analysis API.
         *
         * **Caution:** The set may grow in future versions, so by requesting [ALL], the client opts in to diagnostics and performance costs
         * of checker kinds which do not exist yet. Prefer listing the required kinds explicitly.
         */
        public val ALL: Set<KaDiagnosticCheckerKind> = setOf(COMMON, EXTENDED, EXPERIMENTAL)
    }
}
