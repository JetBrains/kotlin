/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.java.direct.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Helpers for lazy-cache patterns, roughly equivalent to `lazy(PUBLICATION)` but avoiding wrapper and lambda allocation.
 */

/**
 * Lazy cache for a non-null property.
 *
 * Usage:
 * ```
 * @Volatile private var _typeParameters: List<JavaTypeParameter>? = null
 * override val typeParameters: List<JavaTypeParameter>
 *     get() = cachedNonNull({ _typeParameters }, { _typeParameters = it }) {
 *         computeTypeParameters(node, tree, resolutionContext)
 *     }
 * ```
 *
 * @param read snapshots the backing field. Must not have side effects.
 * @param write stores the computed value into the backing field.
 * @param compute the (possibly expensive) producer. Called at most once per instance under
 *                non-racy conditions; may be called more than once under racy reads, in which
 *                case the last writer wins — so [compute] must be a pure function.
 */
internal inline fun <T : Any> cachedNonNull(
    read: () -> T?,
    write: (T) -> Unit,
    compute: () -> T,
): T {
    contract {
        callsInPlace(read, InvocationKind.EXACTLY_ONCE)
        callsInPlace(write, InvocationKind.AT_MOST_ONCE)
        callsInPlace(compute, InvocationKind.AT_MOST_ONCE)
    }
    read()?.let { return it }
    val c = compute()
    write(c)
    return c
}

/**
 * Lazy cache for a nullable property
 *
 * Usage:
 * ```
 * @Volatile private var _fqName: Any? = NOT_COMPUTED
 * override val fqName: FqName?
 *     get() = cachedNullable({ _fqName }, { _fqName = it }) { computeFqName() }
 * ```
 */
internal inline fun <T> cachedNullable(
    read: () -> Any?,
    write: (Any?) -> Unit,
    compute: () -> T,
): T {
    contract {
        callsInPlace(read, InvocationKind.EXACTLY_ONCE)
        callsInPlace(write, InvocationKind.AT_MOST_ONCE)
        callsInPlace(compute, InvocationKind.AT_MOST_ONCE)
    }
    val cached = read()
    if (cached !== NOT_COMPUTED) {
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }
    val c = compute()
    write(c)
    return c
}

/**
 * Lazy cache for a `Boolean` property. Encodes the tri-state
 * `-1 = not computed, 0 = false, 1 = true` in a single `Int`
 *
 * The backing `@Volatile Int` field must be initialized to `-1`.
 *
 * Usage:
 * ```
 * @Volatile private var _isInterface: Int = -1
 * override val isInterface: Boolean
 *     get() = cachedBoolean({ _isInterface }, { _isInterface = it }) {
 *         tree.findChildByType(node, JavaSyntaxTokenType.INTERFACE_KEYWORD) != null
 *     }
 * ```
 */
internal inline fun cachedBoolean(
    read: () -> Int,
    write: (Int) -> Unit,
    compute: () -> Boolean,
): Boolean {
    contract {
        callsInPlace(read, InvocationKind.EXACTLY_ONCE)
        callsInPlace(write, InvocationKind.AT_MOST_ONCE)
        callsInPlace(compute, InvocationKind.AT_MOST_ONCE)
    }
    val cached = read()
    if (cached >= 0) return cached != 0
    val c = compute()
    write(if (c) 1 else 0)
    return c
}

// --- cache sentinels -------------------------------------------------------

/**
 * Sentinel for [cachedNullable]: distinguishes "not yet computed" from "computed and the result was null"
 */
internal val NOT_COMPUTED: Any = Any()

/**
 * Sentinel for [java.util.concurrent.ConcurrentHashMap]-backed caches that need to store a "computed, result was null"
 */
internal val NULL_CACHE_VALUE: Any = Any()

