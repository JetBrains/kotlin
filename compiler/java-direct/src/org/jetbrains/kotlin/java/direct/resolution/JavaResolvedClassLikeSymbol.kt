/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.name.ClassId

/**
 * Origin classification of a class-like symbol resolved by the FIR symbol provider.
 *
 * Allows [JavaResolutionContext] to reason about a candidate class's origin (Java source,
 * Java binary, Kotlin, ...) without taking a direct dependency on FIR-internal types.
 * Mirrors the relevant subset of `org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin`.
 */
enum class JavaResolvedClassOrigin {
    /** Java source class — answerable via the java-direct AST. */
    JAVA_SOURCE,

    /** Java binary class (`.class` file on the classpath, including JDK and library jars). */
    JAVA_LIBRARY,

    /** Kotlin class (source or library). */
    KOTLIN,

    /** Anything else (synthetic, plugin-generated, etc.). */
    OTHER,
}

/**
 * Result of a class-like symbol probe via the FIR symbol provider.
 *
 * Threading this through [JavaResolutionContext.resolve] (instead of just a boolean
 * "exists?" answer) preserves the origin information that the FIR side already has at the
 * point of the symbol-provider lookup. The FIR caller currently wraps the lookup in a
 * boolean `tryResolve` callback and discards the origin; carrying the [JavaResolvedClassOrigin]
 * back lets the resolver make origin-sensitive decisions (for example, skipping the AST
 * fast-path when the candidate is known to be a Kotlin class) without re-querying the
 * symbol provider.
 *
 * Stage 1 of the resolver-unification refactoring (see
 * `implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`) introduces this type as the
 * structural counterpart to `tryResolve`. Stage 1 is a pure API addition: the callback
 * is plumbed through [JavaResolutionContext.resolve] but is not yet consumed; subsequent
 * stages of the unification will read [origin] to short-circuit AST-side work.
 */
data class JavaResolvedClassLikeSymbol(
    val classId: ClassId,
    val origin: JavaResolvedClassOrigin,
)
