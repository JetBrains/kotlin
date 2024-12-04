/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 */
public interface KaSessionComponent : KaLifetimeOwner
