/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

/**
 * A component of a [KaSession][org.jetbrains.kotlin.analysis.api.KaSession].
 *
 * Session components mix functions and properties into the session, which allows using them directly from an [analyze][org.jetbrains.kotlin.analysis.api.analyze]
 * block where a [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] is available as a receiver. As such, functions from session
 * components define a large part of the Analysis API's surface, in addition to other [lifetime owners][org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner]
 * like [KaSymbol][org.jetbrains.kotlin.analysis.api.symbols.KaSymbol].
 *
 * **Important:** Any public function or property in a "session component" is directly available in a [KaSession][org.jetbrains.kotlin.analysis.api.KaSession]
 * context. There is no need to first retrieve the session component in any way.
 *
 * #### Example
 *
 * ```kotlin
 * // element: KtDeclaration
 * analyze(element) { // this: KaSession
 *     element.symbol
 * }
 * ```
 *
 * While [symbol][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProvider.symbol] is actually a property from the [KaSymbolProvider][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProvider]
 * session component, it is usable directly in the [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] context because the property has
 * been mixed into the session.
 *
 * All public API components inherited from [KaSessionComponent] are expected to be direct children of [KaSessionComponent].
 * That's required for the correctness of the Analysis API context parameter bridge checker, which ensures that each API endpoint from
 * session components has a corresponding context parameter bridge in the same file.
 */
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
