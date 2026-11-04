/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Represents a value that is computed on demand and cached.
 *
 * This is similar to [Lazy], but does not have [Lazy.isInitialized], because implementations
 * may drop the cached value at any moment (see, for example, [softRefCached]). So the value may need to be
 * computed more than once, and the information on whether it is currently computed is of little use.
 *
 * Like [Lazy], an instance of [Cached] can be used as a property delegate:
 * ```kotlin
 * val foo: Foo by softRefCached { computeFoo() }
 * ```
 * Unlike [Lazy], whose `getValue()` operator is an extension function, [Cached] implements [ReadOnlyProperty],
 * so no additional import is needed at the use site of the delegate.
 */
interface Cached<out T> : ReadOnlyProperty<Any?, T> {
    /**
     * Gets the cached value, or computes it if it has not been computed yet or has been dropped.
     */
    val value: T

    /**
     * Returns [value]. This allows using instances of [Cached] for property delegation.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}
