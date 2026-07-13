/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * A [KaTypeScope] for the given [KaType], or `null` if the type is [erroneous][org.jetbrains.kotlin.analysis.api.types.KaErrorType].
 * The scope includes all members which are callable on a given type. It also includes [synthetic Java properties](https://kotlinlang.org/docs/java-interop.html#getters-and-setters).
 *
 * Comparing to [KaScope], the [KaTypeScope] contains members whose use-site type parameters have been substituted.
 *
 * #### Example
 *
 * ```kotlin
 * fun foo(list: List<String>) {
 *     list
 * }
 *```
 *
 * We can get a [KaTypeScope] for the [expression type][org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.expressionType]
 * of `list`. This scope contains a `get(index: Int): String` function, where the return type `E` from [List.get] is substituted with
 * the type argument `String`.
 *
 * @see KaTypeScope
 * @see org.jetbrains.kotlin.analysis.api.components.KaTypeProvider.type
 * @see org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.expressionType
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.scope: KaTypeScope?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.scope(this)
    }

/**
 * A [KaScope] containing unsubstituted declarations from the [KaType]'s underlying declaration.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaTypeScope.declarationScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.declarationScope(this)
    }

/**
 * A [KaTypeScope] containing the [synthetic Java properties](https://kotlinlang.org/docs/java-interop.html#getters-and-setters) created
 * for a given [KaType].
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.syntheticJavaPropertiesScope: KaTypeScope?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.syntheticJavaPropertiesScope(this)
    }
