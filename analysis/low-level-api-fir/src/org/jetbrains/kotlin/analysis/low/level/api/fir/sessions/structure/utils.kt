/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.structure

import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

internal fun <T> KProperty1<T, *>.isLazyInitialized(receiver: T): Boolean {
    isAccessible = true

    // If we don't error out here, we won't notice if a property actually isn't `by lazy`. Since the code is only run when
    // explicitly enabled by an internal flag, an error is fine.
    val lazy = getDelegate(receiver) as? Lazy<*> ?: error("Expected a lazy property.")

    return lazy.isInitialized()
}
