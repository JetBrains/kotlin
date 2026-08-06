/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

/**
 * Marks a diagnostic as unstable. The diagnostic is intended for user consumption, but it has no compatibility guarantees and might change
 * at any moment, or even be removed, without a deprecation cycle.
 *
 * Diagnostics mirror the ones reported by the compiler, and the Analysis API cannot control that set: a diagnostic may be renamed, split,
 * merged, or gain and lose parameters whenever the compiler's checkers change. Therefore, unlike the rest of the Analysis API surface, no
 * particular diagnostic can be guaranteed to survive.
 *
 * The opt-in level is a warning only to give existing clients time to migrate. It is meant to be raised to an error once they have.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
)
@RequiresOptIn(
    message = "Diagnostics have no compatibility guarantees, as they follow the compiler's diagnostics, which the Analysis API does not control",
    level = RequiresOptIn.Level.WARNING,
)
public annotation class KaUnstableDiagnosticApi
