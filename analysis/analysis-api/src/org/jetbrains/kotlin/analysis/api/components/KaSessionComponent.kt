/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

/**
 * Marks the legacy session components of a [KaSession][org.jetbrains.kotlin.analysis.api.KaSession].
 *
 * Session components mix their functions and properties into [KaSession][org.jetbrains.kotlin.analysis.api.KaSession], so they are
 * available wherever the session is a receiver. They are superseded by the context-parameter API: top-level `context(session: KaSession)`
 * declarations in the domain packages (`types`, `symbols`, `scopes`, …), which are available inside an [analyze][org.jetbrains.kotlin.analysis.api.session.analyze]
 * block and in any function with a [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] context parameter.
 *
 * Session components and their members are deprecated and will be removed.
 */
@Target(AnnotationTarget.CLASS)
@RequiresOptIn(
    "The session component is an implementation detail of the Analysis API and doesn't provide compatibility guarantees, so it should not be used directly. Access its members via `KaSession` or context parameter bridges instead.",
    level = RequiresOptIn.Level.WARNING,
)
internal annotation class KaSessionComponentImplementationDetail
