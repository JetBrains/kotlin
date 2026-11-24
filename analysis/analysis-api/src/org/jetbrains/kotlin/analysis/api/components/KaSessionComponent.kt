/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

/**
 * A legacy component of a [KaSession][org.jetbrains.kotlin.analysis.api.KaSession].
 *
 * Session components mix their functions and properties into [KaSession][org.jetbrains.kotlin.analysis.api.KaSession], so they are
 * available wherever the session is a receiver. They are superseded by the context-parameter API: top-level `context(session: KaSession)`
 * declarations in the domain packages (`types`, `symbols`, `scopes`, …), which are available inside an [analyze][org.jetbrains.kotlin.analysis.api.session.analyze]
 * block and in any function with a [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] context parameter.
 *
 * Session components and their members are deprecated and will be removed.
 */
@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSessionComponent : KaLifetimeOwner

/**
 * All [KaSessionComponent]s (except [KaSession][org.jetbrains.kotlin.analysis.api.KaSession]) are not supposed to be used directly –
 * they are an implementation detail of the Analysis API.
 *
 * Their members are supposed to be used via a [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] instance.
 *
 * @see KaSessionComponent
 */
@Target(AnnotationTarget.CLASS)
@RequiresOptIn(
    "The session component is an implementation detail of the Analysis API and doesn't provide compatibility guarantees, so it should not be used directly. Access its members via `KaSession` or context parameter bridges instead.",
    level = RequiresOptIn.Level.WARNING,
)
internal annotation class KaSessionComponentImplementationDetail
